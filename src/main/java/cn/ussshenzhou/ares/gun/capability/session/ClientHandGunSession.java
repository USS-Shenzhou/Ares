package cn.ussshenzhou.ares.gun.capability.session;

import cn.ussshenzhou.ares.Ares;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler;
import cn.ussshenzhou.ares.gun.custom.attribute.GunPropertiesCaches;
import cn.ussshenzhou.ares.gun.capability.GunHolderState;
import cn.ussshenzhou.ares.gun.capability.GunModuleMode;
import cn.ussshenzhou.ares.gun.capability.HandGunSession;
import cn.ussshenzhou.ares.gun.capability.LocalGunOperator;
import cn.ussshenzhou.ares.gun.capability.WieldProfile;
import cn.ussshenzhou.ares.gun.gameplay.ShootResult;
import cn.ussshenzhou.ares.gun.item.AttachmentPath;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.gun.item.IMountedWeapon;
import cn.ussshenzhou.ares.gun.client.handler.CameraEventHandler;
import cn.ussshenzhou.ares.gun.client.renderer.animation.GunAnimationInstance;
import cn.ussshenzhou.ares.gun.custom.gun.BoltType;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import cn.ussshenzhou.ares.gun.custom.gun.GunIndex;
import cn.ussshenzhou.ares.gun.gameplay.reload.ReloadPlan;
import cn.ussshenzhou.ares.gun.gameplay.reload.ReloadPolicy;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionGate;
import cn.ussshenzhou.ares.gun.gameplay.timeline.FireController;
import cn.ussshenzhou.ares.gun.gameplay.timeline.Timer;
import cn.ussshenzhou.ares.gun.gameplay.timeline.SegmentSet;
import cn.ussshenzhou.ares.gun.gameplay.timeline.segment.AutoChamberRequestSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.segment.CooldownSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.segment.DrawSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.segment.InspectSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.segment.ModeSwitchPendingSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.segment.ModeTransitionSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.segment.PutAwaySegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.segment.ReloadSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.segment.StopSprintSegment;
import cn.ussshenzhou.ares.gun.network.message.c2s.*;
import cn.ussshenzhou.ares.gun.util.AmmoDataHelper;
import cn.ussshenzhou.ares.gun.util.AttributeDataHelper;
import cn.ussshenzhou.ares.gun.util.FireModeHelper;
import cn.ussshenzhou.ares.gun.util.ModuleModeHelper;
import cn.ussshenzhou.ares.gun.util.MountedWeaponHelper;
import cn.ussshenzhou.ares.gun.util.ZoomHelper;
import cn.ussshenzhou.t88.network.NetworkHelper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import cn.ussshenzhou.ares.gun.util.math.SecondOrderDynamics;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.EnumSet;
import java.util.List;

/// @author TACZ, USS_Shenzhou
public class ClientHandGunSession extends HandGunSession {
    /// > 当前开火序列 / 动画已构建所对应的模块模式。由 tick 对账权威 NBT 驱动，不做预测。
    @Nonnull
    private GunModuleMode appliedMode = GunModuleMode.PRIMARY;

    /// > 当前开火序列已构建所对应的快慢机档位。由 tick 对账权威 NBT 驱动，
    /// > 档位变化时重建开火序列并播拨杆动画。
    private int appliedFireModeIndex = 0;

    public long lastShootTimestamp = -1;
    public long lastTickTimestamp = -1;
    public boolean isAiming = false;

    /// > 收枪倒计时结束标志。在 PutAwaySegment 的 onComplete（tracks.tick 迭代内）置位，
    /// > 由 operator 在 tick 迭代之外轮询 consumePutAwayCompleted() 消费并落地（重建/移除会话），
    /// > 避免在 segment 迭代中调用 draw() 的 tracks.clear()。
    private boolean putAwayCompleted = false;

    /// > 装填被打断后 0.5s 内不接受新的 reload 输入，防止状态机跳过 intro。收编旧的 reloadInterruptedTimestamp。
    private final Timer reloadInterruptCooldown = new Timer();
    private static final long RELOAD_INTERRUPT_COOLDOWN_MS = 500;
    /// > 无过渡动画时的缺省模式切换过渡时长（毫秒），约 0.2s。
    private static final long DEFAULT_MODE_TRANSITION_MS = 200;

    private final SegmentSet tracks = new SegmentSet();

    @Nullable
    private ReloadPolicy reloadPolicy;

    /// > 瞄准进度（纯客户端表现插值，0~1）。每 tick ±0.33 步进，与墙上时钟无关，
    /// > 由渲染按 partialTicks 在 last/current 间平滑。不投影状态、不约束门禁，
    /// > 故不作为片段塞进 SegmentSet，仅作 operator 表现字段。
    // FIXME support aim time
    private static final float AIM_STEP = 0.33f;
    private float aimProgress = 0f;
    private float lastAimProgress = 0f;

    /// > 瞄准视图位切换（纯客户端表现，不做服务端权威）：激活视图位索引（AimViewHelper 收集顺序，
    /// > 含本体机瞄 / 战术拒枪位 / 安装瞄具）与切换过渡进度。由 SightSwitchKey 触发，渲染器按
    /// > active/pending 过滤 stencil 会话，并按过渡进度缩放圆切半径 / 插值瞄准视点。过渡收敛
    /// > （NextSecondOrderDynamics.isStable）后落定 activeAimViewIndex。换枪 / 收枪后状态随会话重建或 draw 重置。
    private int activeAimViewIndex = 0;
    private int pendingAimViewIndex = -1;
    @Nullable
    private SecondOrderDynamics aimViewSwitchDynamics;

    /// > 变倍预测（纯客户端先行，服务端权威写配件 NBT、随常规物品同步回程对账）：预测写入后
    /// > 持续生效（不设短窗口自动过期），由 operator 对账：NBT 与预测一致则清除（无缝交接），
    /// > 超过确认超时仍未一致（丢包 / 服务端拒绝）才放弃、回读 NBT。预测与瞄具耦合——
    /// > 侧瞄切换 / 换枪 / 重绑时一律清除。
    private static final long ZOOM_CONFIRM_TIMEOUT_MS = 1500;
    /// > 滚轮无级变倍每格进度步进（0~1 全程约 20 格）。
    private static final float ZOOM_PROGRESS_STEP = 0.05F;
    /// > 滚轮无级变倍防抖上报窗口（毫秒）：滚停该时长后落一次最终值。
    private static final long ZOOM_DEBOUNCE_MS = 300;
    private int predictZoomIndex = -1;
    private float predictZoomProgress = -1.0F;
    private long predictZoomTimestamp = -1L;
    /// > 滚轮无级变倍防抖上报：滚停超时后把最终预测值落一次服务端权威 NBT（期间不逐格发包）。
    @Nullable
    private AttachmentPath pendingZoomPath = null;
    private long pendingZoomSyncTimestamp = -1L;

    @Nullable
    private FireController fireController;

    private final Player player;

    public ClientHandGunSession(Player player, InteractionHand hand, ItemStack stack, WieldProfile profile) {
        super(hand, stack.copy(), profile);
        this.player = player;
    }

    /// > 当前冷却片段（可能不存在）。
    @Nullable
    private CooldownSegment cooldown() {
        return tracks.get(CooldownSegment.TOKEN);
    }

    @Nonnull
    @Override
    public EnumSet<GunHolderState> getActiveStates() {
        return tracks.projectStates();
    }

    @Override
    public boolean isIdle() {
        return tracks.isIdle();
    }

    @Override
    public boolean canReload() {
        return tracks.gate().canReload();
    }

    @Nonnull
    public ItemStack getCurrentItem() {
        return player.getItemInHand(hand());
    }

    /// > 当前玩法预判目标：下挂模式返回宿主属性缓存里解析好的下挂副本，否则返回宿主手持物。
    /// > <p>
    /// > 下挂副本随宿主 {@link GunPropertiesCaches} 生命周期存活（按宿主 {@link ItemStack} 实例为 key）：
    /// > 宿主实例不变则复用同一副本（不重复解析、不堆短命缓存）；服务端同步重建宿主实例后缓存随之失效重建，
    /// > 永远反映最新权威值，不会 desync。
    @Nonnull
    @Override
    public ItemStack activeStack() {
        if (appliedMode != GunModuleMode.UNDER_BARREL) {
            return getCurrentItem();
        }
        ItemStack mounted = GunPropertiesCaches.of(getCurrentItem()).getMountedWeapon();
        return mounted != null ? mounted : getCurrentItem();
    }

    @Override
    public boolean isAiming() {
        return isAiming;
    }

    @Override
    public boolean isClient() {
        return true;
    }

    @Override
    public Player owner() {
        return player;
    }

    @Override
    public void fire() {
        /// > 模式切换过渡期间锁定输入。
        if (isModeSwitching()) {
            return;
        }
        CooldownSegment cooldown = cooldown();
        if (fireController == null || cooldown == null) {
            return;
        }

        ActionGate gate = tracks.gate();
        if (!gate.canShoot()) {
            EnumSet<GunHolderState> states = tracks.projectStates();
            ReloadSegment reload = tracks.get(ReloadSegment.TOKEN);
            if (states.contains(GunHolderState.BOLT_ACTION)) {
                /// > 拉栓窗口：硬阻断
                return;
            } else if (reload != null
                    && reload.canFireInterrupt()
                    && fireController.canFire(activeStack())) {
                /// > 装填中：确认有弹药可射击时才打断
                cancelReload();
                NetworkHelper.sendToServer(new C2SReloadMessage(C2SReloadMessage.ACTION_CANCEL, hand()));
            } else if (states.contains(GunHolderState.FIRE_COOLDOWN) && !tracks.has(ReloadSegment.TOKEN)) {
                /// > FIRE_COOLDOWN：由冷却片段严格判定毫秒级冷却是否已过，避免 tick 量化导致节奏不均
                if (!cooldown.isCooldownOver()) {
                    return;
                }
                /// > 冷却已过，允许继续射击
            } else {
                return;
            }
        }

        /// > 检视中开火：取消检视
        if (tracks.has(InspectSegment.TOKEN)) {
            tracks.cancel(InspectSegment.TOKEN);
        }

        if (this.player.isSprinting()) {
            tracks.add(StopSprintSegment.interruptSprint());
            this.player.setSprinting(false);
            return;
        }

        if (this.player instanceof LocalPlayer) {
            if (!fireController.canFire(activeStack())) {
                return;
            }

            /// > 委托给开火控制器，控制器编排单发 / burst / bolt 片段。
            ShootResult result = fireController.onTriggerPress(player.getYRot(), player.getXRot());
            if (result == ShootResult.ACCEPTED) {
                /// > 请求已接受但未发射（delayed/charge），等待 tick 中触发
                this.player.setSprinting(false);
            }
            /// > SUCCESS：postPrimaryFire 已处理时间戳 / 阻止冲刺。BLOCKED/NO_AMMO 等：不做任何事
        }
    }

    /// > 扳机松开。charge 模式松手发射。
    @Override
    public ShootResult fireRelease(float yaw, float pitch) {
        if (fireController == null) {
            return ShootResult.BLOCKED;
        }
        /// > 松手前先读蓄力原始累积量（onRelease 会清零），随 C2S 上报供服务端权威重算倍率（不上报倍率）。
        int chargeTicks = fireController.chargeInput();
        ShootResult result = fireController.onTriggerRelease(yaw, pitch);
        /// > SUCCESS 由 postPrimaryFire 处理时间戳；charge 松手后发送 release 消息（带蓄力原始量）
        NetworkHelper.sendToServer(new C2SFireMessage(yaw, pitch, C2SFireMessage.ACTION_RELEASE, chargeTicks, hand()));
        return result;
    }

    /// > 客户端主发出膛后的端副作用：更新 muzzle-flash 时间戳、阻止冲刺、起冲刺锁。
    /// > 由开火序列片段在子弹实际出膛后通过 postPrimaryFire 回调触发。
    /// > （RPM 冷却节奏由 {@link CooldownSegment} 在出膛 notify 时驱动，不在此处理。）
    private void onShotFired() {
        /// > muzzle-flash 时间戳：记录真实出膛时刻（射速节奏由冷却片段独立维护）
        this.lastShootTimestamp = System.currentTimeMillis();
        this.player.setSprinting(false);
        /// > 射击后 400ms 冲刺锁（不投影 STOP_SPRINTING），避免射击打断冲刺后反复进入/取消冲刺。
        /// > 若已有跑射片段（含打断投影态）则只刷新其冲刺锁窗口，不覆盖投影。
        StopSprintSegment ss = tracks.get(StopSprintSegment.TOKEN);
        if (ss != null) {
            ss.refreshBlock();
        } else {
            tracks.add(StopSprintSegment.blockOnly());
        }
    }

    @Override
    public long getLastFireTime() {
        return lastShootTimestamp;
    }

    @Override
    public void draw(ItemStack newStack) {
        updateStack(newStack.copy());
        tracks.clear();
        this.fireController = null;
        this.reloadPolicy = null;
        /// > 按宿主 NBT 恢复模块模式 / 快慢机档位（不强制复位，避免额外写 NBT 造成同步延迟）。
        /// > NBT 即权威：是下挂模式就恢复下挂模式；若下挂已不在则退回主枪。
        this.appliedMode = ModuleModeHelper.getModuleMode(newStack);
        if (this.appliedMode == GunModuleMode.UNDER_BARREL
                && MountedWeaponHelper.findInstalledMountedWeapon(newStack).isEmpty()) {
            this.appliedMode = GunModuleMode.PRIMARY;
        }
        this.appliedFireModeIndex = FireModeHelper.getFireModeIndex(newStack);

        if (newStack.getItem() instanceof IGun || (newStack.getItem() instanceof IMountedWeapon mw && mw.isStandalone(newStack))) {
            int dataDraw = 10;
            if (newStack.getItem() instanceof IGun gun) {
                dataDraw = gun.getIndex(newStack).map(GunIndex::getData).map(GunData::getDrawTicks).orElse(10);
            }
            /// > 会话持有的是副本，副本本身无旧缓存条目；这里清理的是客户端物品栏原实例上的条目，
            /// > 属防御性失效（就地改写过 NBT 时不留旧值）。写入方失效契约见 GunPropertiesCaches。
            GunPropertiesCaches.invalidateCache(newStack);
            initFireBehavior(activeStack());
            tracks.add(new DrawSegment(dataDraw * 50L));
        }
        /// > 非持枪：不加 DrawSegment，无投影片段即空闲（isIdle）

        this.lastShootTimestamp = 0;
        this.isAiming = false;
        this.aimProgress = 0f;
        this.lastAimProgress = 0f;
        resetAimViewSwitch();
    }

    @Override
    public void putAway(ItemStack pendingStack) {
        /// > 会话不再按枪 id 销毁重建：切枪统一走 requestSwitch 起收枪窗口。
        /// > 保留 override 仅为兼容基类入口（operator 不再调用它做收枪）。
        this.isAiming = false;
        cancelReload();
        resetAimViewSwitch();
    }

    /// > 请求切换（收枪 → 掏枪）。客户端会话跨切枪持久存活：
    /// > <ul>
    /// >   <li>若已处于收枪窗口（{@link PutAwaySegment} 活跃），忽略本次请求，让窗口跑完。
    /// >       窗口结束时由 operator 读「实时手持物」落地，故窗口期内目标再变会被自然吸收。</li>
    /// >   <li>否则清空当前所有片段（打断 reload/burst/inspect/draw），起一段收枪窗口；
    /// >       时长由「旧枪」（{@link #stack()}）的 putAwayTicks 经形态调整推导。</li>
    /// > </ul>
    /// > 收枪结束仅置 {@link #putAwayCompleted} 标志，真正落地延后到 operator tick 轮询时执行，
    /// > 避免在 {@code tracks.tick()} 迭代内调用 {@link #draw} 的 {@code tracks.clear()}。
    public void requestSwitch() {
        if (tracks.has(PutAwaySegment.TOKEN)) {
            return;
        }
        this.isAiming = false;
        int putAwayTicks = 10;
        ItemStack oldStack = stack();
        if (oldStack.getItem() instanceof IGun oldGun) {
            putAwayTicks = oldGun.getIndex(oldStack).map(GunIndex::getData)
                    .map(GunData::getPutAwayTicks).orElse(10);
        }
        putAwayTicks = profile().adjustDurationTicks(WieldProfile.DurationKind.PUT_AWAY, putAwayTicks);
        tracks.clear();
        this.fireController = null;
        this.reloadPolicy = null;
        this.putAwayCompleted = false;
        tracks.add(new PutAwaySegment(putAwayTicks * 50L, () -> this.putAwayCompleted = true));
    }

    /// > 消费收枪完成标志（读取并清零）。由 operator 在 tick 迭代之外轮询。
    public boolean consumePutAwayCompleted() {
        if (putAwayCompleted) {
            putAwayCompleted = false;
            return true;
        }
        return false;
    }

    /// > 玩家请求切换瞄准视图位（纯客户端表现）：循环切到下一个视图位并起二阶动力学过渡。
    /// > 需要至少两个视图位（aimViewCount 由调用方按 {@code AimViewHelper} 收集得出）；已在切换中则忽略。
    public void requestAimViewSwitch(int aimViewCount) {
        if (aimViewCount <= 1 || isAimViewSwitching()) {
            return;
        }
        clearZoomPredict();
        pendingAimViewIndex = (activeAimViewIndex + 1) % aimViewCount;
        aimViewSwitchDynamics = new SecondOrderDynamics(1.2f, 1.2f, 0.5f, new float[]{0});
        aimViewSwitchDynamics.target(0, 1f);
    }

    /// > 是否正在瞄准视图位切换过渡中。
    public boolean isAimViewSwitching() {
        return pendingAimViewIndex >= 0;
    }

    public int getActiveAimViewIndex() {
        return activeAimViewIndex;
    }

    /// > 切换目标视图位索引；未在切换时为 -1。
    public int getPendingAimViewIndex() {
        return pendingAimViewIndex;
    }

    /// > 切换过渡进度 0~1（0 = 完全旧视图位，1 = 完全新视图位）。未在切换时恒为 1。
    /// > 渲染器据此缩放瞄具圆切半径并插值瞄准视点；取值已含动力学平滑，无需再插值。
    public float getAimViewSwitchProgress() {
        return aimViewSwitchDynamics == null ? 1f : aimViewSwitchDynamics.get(0);
    }

    /// > 收枪 / 换枪 / 会话销毁时重置瞄准视图位切换状态并释放过渡动力学定时器。
    public void dispose() {
        resetAimViewSwitch();
    }

    /// > 收枪 / 换枪时重置瞄准视图位切换状态。由 draw / putAway / dispose 调用。
    private void resetAimViewSwitch() {
        activeAimViewIndex = 0;
        pendingAimViewIndex = -1;
        clearZoomPredict();
        if (aimViewSwitchDynamics != null) {
            aimViewSwitchDynamics.cancel();
            aimViewSwitchDynamics = null;
        }
    }

    /// > —— 变倍（有级档位 / 无级滚轮）——

    /// > 变倍预测状态载体。无级运行时状态是单个进度 {@code zoomProgress}（0~1），
    /// > 模型 FOV 与倍率由各自的声明范围在同一进度上插值（同比例映射）；
    /// > {@code < 0} 表示无级未生效（按档位数组取值）。
    /// > <p>
    /// > 预测持续生效直到 NBT 对账一致（{@link LocalGunOperator} 调用 {@link #clearZoomPredict()} 清除）
    /// > 或确认超时（{@link #isFresh()} 为 false）。
    public record ZoomPrediction(int zoomIndex, float zoomProgress, long timestampMs) {
        public boolean isFresh() {
            return System.currentTimeMillis() - timestampMs < ZOOM_CONFIRM_TIMEOUT_MS;
        }
    }

    /// > 当前变倍预测状态；无预测（读配件 NBT 权威值）返回 null。
    @Nullable
    public ZoomPrediction zoomPrediction() {
        if (predictZoomTimestamp < 0) {
            return null;
        }
        return new ZoomPrediction(predictZoomIndex, predictZoomProgress, predictZoomTimestamp);
    }

    /// > 玩家请求有级变倍（档位 +1 循环，并清除无级进度）：本地预测立即生效并上报服务端权威落地。
    /// >
    /// > @param activeScope 当前激活瞄具（读取其配件 NBT 档位索引）
    /// > @param path        激活瞄具槽位路径（随 C2S 上报）
    /// > @param scopeCount  档位数（views_fov 长度）
    public void requestZoomStep(ItemStack activeScope, AttachmentPath path, int scopeCount) {
        if (activeScope.isEmpty() || scopeCount <= 1) {
            return;
        }
        int next = (ZoomHelper.getZoomIndex(activeScope) + 1) % scopeCount;
        applyZoomPredict(path, next, -1.0F, true);
    }

    /// > 玩家请求无级变倍（按住变倍键拨动滚轮）：进度按等比步进增减并 clamp 到 [0, 1]；
    /// > 模型 FOV 与倍率由各自声明范围在同一进度上插值。以当前生效进度为基准（调用方解析）。
    /// > 预测立即生效；上报走防抖（滚停 {@value #ZOOM_DEBOUNCE_MS}ms 后落最终值），期间不逐格发包。
    /// >
    /// > @param activeScope 当前激活瞄具（档位索引保持来源）
    /// > @param path        激活瞄具槽位路径
    /// > @param delta       本次滚轮格数（正 = 放大倍率）
    /// > @param baseProgress 当前生效进度（预测 / NBT / 档位在范围上的映射）
    public void requestZoomDelta(ItemStack activeScope, AttachmentPath path, double delta, float baseProgress) {
        if (activeScope.isEmpty()) {
            return;
        }
        float next = Mth.clamp((baseProgress < 0.0F ? 0.0F : baseProgress)
                + (float) delta * ZOOM_PROGRESS_STEP, 0.0F, 1.0F);
        applyZoomPredict(path, ZoomHelper.getZoomIndex(activeScope), next, false);
    }

    /// > 本地预测写入；{@code sendImmediately} 为真（V 键切档）立即上报服务端权威，
    /// > 为假（滚轮无级）挂起待 {@link #tick()} 防抖上报最终值。
    private void applyZoomPredict(AttachmentPath path, int zoomIndex, float zoomProgress, boolean sendImmediately) {
        predictZoomIndex = Math.max(0, zoomIndex);
        predictZoomProgress = zoomProgress;
        predictZoomTimestamp = System.currentTimeMillis();
        if (sendImmediately) {
            pendingZoomPath = null;
            pendingZoomSyncTimestamp = -1L;
            NetworkHelper.sendToServer(new C2SZoomMessage(hand(), path, zoomIndex, zoomProgress));
        } else {
            pendingZoomPath = path;
            pendingZoomSyncTimestamp = System.currentTimeMillis();
        }
    }

    /// > 清除变倍预测（回读配件 NBT 权威值）。由 operator 对账（NBT 一致 / 确认超时）与
    /// > 入口切换（侧瞄切换 / 换枪 / 重绑）调用。
    public void clearZoomPredict() {
        predictZoomIndex = -1;
        predictZoomProgress = -1.0F;
        predictZoomTimestamp = -1L;
        pendingZoomPath = null;
        pendingZoomSyncTimestamp = -1L;
    }

    /// > 玩家请求切换模式（模块模式 / 快慢机）。客户端<b>不做预测</b>：仅起一个锁定窗口
    /// > （{@link ModeSwitchPendingSegment}）锁住后续操作，等待服务端权威 NBT 同步回来后由
    /// > {@link #reconcileModes()} 真正应用。已在切换 / 过渡中则忽略本次请求。
    /// > <p>
    /// > 发送 C2S 请求由调用方（按键 / {@link #setModuleMode}）负责。
    public void requestModeSwitch() {
        if (isModeSwitching()) {
            return;
        }
        if (!tracks.isIdle()) {
            return;
        }
        tracks.add(new ModeSwitchPendingSegment(ModeSwitchPendingSegment.DEFAULT_TIMEOUT_MS));
    }

    /// > 对账权威 NBT，把模块模式 / 快慢机档位的变化真正落到客户端表现（重建开火序列 / 播过渡 / 播拨杆动画）。
    /// > 由 {@link #tick()} 每刻调用，覆盖：玩家请求被服务端确认、纯服务端驱动的模式变化。
    /// > 任一变化被应用后取消等待锁定段（{@link ModeSwitchPendingSegment}）解锁。
    private void reconcileModes() {
        /// > 收枪 / 切枪窗口期间（PutAwaySegment 活跃）：会话仍绑定旧枪、applied* 是旧枪缓存，
        /// > 而手持物可能已切成另一把枪。此时一切等收枪结束的 draw() 按新枪重设 applied*，
        /// > 绝不能对账——否则会读新枪 NBT 与旧枪缓存的差异，误判为「切换」而重建 / 播过渡，把收枪动画吃掉。
        /// > 注意不能用 sameHeldGun 判据：两把同 id 枪会被判为「同一把」而失效。
        if (tracks.has(PutAwaySegment.TOKEN)) {
            return;
        }
        ItemStack host = getCurrentItem();
        /// > 下挂被卸载 / 换枪导致下挂不在，但 applied 仍是 UNDER_BARREL（权威 NBT 可能尚未同步回 PRIMARY）：
        /// > 先本地回退主枪并静默重建。服务端也会因找不到下挂而写 NBT=PRIMARY，最终一致。
        if (appliedMode == GunModuleMode.UNDER_BARREL
                && MountedWeaponHelper.findInstalledMountedWeapon(host).isEmpty()) {
            appliedMode = GunModuleMode.PRIMARY;
            appliedFireModeIndex = FireModeHelper.getFireModeIndex(host);
            initFireBehavior(getCurrentItem());
            return;
        }
        /// > 是否玩家主动请求的切换：存在 pending 锁定段即为主动切换（区别于换成另一把同 id 枪 / 服务端纠正）。
        /// > 仅主动切换才播拨杆 / 过渡动画；换枪或服务端纠正只静默重建功能状态，不播动画。
        boolean requested = tracks.has(ModeSwitchPendingSegment.TOKEN);
        GunModuleMode authMode = ModuleModeHelper.getModuleMode(host);
        if (authMode != appliedMode) {
            applyModuleMode(authMode, requested);
            tracks.cancel(ModeSwitchPendingSegment.TOKEN);
            return;
        }
        /// > 模块模式未变；主枪模式下对账快慢机档位（appliedFireModeIndex 即当前 FireController 的重建键）。
        if (appliedMode == GunModuleMode.PRIMARY) {
            int authFireIndex = FireModeHelper.getFireModeIndex(host);
            if (authFireIndex != appliedFireModeIndex) {
                appliedFireModeIndex = authFireIndex;
                initFireBehavior(activeStack());
                if (requested) {
                    var ani = FirstPersonRenderHandler.getActiveAnimationInstance(hand());
                    if (ani instanceof GunAnimationInstance gunAni) {
                        gunAni.switchFireMode();
                    }
                }
                tracks.cancel(ModeSwitchPendingSegment.TOKEN);
            }
        }
    }

    /// > 按权威模块模式重建客户端开火序列 / 冷却轨。仅在玩家主动切换（{@code playTransition}）且涉及下挂模式翻转时播过渡。
    /// > 由 {@link #reconcileModes()} 调用，不发包、不写 NBT（纯表现落地）。换枪 / 服务端纠正时 {@code playTransition=false}，静默重建。
    private void applyModuleMode(@Nonnull GunModuleMode newMode, boolean playTransition) {
        GunModuleMode oldMode = this.appliedMode;
        /// > 进入下挂模式：找不到下挂武器则退回主枪模式。下挂副本由 activeStack 按宿主实例身份缓存。
        if (newMode == GunModuleMode.UNDER_BARREL
                && MountedWeaponHelper.findInstalledMountedWeapon(getCurrentItem()).isEmpty()) {
            this.appliedMode = GunModuleMode.PRIMARY;
        } else {
            this.appliedMode = newMode;
        }
        this.appliedFireModeIndex = FireModeHelper.getFireModeIndex(getCurrentItem());
        initFireBehavior(activeStack());
        /// > 仅玩家主动切换且涉及下挂模式翻转（PRIMARY↔UNDER_BARREL）时播过渡。
        if (playTransition && oldMode != this.appliedMode
                && (oldMode == GunModuleMode.UNDER_BARREL || this.appliedMode == GunModuleMode.UNDER_BARREL)) {
            beginModeTransition();
        }
    }

    /// > 开始模式切换过渡（轻量客户端表现锁）。durationMs 由动画侧根据是否有过渡动画回填，
    /// > 这里先按缺省时长起锁，动画侧若拿到更长的过渡动画时长会覆盖延长。
    private void beginModeTransition() {
        tracks.add(new ModeTransitionSegment(DEFAULT_MODE_TRANSITION_MS));
    }

    /// > 动画侧拿到过渡动画实际时长后回填，延长过渡锁定窗口（仅允许延长，不缩短）。
    public void extendModeTransition(long durationMs) {
        ModeTransitionSegment t = tracks.get(ModeTransitionSegment.TOKEN);
        if (t != null) {
            t.extend(durationMs);
        }
    }

    /// > 是否正处于模式切换中（客户端表现锁）。包括「已发请求等待服务端确认」的锁定窗口
    /// > （{@link ModeSwitchPendingSegment}）与「确认后播放过渡」的过渡段（{@link ModeTransitionSegment}）。
    /// > 期间禁止开火/换弹/检视/再切换。
    public boolean isModeSwitching() {
        return tracks.has(ModeSwitchPendingSegment.TOKEN) || tracks.has(ModeTransitionSegment.TOKEN);
    }

    /// > 模式切换过渡进度 0~1（无过渡时返回 1）。动画侧据此驱动持枪姿态权重淡入淡出。
    public float modeTransitionProgress() {
        ModeTransitionSegment t = tracks.get(ModeTransitionSegment.TOKEN);
        return t != null ? t.progress() : 1.0f;
    }

    /// > 强制结束模式切换过渡（切枪/卸下挂等场景，避免卡在锁定态）。
    public void clearModeTransition() {
        tracks.cancel(ModeTransitionSegment.TOKEN);
    }

    private void initFireBehavior(ItemStack stack) {
        GunData data = MountedWeaponHelper.resolveGunData(stack);
        if (data != null) {
            installFireTracks(stack, data);
            this.reloadPolicy = ReloadPolicy.of(data);
            return;
        }
        this.fireController = null;
        this.reloadPolicy = null;
    }

    /// > 安装/替换客户端开火控制器与冷却片段。端差异仅由 Hooks 回调注入（客户端表现预测）。
    /// > 冷却由单发片段在出膛 notify 时自动驱动，operator 不再手动刷新。
    private void installFireTracks(ItemStack gun, GunData data) {
        FireController.Hooks hooks = FireController.Hooks.builder(
                        /// > 主发：播放动画/音效/后坐力，预消耗弹药，发送 C2S
                        this::onClientFireCallback)
                /// > burst 自驱回调：客户端逐发触发表现 + C2S
                .burstShot(this::onClientBurstFire)
                /// > 主发出膛后端副作用：更新 muzzle-flash 时间戳 + 阻止冲刺 + 起冲刺锁
                .postPrimaryFire(this::onShotFired)
                /// > burst 后续子弹的服务端发射 / 补膛：客户端不使用（不写 NBT，服务端权威同步）
                /// > 拉栓窗口启动：统一起自愈屏蔽锁，覆盖射击/burst/补膛/自愈所有路径，
                /// > 避免窗口结束后服务端补膛尚未同步回来时被 tryAutoChamber 重复补偿拉栓
                .onBoltStart(this::lockAutoChamber)
                .build();
        tracks.add(new CooldownSegment(() -> computeCooldownMs(activeStack())));
        this.fireController = FireController.create(tracks, this::activeStack, data, hooks);
    }

    private static long computeCooldownMs(ItemStack gun) {
        return AttributeDataHelper.shootIntervalMs(gun);
    }

    /// > 客户端 trigger 发射回调（首发）：播放动画/音效/后坐力，预消耗弹药，发送 C2S。
    private ShootResult onClientFireCallback(float yaw, float pitch, float damageMult, float speedMult, int muzzleIndex) {
        BoltType boltType = fireController != null ? fireController.getBoltType() : BoltType.OPEN;
        boolean ammoReady = boltType.usesChamber()
                ? AmmoDataHelper.isChambered(activeStack())
                : AmmoDataHelper.hasAmmo(activeStack());
        if (!ammoReady) {
            return ShootResult.NO_AMMO;
        }

        var ani = FirstPersonRenderHandler.getActiveAnimationInstance(hand());
        if (ani instanceof GunAnimationInstance gunAni) {
            gunAni.fire();
            // FIXME REVIEW placeholder shoot sound ares:ak47/ak47_shoot kept from respawn, also played in onClientBurstFire;
            // replace with the gun's own sound later.
            player.playSound(
                    SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Ares.MOD_ID, "ak47/ak47_shoot"))
            );
        }
        CameraEventHandler.initialCameraRecoil();

        predictConsume(boltType);

        NetworkHelper.sendToServer(new C2SFireMessage(yaw, pitch, C2SFireMessage.ACTION_PRESS, hand()));

        return ShootResult.SUCCESS;
    }

    /// > 客户端 burst 后续子弹回调：触发动画/音效/后坐力，消耗弹药预测，发送 C2S。
    /// > （冷却由开火序列片段在出膛 notify 时驱动，此处不再手动刷新。）
    private void onClientBurstFire() {
        BoltType boltType = fireController != null ? fireController.getBoltType() : BoltType.OPEN;
        boolean ammoReady = boltType.usesChamber()
                ? AmmoDataHelper.isChambered(activeStack())
                : AmmoDataHelper.hasAmmo(activeStack());
        if (!ammoReady) {
            return;
        }
        var ani = FirstPersonRenderHandler.getActiveAnimationInstance(hand());
        if (ani instanceof GunAnimationInstance gunAni) {
            gunAni.fire();
        }
        CameraEventHandler.initialCameraRecoil();
        player.playSound(
                SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Ares.MOD_ID, "ak47/ak47_shoot"))
        );
        predictConsume(boltType);
        /// > 更新 muzzle-flash 时间戳
        this.lastShootTimestamp = System.currentTimeMillis();
        NetworkHelper.sendToServer(new C2SFireMessage(
                player.getYRot(), player.getXRot(), C2SFireMessage.ACTION_PRESS, hand()));
    }

    /// > 客户端弹药消耗预测。
    /// > <p>
    /// > 只对 open 预测弹匣计数（纯数值，错了下次同步自愈，不影响门禁逻辑）。
    /// > closed/manual <b>绝不写 chambered</b>——它是服务端权威、且自愈逻辑读它分支，
    /// > 客户端写入会在丢同步时造成死锁（客户端以为已上膛而停止请求，服务端仍未上膛）。
    /// > chamber 类型的开火门禁靠 RPM 冷却 + bolt 窗口（本地运行时状态），不依赖预测 chambered。
    /// > <p>
    /// > 下挂模式下 {@code activeStack()} 返回宿主属性缓存里的下挂副本，原地消耗即作为预判（随宿主缓存存活，
    /// > 服务端同步重建宿主实例时缓存重建自愈）。主枪模式下 {@code activeStack()} 即手持活引用，原地消耗即生效。
    private void predictConsume(BoltType boltType) {
        if (!boltType.usesChamber()) {
            AmmoDataHelper.consumeAmmo(activeStack(), 1);
        }
    }

    /// > 客户端驱动的自愈拉栓：检测「持枪 + NORMAL + closed/manual + 未上膛 + 弹匣有弹 + 非 bolting」，
    /// > 满足时播拉栓表现、起本地 bolt 窗口并发送拉栓意图。节流轨避免每 tick 重复发包。
    /// > 补膛的权威执行在服务端，客户端仅做表现预测，错了靠同步自愈。
    private void tryAutoChamber() {
        if (fireController == null) {
            return;
        }
        if (!tracks.isIdle()) {
            return;
        }

        BoltType boltType = fireController.getBoltType();
        if (!boltType.usesChamber()) {
            return;
        }

        ItemStack gun = activeStack();
        if (AmmoDataHelper.isChambered(gun)) {
            return;
        }
        if (AmmoDataHelper.getAmmoCount(gun) <= 0) {
            return;
        }
        if (fireController.isBolting()) {
            return;
        }

        /// > 客户端 Bolt 请求锁定：本地 bolt 窗口 + 网络容差内不重复发包。
        if (tracks.has(AutoChamberRequestSegment.TOKEN)) {
            return;
        }

        /// > 客户端表现：起拉栓窗口（仅本地运行时状态）。BOLT_ACTION 投影由 BoltSegment 自动驱动，
        /// > 拉栓动画由状态机 gameplay_bolting query 自动驱动。
        /// > 不写任何 NBT——补膛由服务端权威执行并同步回 chambered。若本次发包丢失，
        /// > 客户端读到的 chambered 仍为 false，锁定窗口过后会再次请求。
        /// > 自愈屏蔽锁由 startBoltWindow 经 onBoltStart 回调（lockAutoChamber）统一设置。
        fireController.startBoltWindow();
        NetworkHelper.sendToServer(new C2SBoltMessage(hand()));
    }

    /// > 起自愈拉栓屏蔽锁。由 FireController 在任意拉栓窗口启动时经 onBoltStart 回调触发，
    /// > 覆盖射击后 / burst 后 / 补膛 / 自愈所有路径。锁存活到「bolt 窗口 + 网络容差」，
    /// > 期间 tryAutoChamber 不重复发起拉栓，避免窗口结束、服务端补膛尚未同步时被重复补偿。
    private void lockAutoChamber(long boltWindowMs) {
        tracks.add(AutoChamberRequestSegment.afterClientBoltRequest(boltWindowMs));
    }

    @Override
    public void reload() {
        startReloadVisual(true);
    }

    /// > 当前枪械是否使用膛（closed/manual）。无开火控制器时按 open 处理（不使用膛）。
    private boolean usesChamberBolt() {
        return fireController != null && fireController.getBoltType().usesChamber();
    }

    /// > 换弹表现的公共实现。{@code sendPacket=true} 时额外发送 {@link C2SReloadMessage}（玩家主动换弹），
    /// > {@code false} 时仅做客户端表现（如弹种切换后补播动画）。
    /// > <p>
    /// > 切弹种补播表现（{@code sendPacket=false}）是<b>强制表现</b>：服务端已权威决定换弹（退弹 + 重装），
    /// > 客户端只补动画。因此跳过 {@link ReloadPlan#canReload}（弹匣容量 / 背包余弹这类「主动换弹」专属前置）——
    /// > 切弹种时客户端 stack 仍是旧状态（旧弹种、弹匣可能满），用它判断会误挡掉表现。
    private void startReloadVisual(boolean sendPacket) {
        if (isModeSwitching()) {
            return;
        }
        if (!tracks.gate().canReload()) {
            return;
        }
        if (reloadInterruptCooldown.isRunning()) {
            return;
        }
        if (reloadPolicy == null) {
            return;
        }
        ItemStack target = activeStack();
        GunData data = MountedWeaponHelper.resolveGunData(target);
        if (data == null) {
            return;
        }
        if (sendPacket && !ReloadPlan.canReload(player, target, data)) {
            return;
        }
        /// > 切弹种补播：弹匣即将被退空，按空仓换弹表现；主动换弹按当前弹量判断空仓与否
        boolean empty = !sendPacket || AmmoDataHelper.getAmmoCount(target) <= 0;
        long reloadMs = ReloadPlan.totalDurationMs(player, target, data);
        // -1 lets the animation context derive the shell loop count from data.
        int shellLoops = -1;
        /// > 空仓换弹自带上膛：服务端在换弹完成步权威写 chamber，但客户端 chambered 要等 S2C 同步才翻。
        /// > 若不锁定，换弹片段完成的同一 tick 里 tryAutoChamber 会因 chambered=false 立即多发起一次自愈拉栓
        /// > （多余的拉栓动画 + 发包）。故 chamber 类枪的空仓换弹完成时起一段自愈屏蔽锁盖住同步间隙。
        boolean lockAutoChamberOnComplete = empty && usesChamberBolt();
        Runnable onComplete = lockAutoChamberOnComplete
                ? () -> tracks.add(new AutoChamberRequestSegment(AutoChamberRequestSegment.NETWORK_TOLERANCE_MS))
                : null;
        tracks.add(ReloadSegment.client(reloadMs, reloadPolicy, onComplete));
        this.player.setSprinting(false);
        var ani = FirstPersonRenderHandler.getActiveAnimationInstance(hand());
        if (ani instanceof GunAnimationInstance gunAni) {
            gunAni.reload(empty, (int) (reloadMs / 50L), shellLoops);
        }
        if (sendPacket) {
            NetworkHelper.sendToServer(new C2SReloadMessage(C2SReloadMessage.ACTION_START, hand()));
        }
    }

    @Override
    public void aim(boolean aiming) {
        /// > 形态禁瞄（如单手双持）：强制不瞄准，且不发包。
        if (!profile().canAim()) {
            if (this.isAiming) {
                this.isAiming = false;
                NetworkHelper.sendToServer(new C2SAimMessage(false, hand()));
            }
            return;
        }
        if (aiming && tracks.has(InspectSegment.TOKEN)) {
            tracks.cancel(InspectSegment.TOKEN);
        }
        if (aiming && tracks.has(ReloadSegment.TOKEN) && !canAimWhileReloading()) {
            return;
        }
        if (this.isAiming != aiming) {
            this.isAiming = aiming;
            NetworkHelper.sendToServer(new C2SAimMessage(aiming, hand()));
        }
    }

    @Override
    public float aimingProgress(double partialTicks) {
        return Mth.lerp((float) partialTicks, lastAimProgress, aimProgress);
    }

    @Override
    public List<String> debugTracks() {
        return tracks.debugLines();
    }

    @Override
    public void inspect() {
        if (!canInspectWeapon()) {
            return;
        }
        if (isModeSwitching()) {
            return;
        }
        if (!tracks.isIdle() || aimProgress > 0 || this.player.isSprinting()) {
            return;
        }
        tracks.add(new InspectSegment());
        var ani = FirstPersonRenderHandler.getActiveAnimationInstance(hand());
        if (ani instanceof GunAnimationInstance gunAni) {
            gunAni.inspect();
        }
        NetworkHelper.sendToServer(new C2SInspectMessage());
    }

    public void cancelReload() {
        if (tracks.has(ReloadSegment.TOKEN)) {
            tracks.cancel(ReloadSegment.TOKEN);
        }

    }

    @Override
    public void tick() {
        long currentTime = System.currentTimeMillis();
        if (lastTickTimestamp == -1) {
            lastTickTimestamp = currentTime;
        }
        long deltaMs = currentTime - lastTickTimestamp;
        lastTickTimestamp = currentTime;

        /// > 确认是否需要重建开火模式策略
        reconcileModes();

        reloadInterruptCooldown.tick(deltaMs);
        if (fireController != null) {
            fireController.tick();
        }
        tracks.tick(deltaMs);

        /// > 视图位切换过渡收敛后落定激活视图位，释放动力学定时器
        if (pendingAimViewIndex >= 0 && aimViewSwitchDynamics != null && aimViewSwitchDynamics.isStable()) {
            activeAimViewIndex = pendingAimViewIndex;
            pendingAimViewIndex = -1;
            aimViewSwitchDynamics.cancel();
            aimViewSwitchDynamics = null;
        }

        /// > 滚轮无级变倍防抖上报：滚停超时后把最终预测值落一次服务端权威 NBT
        if (pendingZoomPath != null && System.currentTimeMillis() - pendingZoomSyncTimestamp >= ZOOM_DEBOUNCE_MS) {
            NetworkHelper.sendToServer(new C2SZoomMessage(hand(), pendingZoomPath, predictZoomIndex, predictZoomProgress));
            pendingZoomPath = null;
            pendingZoomSyncTimestamp = -1L;
        }

        /// > 客户端驱动的自愈拉栓：持枪 + NORMAL + closed/manual + 未上膛 + 弹匣有弹 + 非 bolting + 过节流窗口
        /// > （tryAutoChamber 内部自查开火序列片段是否存在）
        tryAutoChamber();

        /// > 冲刺校验
        ActionGate gate = tracks.gate();
        if (!gate.canSprint()) {
            ReloadSegment reload = tracks.get(ReloadSegment.TOKEN);
            if (reload != null && reload.canSprintInterrupt() && this.player.isSprinting()) {
                cancelReload();
                NetworkHelper.sendToServer(new C2SReloadMessage(C2SReloadMessage.ACTION_CANCEL, hand()));
            } else {
                this.player.setSprinting(false);
            }
        }

        /// > 瞄准进度推进（纯表现插值，每 tick ±0.33，与 deltaMs 无关）
        boolean canAimNow = gate.canAim()
                && (!tracks.has(ReloadSegment.TOKEN) || canAimWhileReloading());
        lastAimProgress = aimProgress;
        if (isAiming && canAimNow) {
            aimProgress = Math.min(aimProgress + AIM_STEP, 1f);
        } else if (aimProgress > 0f) {
            aimProgress = Math.max(aimProgress - AIM_STEP, 0f);
        }
        if (aimProgress > 0) {
            this.player.setSprinting(false);
        }
    }

    @Nonnull
    @Override
    public GunModuleMode getModuleMode() {
        return appliedMode;
    }

    @Override
    public void setModuleMode(@Nonnull GunModuleMode mode) {
        if (!canSwitchWeaponMode(mode)) {
            return;
        }
        if (mode == appliedMode) {
            return;
        }
        if (!tracks.isIdle()) {
            return;
        }
        if (mode == GunModuleMode.UNDER_BARREL && MountedWeaponHelper.findInstalledMountedWeapon(this.getCurrentItem()).isEmpty()) {
            return;
        }
        /// > 无预测：起锁定窗口 + 发请求，等待权威 NBT 同步回来由 reconcileModes 应用。
        requestModeSwitch();
        NetworkHelper.sendToServer(new C2SModuleModeMessage((byte) mode.ordinal()));
    }

    private boolean canAimWhileReloading() {
        GunData data = MountedWeaponHelper.resolveGunData(activeStack());
        return data == null || data.isAllowAimWhileReloading();
    }
}
