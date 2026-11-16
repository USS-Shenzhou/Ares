package cn.ussshenzhou.ares.attachment.entity;

import cn.ussshenzhou.ares.attachment.entity.gun.ClientHandGunSession;
import cn.ussshenzhou.ares.attachment.entity.gun.WieldProfile;
import cn.ussshenzhou.ares.gun.item.AttachmentPath;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.gun.item.IMountedWeapon;
import cn.ussshenzhou.ares.gun.client.resource.display.AttachmentDisplayInstance;
import cn.ussshenzhou.ares.gun.client.util.AimFovHelper;
import cn.ussshenzhou.ares.gun.client.util.AimView;
import cn.ussshenzhou.ares.gun.client.util.AimViewHelper;
import cn.ussshenzhou.ares.gun.network.message.c2s.C2SAimStanceMessage;
import cn.ussshenzhou.ares.gun.util.DualWieldHelper;
import cn.ussshenzhou.ares.gun.util.ZoomHelper;
import cn.ussshenzhou.ares.util.GunModuleMode;
import cn.ussshenzhou.ares.util.GunOperationSessionKind;
import cn.ussshenzhou.t88.network.NetworkHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/// > 客户端 LocalPlayer 持枪能力：第一人称表现 + 预判，并通知服务端。
/// >
/// > 客户端没有稳定的物品实例可追踪（任何 NBT 变化都会让网络层重建新 `ItemStack` 实例），
/// > 故会话身份只能用**值启发式**判定（同 Item + 同 gun 自定义 id，忽略弹药 / 配件 NBT）。
/// > 会话同步不自轮询，由 `InventoryEventHandler` 作为唯一检测器驱动。
/// @author TACZ, USS_Shenzhou
public class LocalGunOperator extends AbstractGunOperator<Player, ClientHandGunSession> {
    /// > 拒枪姿态上报对账：最近一次已上报的值，未变化则不再发包。
    private boolean aimStanceReported = false;
    private boolean reportedTacticalStance = false;

    public LocalGunOperator(Player player) {
        super(player);
    }

    @Override
    protected boolean isOperable(ItemStack stack) {
        return stack.getItem() instanceof IGun
                || (stack.getItem() instanceof IMountedWeapon mw && mw.isStandalone(stack));
    }

    @Override
    protected ClientHandGunSession createSession(InteractionHand hand, ItemStack live, WieldProfile profile) {
        return new ClientHandGunSession(player, hand, live, profile);
    }

    @Override
    protected boolean matches(ClientHandGunSession session, ItemStack live, GunOperationSessionKind expectedKind) {
        /// > 值启发式：客户端无稳定实例可比
        return DualWieldHelper.sameHeldGun(session.stack(), live) && session.kind() == expectedKind;
    }

    /// > 客户端会话**跨切枪持久存活**，不按枪 id 销毁重建。
    /// >
    /// > 切换（同位换枪 / 双持移除副手致 kind 变 / 切空手）统一让现有会话起收枪窗口
    /// > （[ClientHandGunSession#requestSwitch()]），窗口跑完后由 [#onPutAwayComplete]
    /// > 按「实时手持物」落地（重建为新形态并掏枪，或移除会话）。窗口期内目标再变不重启窗口，
    /// > 结束时读实时值天然吸收（含「切空手收一半又拿枪」的场景）。
    @Override
    protected void synchronizeSession(InteractionHand hand) {
        ItemStack live = player.getItemInHand(hand);
        GunOperationSessionKind expectedKind = resolveSessionKind(hand, live);
        ClientHandGunSession current = sessions.get(hand);
        if (current == null) {
            /// > 空手 → 仍空手：无会话
            if (expectedKind == null) {
                return;
            }
            /// > 空手 → 持枪：直接掏枪（无收枪窗口）
            ClientHandGunSession next = createSession(hand, live, profileFor(expectedKind));
            sessions.put(hand, next);
            next.draw(live);
            return;
        }
        /// > 已有会话且仍对应当前手持物（含形态）：无变化
        if (expectedKind != null && matches(current, live, expectedKind)) {
            return;
        }
        /// > 物品 / 形态变化（含变空手）：起收枪窗口，不销毁会话，落地延后到收枪结束
        current.requestSwitch();
    }

    /// > 收枪窗口结束后的落地（在 operator tick 迭代之外执行，`draw` 的 clear 安全）。
    /// > 按**实时手持物**决定结果：变空手则移除会话；否则按当前形态重绑并掏枪。
    private void onPutAwayComplete(InteractionHand hand) {
        ClientHandGunSession session = sessions.get(hand);
        if (session == null) {
            return;
        }
        ItemStack live = player.getItemInHand(hand);
        GunOperationSessionKind kind = resolveSessionKind(hand, live);
        if (kind == null) {
            /// > 收枪结束时已是空手：移除会话（先释放侧瞄过渡定时器等资源）
            session.dispose();
            sessions.remove(hand);
            return;
        }
        /// > 按实时形态重绑并掏枪（draw 内部起 DrawSegment 并重置开火 / 换弹策略）
        session.rebind(profileFor(kind));
        session.draw(live);
    }

    @Override
    public void tick() {
        tickSessions();
        reconcileZoomPredictions();
        reconcileAimStance();
        /// > 收枪窗口结束的落地：在 tracks.tick 之后轮询，避免在 segment 迭代内重建 / 移除会话
        for (InteractionHand hand : InteractionHand.values()) {
            ClientHandGunSession session = sessions.get(hand);
            if (session != null && session.consumePutAwayCompleted()) {
                onPutAwayComplete(hand);
            }
        }
    }

    /// > 拒枪姿态对账（每 tick）：姿态影响散布且服务端权威，故只在姿态变化时上报一次。
    /// >
    /// > 除 Alt+V 请求外，这里还兜住「配件增删导致视图位列表变化、索引被动钳制」以及收枪 / 换手等隐式变化。
    /// > 复位由服务端自身生命周期处理，客户端不额外发复位包，避免与权威复位乱序。
    private void reconcileAimStance() {
        InteractionHand hand = preferredHand();
        boolean tactical = isTacticalStance(hand);
        if (aimStanceReported && tactical == reportedTacticalStance) {
            return;
        }
        aimStanceReported = true;
        reportedTacticalStance = tactical;
        NetworkHelper.sendToServer(new C2SAimStanceMessage(tactical, hand));
    }

    /// > 变倍预测对账（每 tick）：预测持续生效直到服务端 NBT 同步回来与预测一致（无缝清除），
    /// > 或等待确认超时（丢包 / 服务端拒绝）后放弃并回读 NBT。预测存在但激活瞄具失效时也清除。
    private void reconcileZoomPredictions() {
        for (InteractionHand hand : InteractionHand.values()) {
            ClientHandGunSession session = sessions.get(hand);
            if (session == null) {
                continue;
            }
            var prediction = session.zoomPrediction();
            if (prediction == null) {
                continue;
            }
            ActiveScope scope = resolveActiveScope(session);
            if (scope == null) {
                /// > 激活瞄具不存在（换枪 / 卸镜），预测悬空
                session.clearZoomPredict();
                continue;
            }
            int authIndex = ZoomHelper.getZoomIndex(scope.stack());
            float authProgress = ZoomHelper.getZoomProgress(scope.stack());
            boolean indexMatch = prediction.zoomIndex() == authIndex;
            boolean progressMatch = prediction.zoomProgress() < 0.0F
                    ? authProgress < 0.0F
                    : (authProgress >= 0.0F && Math.abs(prediction.zoomProgress() - authProgress) <= 0.02F);
            if (indexMatch && progressMatch) {
                session.clearZoomPredict();
            } else if (!prediction.isFresh()) {
                /// > 确认超时：放弃预测，让渲染回读 NBT 权威值
                session.clearZoomPredict();
            }
        }
    }

    /// > 强制主手切换（绕过值启发式 [#matches]）。
    /// >
    /// > 客户端无稳定物品实例，「切 hotbar 格子 / 同格子换成同 id 另一把枪」无法靠值判出
    /// > （gunId、kind 都可能不变，但确实是另一把枪）。`InventoryEventHandler` 作为外部检测器
    /// > 已确定发生了切换，故此处直接令现有会话起收枪窗口，不再经 matches 短路。
    /// > - 主手有会话 → 起收枪窗口（[ClientHandGunSession#requestSwitch()]），
    /// >   落地延后到收枪结束，按实时手持物掏新枪。
    /// > - 主手无会话 → 退化为常规同步（空手→持枪直接掏枪 / 空手→空手无操作）。
    public void forceSwitch(InteractionHand hand) {
        ClientHandGunSession current = sessions.get(hand);
        if (current == null) {
            synchronizeSession(hand);
            return;
        }
        current.requestSwitch();
    }

    /// > 立刻丢弃并按当前主 / 副手物品重建客户端会话，不播放收枪过渡。
    /// > 用于创造模式物品栏这类会重建 [ItemStack] 实例的强同步边界。
    public void forceRebuildSessions() {
        for (InteractionHand hand : InteractionHand.values()) {
            ClientHandGunSession current = sessions.remove(hand);
            if (current != null) {
                current.putAway(player.getItemInHand(hand));
            }
            synchronizeSession(hand);
        }
    }

    /// > —— 客户端特有方法 ——

    @Override
    public void fireRelease() {
        fireRelease(preferredHand());
    }

    @Override
    public void fireRelease(InteractionHand hand) {
        ClientHandGunSession session = session(hand);
        if (session != null) {
            session.fireRelease(player.getYRot(), player.getXRot());
        }
    }

    /// > 当前是否允许换弹 / 退弹 / 切换弹种（客户端预判）。用于弹种轮盘等入口在不可操作时不弹出。
    public boolean canReload(InteractionHand hand) {
        ClientHandGunSession session = ensureSession(hand, false);
        return session != null && session.canReload();
    }

    /// > 玩家请求切换模式（模块模式 / 快慢机）：起锁定窗口、不预测，等待服务端权威 NBT 同步回来应用。
    /// > 发送 C2S 请求由调用方负责。
    public void requestModeSwitch() {
        requestModeSwitch(preferredHand());
    }

    public void requestModeSwitch(InteractionHand hand) {
        ClientHandGunSession session = ensureSession(hand, false);
        if (session != null) {
            session.requestModeSwitch();
        }
    }

    public boolean isModeSwitching() {
        return isModeSwitching(preferredHand());
    }

    public boolean isModeSwitching(InteractionHand hand) {
        ClientHandGunSession session = session(hand);
        return session != null && session.isModeSwitching();
    }

    /// > 玩家请求切换瞄准视图位（纯客户端表现）：按当前视图位列表（本体机瞄 / 战术拒枪位 / 安装瞄具）
    /// > 循环切到下一个。至少两个视图位才生效。
    public void requestAimViewSwitch(InteractionHand hand) {
        ClientHandGunSession session = session(hand);
        if (session == null) {
            return;
        }
        int count = AimViewHelper.collect(session.getCurrentItem()).size();
        session.requestAimViewSwitch(count);
    }

    /// > 当前（目标）瞄准视图位是否为战术拒枪姿态。
    /// >
    /// > 切换请求即按目标视图位翻转，不等过渡收敛：动画状态机需要立刻起姿态，服务端也需要尽早拿到
    /// > 权威散布口径。
    @Override
    public boolean isTacticalStance(InteractionHand hand) {
        ClientHandGunSession session = session(hand);
        if (session == null) {
            return false;
        }
        List<AimView> views = AimViewHelper.collect(session.getCurrentItem());
        int index = session.getPendingAimViewIndex() >= 0
                ? session.getPendingAimViewIndex() : session.getActiveAimViewIndex();
        return AimViewHelper.viewAt(views, index) instanceof AimView.BodyView body
                && body.kind() == AimView.Kind.TACTICAL;
    }

    /// > 是否正在瞄准视图位切换过渡中。
    public boolean isAimViewSwitching(InteractionHand hand) {
        ClientHandGunSession session = session(hand);
        return session != null && session.isAimViewSwitching();
    }

    /// > 瞄准视图位切换状态（渲染器读取）：激活 / 切换目标视图位索引与过渡进度。无会话时返回 null。
    @Nullable
    public AimViewState aimViewState(InteractionHand hand) {
        ClientHandGunSession session = session(hand);
        if (session == null) {
            return null;
        }
        return new AimViewState(session.getActiveAimViewIndex(), session.getPendingAimViewIndex(),
                session.getAimViewSwitchProgress());
    }

    /// > 瞄准视图位切换状态载体：进度 0 = 完全旧视图位，1 = 完全新视图位。
    public record AimViewState(int activeIndex, int pendingIndex, float progress) {
        public static final AimViewState NONE = new AimViewState(0, -1, 1f);
    }

    /// > —— 变倍（有级档位 / 无级滚轮）——

    /// > 当前激活瞄具的解析结果：配件栈 + 槽位路径 + display。
    private record ActiveScope(ItemStack stack, AttachmentPath path, AttachmentDisplayInstance display) {
    }

    /// > 玩家请求有级变倍（开镜时按 V）：在当前激活瞄具的档位间 +1 循环。
    /// > 会话本地预测 + C2S 服务端权威落地。未开镜 / 非多档瞄具时忽略。
    public void requestZoomStep(InteractionHand hand) {
        ClientHandGunSession session = session(hand);
        if (session == null) {
            return;
        }
        ActiveScope scope = resolveActiveScope(session);
        /// > 有级 / 无级互斥：声明了无级范围（zoom_fov_range）后 V 键不再循环档位。
        if (scope == null || !scope.display().hasSteppedZooms() || scope.display().hasSmoothZoom()) {
            return;
        }
        session.requestZoomStep(scope.stack(), scope.path(), scope.display().getViewsFov().length);
    }

    /// > 玩家请求无级变倍（开镜按住 V + 滚轮）：倍率等比步进，clamp 到瞄具声明的 zoom_fov_range。
    /// > 会话本地预测 + 防抖后 C2S 服务端权威落地。未开镜 / 瞄具未声明无级范围时忽略。
    public void requestZoomDelta(InteractionHand hand, double delta) {
        ClientHandGunSession session = session(hand);
        if (session == null) {
            return;
        }
        ActiveScope scope = resolveActiveScope(session);
        if (scope == null || !scope.display().hasSmoothZoom()) {
            return;
        }
        float baseProgress = AimFovHelper.resolveScopeZoomProgress(scope.stack(), session.zoomPrediction());
        if (baseProgress < 0.0F) {
            /// > 尚未启用无级：以当前档位模型 FOV 在无级模型 FOV 范围上的映射为初始进度
            float[] fovRange = AimFovHelper.resolveSmoothFovRange(scope.display());
            if (fovRange == null) {
                return;
            }
            float baseFov = AimFovHelper.resolveScopeZoomFov(scope.stack(), null, fovRange[1]);
            baseProgress = Mth.clamp((fovRange[1] - baseFov) / (fovRange[1] - fovRange[0]), 0.0F, 1.0F);
        }
        session.requestZoomDelta(scope.stack(), scope.path(), delta, baseProgress);
    }

    /// > 当前变倍预测状态（渲染 / 滚轮基准读取）；无预测返回 null。
    @Nullable
    public ClientHandGunSession.ZoomPrediction zoomPrediction(InteractionHand hand) {
        ClientHandGunSession session = session(hand);
        return session != null ? session.zoomPrediction() : null;
    }

    /// > 按侧瞄激活索引解析当前激活瞄具（收集顺序 = 槽位声明顺序，与 stencil 渲染会话一致）。
    /// > 未持枪 / 无瞄具返回 null。
    @Nullable
    private ActiveScope resolveActiveScope(ClientHandGunSession session) {
        ItemStack gun = session.getCurrentItem();
        if (IGun.asGun(gun) == null) {
            return null;
        }
        List<AimView> views = AimViewHelper.collect(gun);
        if (views.isEmpty()) {
            return null;
        }
        int active = Mth.clamp(session.getActiveAimViewIndex(), 0, views.size() - 1);
        /// > 与索引口径一致：激活的是本体视位（机瞄 / 拒枪）时没有可变倍的瞄具。
        if (!(views.get(active) instanceof AimView.ScopeView scope)) {
            return null;
        }
        return new ActiveScope(scope.stack(), AttachmentPath.of(scope.slotId()), scope.display());
    }

    public boolean canInspectWeapon(InteractionHand hand) {
        ClientHandGunSession session = ensureSession(hand, false);
        return session != null && session.canInspectWeapon();
    }

    public boolean canSwitchWeaponMode(InteractionHand hand, @Nonnull GunModuleMode mode) {
        ClientHandGunSession session = ensureSession(hand, false);
        return session != null && session.canSwitchWeaponMode(mode);
    }

    public float modeTransitionProgress() {
        return modeTransitionProgress(preferredHand());
    }

    public float modeTransitionProgress(InteractionHand hand) {
        ClientHandGunSession session = session(hand);
        return session != null ? session.modeTransitionProgress() : 1.0f;
    }

    public void extendModeTransition(long durationMs) {
        extendModeTransition(preferredHand(), durationMs);
    }

    public void extendModeTransition(InteractionHand hand, long durationMs) {
        ClientHandGunSession session = session(hand);
        if (session != null) {
            session.extendModeTransition(durationMs);
        }
    }

    public void clearModeTransition() {
        sessions.values().forEach(ClientHandGunSession::clearModeTransition);
    }

    public void clearModeTransition(InteractionHand hand) {
        ClientHandGunSession session = session(hand);
        if (session != null) {
            session.clearModeTransition();
        }
    }
}
