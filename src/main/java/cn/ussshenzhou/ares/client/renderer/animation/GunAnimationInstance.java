package cn.ussshenzhou.ares.client.renderer.animation;

import cn.ussshenzhou.ares.attachment.entity.LocalGunOperator;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import cn.ussshenzhou.ares.gun.TimelessHelperC;
import cn.ussshenzhou.ares.util.GunModuleMode;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.client.animation.mounted.HostOverlayBridge;
import cn.ussshenzhou.ares.client.animation.runtime.AnimationRuntime;
import cn.ussshenzhou.ares.client.model.BedrockGunModel;
import cn.ussshenzhou.ares.client.resource.ClientAssetsManager;
import cn.ussshenzhou.ares.client.resource.display.GunDisplay;
import cn.ussshenzhou.ares.gun.util.MountedWeaponHelper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import cn.ussshenzhou.ares.gun.util.DualWieldHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;
import org.joml.Quaternionf;

/// > 第一人称枪械动画实例，存放在玩家实体的IGunHolderCapability中。
/// > 仅承载 animation 运行时：host 由 [AnimationRuntime] 求值，下挂由独立的 mounted runtime 求值。
/// @author TACZ, USS_Shenzhou
public class GunAnimationInstance implements IFPAnimationInstance {
    private Quaternionf cameraRotation = new Quaternionf();
    private Pose cachePose = DummyPose.INSTANCE;
    private Pose cachedAnimationPose = DummyPose.INSTANCE;

    private AnimationRuntime animationRuntime;
    private BedrockGunModel model;

    private Pose mountedCachePose = DummyPose.INSTANCE;
    private Identifier mountedKeyGunDisplayId;
    private Identifier mountedKeyMountedId;
    /// > animation 下挂运行时（与 host 的 animationRuntime 平行）。下挂 display 含 controller 时使用，
    /// > 每 tick 求值后写入 mountedCachePose，复用旧的 renderMountedWeaponAttachment 渲染链路。
    private AnimationRuntime mountedAnimationRuntime;

    /// > 需要保留 player 引用以便重载时使用
    private final LocalPlayer player;

    /// > 本实例所属的手（主/副）。双持时据此选用单手按手变体 display。
    private final InteractionHand hand;
    /// > 顶层（双手）display，作为变体解析的基准与回退。
    private GunDisplay baseDisplay;
    /// > 当前生效的 display（双持时为按手变体，否则为 base）。model/runtime 均据此构建。
    private GunDisplay currentDisplay;

    private ItemStack itemStack;

    private boolean isDrawn = false;
    /// > 上一帧的模块模式镜像，用于检测 PRIMARY↔UNDER_BARREL 翻转以触发 host overlay 过渡段播放。
    private GunModuleMode lastModuleMode = GunModuleMode.PRIMARY;
    /// > 本实例创建时固化的「是否以双手形态持握并霸占副手视野」。
    /// > 绑定到实例固定的渲染形态，过渡期间稳定不变，避免实时 player 判定在收枪/掏枪过渡中抖动。
    private final boolean occupiesView;

    public GunAnimationInstance(LocalPlayer player, ItemStack itemStack, GunDisplay display) {
        this(player, itemStack, display, InteractionHand.MAIN_HAND);
    }

    public GunAnimationInstance(LocalPlayer player, ItemStack itemStack, GunDisplay display, InteractionHand hand) {
        this.player = player;
        this.itemStack = itemStack;
        this.hand = hand;
        this.baseDisplay = display;
        this.currentDisplay = resolveEffectiveDisplay();
        this.model = currentDisplay.getGunModel();
        if (currentDisplay.hasAnimationController()) {
            this.animationRuntime = new AnimationRuntime(player, itemStack, currentDisplay, hand);
        }
        /// > 主手以双手形态持握（非单手形态）时霸占副手视野。仅主手实例此值有意义（SBM 只问主手）。
        this.occupiesView = hand == InteractionHand.MAIN_HAND
                && !DualWieldHelper.isOneHandedForm(player, hand);
    }

    /// > 解析本实例当前生效的 display：本手为单手形态时按手选用单手变体（主手 right / 副手 left），
    /// > 否则用顶层（双手）display。无变体声明时 `forHand` 自身会回退 base。
    /// >
    /// > 单手形态判定见 [DualWieldHelper#isOneHandedForm]：副手始终单手，主手仅双持时单手。
    private GunDisplay resolveEffectiveDisplay() {
        if (baseDisplay.hasOneHandVariants() && DualWieldHelper.isOneHandedForm(player, hand)) {
            return baseDisplay.forHand(hand);
        }
        return baseDisplay;
    }

    /// > 当前生效 display（双持按手变体或本体）。供渲染器取用同一 model 对象施加 pose。
    /// >
    /// > 形态切换（进入/退出双持）由 SBM 经「渲染变体键」统一驱动重建实例，本实例创建后
    /// > [#currentDisplay] 即固定，不在 tick 内自行切换，避免与 SBM 实例生命周期冲突。
    public GunDisplay effectiveDisplay() {
        return currentDisplay;
    }

    private void clearMounted() {
        mountedCachePose = DummyPose.INSTANCE;
        mountedKeyGunDisplayId = null;
        mountedKeyMountedId = null;
        mountedAnimationRuntime = null;
        /// > 下挂卸载时同步卸载 host_overlay（host runtime 不一定存在，需判空）。
        if (animationRuntime != null) {
            animationRuntime.detachHostOverlay();
        }
        /// > 下挂卸载时强制结束可能进行中的模式切换过渡，避免卡在客户端锁定态。
        lastModuleMode = GunModuleMode.PRIMARY;
        IGunOperator.asGunHolder(player).ifPresent(op -> {
            if (op instanceof LocalGunOperator localOp) {
                localOp.clearModeTransition();
            }
        });
    }

    /// > animation 路径下的下挂安装/卸载/切换检测：
    /// > 当下挂 display 含 animation controller 时，创建独立的 mounted runtime 并触发 draw；
    /// > 否则卸载下挂运行时与可能存在的 host overlay。
    private void updateMountedAnimationIfNeeded() {
        IGun gun = IGun.asGun(itemStack);
        if (gun == null) {
            clearMounted();
            return;
        }
        var mountedOpt = MountedWeaponHelper.findInstalledMountedWeapon(itemStack);
        var mountedIdOpt = MountedWeaponHelper.findInstalledMountedWeaponId(itemStack);
        if (mountedOpt.isEmpty() || mountedIdOpt.isEmpty()) {
            clearMounted();
            return;
        }

        Identifier gunDisplayId = gun.getDisplayId(itemStack);
        Identifier mountedId = mountedIdOpt.get();
        if (mountedAnimationRuntime != null
                && mountedId.equals(mountedKeyMountedId)
                && gunDisplayId.equals(mountedKeyGunDisplayId)) {
            return;
        }

        ItemStack mountedStack = mountedOpt.get();
        var displayOpt = TimelessHelperC.getMountedWeaponDisplayForGun(itemStack, mountedStack);
        if (displayOpt.isEmpty() || !displayOpt.get().hasAnimationController()) {
            /// > 下挂无 animation controller：不挂载下挂运行时，并卸载可能存在的 host_overlay。
            clearMounted();
            return;
        }

        mountedKeyGunDisplayId = gunDisplayId;
        mountedKeyMountedId = mountedId;
        mountedAnimationRuntime = new AnimationRuntime(player, mountedStack, displayOpt.get(),
                animationRuntime != null ? animationRuntime.hostOverlayBridge() : HostOverlayBridge.NONE);
        mountedAnimationRuntime.triggerDraw();
        /// > 命中显式声明 grip 的 combo 时给 host 挂载 host overlay（host runtime 必须存在）。
        if (animationRuntime != null) {
            var resolved = ClientAssetsManager.INSTANCE.getResolvedOverlay(gunDisplayId, mountedId);
            if (resolved != null) {
                animationRuntime.attachHostOverlay(resolved);
            } else {
                animationRuntime.detachHostOverlay();
            }
        }
    }

    public Pose getMountedCachedPose() {
        return mountedCachePose;
    }

    /// > 热重载：使用新的 Display 实例更新当前动画实例
    public void reload(GunDisplay newDisplay) {
        this.baseDisplay = newDisplay;
        this.currentDisplay = resolveEffectiveDisplay();
        this.model = currentDisplay.getGunModel();
        if (currentDisplay.hasAnimationController()) {
            this.animationRuntime = new AnimationRuntime(player, itemStack, currentDisplay, hand);
        } else {
            this.animationRuntime = null;
        }
        /// > 重新触发 Draw（重置状态到初始）
        this.isDrawn = false;
        this.triggerDraw();
    }

    @Override
    public void triggerDraw() {
        if (isDrawn) {
            return;
        }
        isDrawn = true;

        if (animationRuntime != null) {
            animationRuntime.triggerDraw();
        }
        /// > 立刻更新一次写入pose
        this.tick(0);
    }

    @Override
    public void triggerPutAway() {
        if (animationRuntime != null) {
            animationRuntime.triggerPutAway();
        }
        if (mountedAnimationRuntime != null) {
            mountedAnimationRuntime.triggerPutAway();
        }
    }

    @Override
    public Pose getPose() {
        if (animationRuntime != null) {
            return animationRuntime.getPose();
        }
        return DummyPose.INSTANCE;
    }

    @Override
    public void tick(float partialTicks) {
        if (animationRuntime == null) {
            /// > 无 controller 兜底：保持绑定姿态，不创建任何 legacy 链路。
            cachedAnimationPose = DummyPose.INSTANCE;
            cachePose = model != null ? model.getBindPose() : DummyPose.INSTANCE;
            mountedCachePose = DummyPose.INSTANCE;
            return;
        }

        animationRuntime.tick(partialTicks);
        cachePose = animationRuntime.getCachedPose();
        cachedAnimationPose = animationRuntime.getCachedAnimationPose();
        /// > animation 下挂：检测安装并驱动独立 mounted runtime，写入 mountedCachePose 供渲染。
        updateMountedAnimationIfNeeded();
        /// > 检测模式翻转，触发 host overlay 过渡段（需在 mounted 检测之后，确保 overlay 已挂载）。
        updateModeTransitionIfNeeded();
        if (mountedAnimationRuntime != null) {
            mountedAnimationRuntime.tick(partialTicks);
            mountedCachePose = mountedAnimationRuntime.getCachedPose();
        } else {
            mountedCachePose = DummyPose.INSTANCE;
        }
    }

    public void fire() {
        if (animationRuntime != null) {
            activeAnimationRuntime().fire();
        }
    }

    /// > 取出本帧 host 主枪动画 `particle_effects` 关键帧累积的粒子请求。由渲染层每帧调用。
    /// > 本期仅消费 host 主枪（下挂武器粒子留待后续）。
    public java.util.List<com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData> consumePendingParticles() {
        if (animationRuntime == null) {
            return java.util.List.of();
        }
        return animationRuntime.consumePendingParticles();
    }

    /// > 取出本帧 host 主枪动画累积的抛壳请求（抛壳窗序号列表）。由渲染层每帧调用。
    /// > 本期仅消费 host 主枪（下挂武器抛壳留待后续）。
    public java.util.List<Integer> consumePendingShellEjects() {
        if (animationRuntime == null) {
            return java.util.List.of();
        }
        return animationRuntime.consumePendingShellEjects();
    }

    public void reload(boolean empty) {
        reload(empty, 0);
    }

    public void reload(boolean empty, int gameplayTicks) {
        reload(empty, gameplayTicks, -1);
    }

    public void reload(boolean empty, int gameplayTicks, int shellLoopsOverride) {
        if (animationRuntime != null) {
            activeAnimationRuntime().reload(empty, gameplayTicks, shellLoopsOverride);
        }
    }

    public void inspect() {
        if (animationRuntime != null) {
            activeAnimationRuntime().inspect();
        }
    }

    public void switchFireMode() {
        if (animationRuntime != null) {
            activeAnimationRuntime().switchFireMode();
        }
    }

    /// > 检测模块模式翻转（PRIMARY↔UNDER_BARREL），触发 host overlay 过渡段并把过渡动画时长回填给 operator
    /// > 延长锁定窗口。仅 host overlay 存在时播过渡段；纯权重淡入淡出由 controller 据 operator 进度驱动，无需此处介入。
    private void updateModeTransitionIfNeeded() {
        GunModuleMode current = currentModuleMode();
        if (current == lastModuleMode) {
            return;
        }
        boolean toUnderBarrel = current == GunModuleMode.UNDER_BARREL;
        lastModuleMode = current;
        if (animationRuntime == null || !animationRuntime.hasHostOverlay()) {
            return;
        }
        float durationSeconds = animationRuntime.playHostOverlayTransition(toUnderBarrel);
        if (durationSeconds > 0) {
            IGunOperator.asGunHolder(player).ifPresent(op -> {
                if (op instanceof LocalGunOperator localOp) {
                    localOp.extendModeTransition((long) (durationSeconds * 1000));
                }
            });
        }
    }

    /// > 按当前模块模式选择接收动作事件（fire/reload/inspect）的 animation runtime：
    /// > 下挂模式且 mounted runtime 存在时路由到下挂，否则路由到 host。
    private AnimationRuntime activeAnimationRuntime() {
        if (mountedAnimationRuntime != null && currentModuleMode() == GunModuleMode.UNDER_BARREL) {
            return mountedAnimationRuntime;
        }
        return animationRuntime;
    }

    private GunModuleMode currentModuleMode() {
        return IGunOperator.asGunHolder(player)
                .map(op -> op.getModuleMode(hand))
                .orElse(GunModuleMode.PRIMARY);
    }

    public boolean isAnimationActive() {
        return animationRuntime != null;
    }

    public String getAnimationDebugState() {
        if (animationRuntime == null) {
            return "none";
        }
        return "base=" + animationRuntime.getControllerState()
                + " fire=" + animationRuntime.getActionSectionName();
    }

    @Override
    @Nonnull
    public Quaternionf getCameraRotation() {
        return cameraRotation;
    }

    @Override
    public void setCameraRotation(@Nonnull Quaternionf cameraRotation) {
        this.cameraRotation = cameraRotation;
    }

    @Override
    public Pose getCachedPose() {
        return cachePose;
    }

    public Pose getCachedAnimationPose() {
        return cachedAnimationPose;
    }

    @Override
    public ItemStack currentItem() {
        return itemStack;
    }

    @Override
    public void updateItem(ItemStack stack) {
        this.itemStack = stack;
        if (animationRuntime != null) {
            animationRuntime.updateItem(stack);
        }
    }

    @Override
    public boolean shouldRenderHand() {
        return true;
    }

    @Override
    public boolean occupiesView() {
        return occupiesView;
    }
}
