package cn.ussshenzhou.ares.client.animation.runtime;

import com.mojang.logging.LogUtils;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.AnimationRateLimiter;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.time.AnimationClocks;
import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.EulerAdditiveBlender;
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender;
import cn.ussshenzhou.ares.client.animation.controller.StandardWeaponController;
import cn.ussshenzhou.ares.client.animation.controller.StandardWeaponControlData;
import cn.ussshenzhou.ares.client.animation.context.AnimationEventDispatcher;
import cn.ussshenzhou.ares.client.animation.context.WeaponAnimationContext;
import cn.ussshenzhou.ares.client.animation.mounted.HostOverlayBridge;
import cn.ussshenzhou.ares.client.model.BedrockGunModel;
import cn.ussshenzhou.ares.client.resource.ClientAssetsManager;
import cn.ussshenzhou.ares.client.resource.display.GunDisplayInstance;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/// @author TACZ, USS_Shenzhou
public class AnimationRuntime {
    private static final EulerAdditiveBlender BLENDER = new SimpleEulerAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);

    private final LocalPlayer player;
    private final GunDisplayInstance display;
    private final BedrockGunModel model;
    private final WeaponAnimationContext context;
    private final StandardWeaponController controller;
    private final AnimationEventDispatcher eventSink;
    private ItemStack itemStack;
    private Pose cachedAnimationPose = DummyPose.INSTANCE;
    private Pose cachedPose = DummyPose.INSTANCE;
    private long lastTickErrorLogMs = 0L;

    private final AnimationRateLimiter<Pose> poseRateLimiter =
            new AnimationRateLimiter<>(AnimationClocks.client(), AnimationRateLimiter.FPS_120);

    public AnimationRuntime(LocalPlayer player, ItemStack itemStack, GunDisplayInstance display) {
        this(player, itemStack, display, HostOverlayBridge.NONE);
    }

    public AnimationRuntime(LocalPlayer player, ItemStack itemStack, GunDisplayInstance display, HostOverlayBridge hostOverlayBridge) {
        this(player, itemStack, display, hostOverlayBridge, net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    public AnimationRuntime(LocalPlayer player, ItemStack itemStack, GunDisplayInstance display,
                            net.minecraft.world.InteractionHand hand) {
        this(player, itemStack, display, HostOverlayBridge.NONE, hand);
    }

    public AnimationRuntime(LocalPlayer player, ItemStack itemStack, GunDisplayInstance display,
                            HostOverlayBridge hostOverlayBridge, net.minecraft.world.InteractionHand hand) {
        this.player = player;
        this.itemStack = itemStack;
        this.display = display;
        this.model = display.getGunModel();
        this.context = new WeaponAnimationContext(player, itemStack, display, hand);
        this.context.setHostOverlayBridge(hostOverlayBridge);
        this.eventSink = new AnimationEventDispatcher(context);
        this.controller = createController(display);
    }

    public void triggerDraw() {
        controller.draw();
        tick(0.0f);
    }

    public void triggerPutAway() {
        controller.putAway();
    }

    public void fire() {
        controller.fire();
    }

    /// > 取出本帧动画 `particle_effects` 关键帧累积的粒子请求并清空。由渲染层每帧调用。
    public List<ParticleEffectData> consumePendingParticles() {
        return eventSink.consumePendingParticles();
    }

    /// > 取出本帧动画累积的抛壳请求（抛壳窗序号列表）并清空。由渲染层每帧调用。
    public List<Integer> consumePendingShellEjects() {
        return eventSink.consumePendingShellEjects();
    }

    public void reload(boolean empty) {
        controller.reload(empty);
    }

    public void reload(boolean empty, int gameplayTicks) {
        controller.reload(empty, gameplayTicks);
    }

    public void reload(boolean empty, int gameplayTicks, int shellLoopsOverride) {
        controller.reload(empty, gameplayTicks, shellLoopsOverride);
    }

    public void inspect() {
        controller.inspect();
    }

    public void switchFireMode() {
        controller.switchFireMode();
    }

    /// > 挂载 host overlay（下挂安装且命中 combo profile 时调用）。
    public void attachHostOverlay(cn.ussshenzhou.ares.client.animation.mounted.MountedOverlayResolved resolved) {
        controller.attachHostOverlay(resolved);
    }

    /// > 卸载 host overlay。
    public void detachHostOverlay() {
        controller.detachHostOverlay();
    }

    /// > 切换 host overlay 操作差量段（下挂 reload/inspect 时同步驱动主枪手部）。
    public void playHostOverlayOperation(String actionName, float expectedDurationSeconds) {
        controller.playHostOverlayOperation(actionName, expectedDurationSeconds);
    }

    /// > 操作差量归零（动作结束回 idle 持枪）。仅当当前 operation 仍是 `actionName` 时才清除。
    public void clearHostOverlayOperation(String actionName) {
        controller.clearHostOverlayOperation(actionName);
    }

    /// > 播放模式切换过渡段。返回过渡动画时长（秒），无动画返回 0。
    public float playHostOverlayTransition(boolean toUnderBarrel) {
        return controller.playHostOverlayTransition(toUnderBarrel);
    }

    public boolean hasHostOverlay() {
        return controller.hasHostOverlay();
    }

    public HostOverlayBridge hostOverlayBridge() {
        return controller;
    }

    public void tick(float partialTicks) {
        try {
            context.update(partialTicks);
            controller.tick(partialTicks);
            /// > fire 脉冲是瞬时信号，状态机本 tick 已读取（用于打断 reload/inspect），此处清除。
            context.clearFirePulse();
            /// > 姿态求值限频：未到间隔则复用上一帧缓存，跳过重的合成；副作用已在 controller.tick 完成。
            if (poseRateLimiter.shouldUpdate()) {
                cachedAnimationPose = controller.evaluatePose();
                cachedPose = BLENDER.blend(model.getBindPose(), cachedAnimationPose);
            }
        } catch (Throwable t) {
            /// > tick 链路若抛异常会被上游事件总线吞掉，导致 cachePose 停止更新、画面冻结。
            /// > 此处显式打印堆栈以便定位（限频，避免每帧刷屏）。
            long now = System.currentTimeMillis();
            if (now - lastTickErrorLogMs > 1000L) {
                lastTickErrorLogMs = now;
                LogUtils.getLogger().error("[TACZ] AnimationRuntime.tick 抛出异常，baseState={}, fireSection={}",
                        controller.getBaseStateId(), controller.getFireSectionName(), t);
            }
        }
    }

    /// > 返回当前动画姿态（合成后，未叠 bind）。走限频缓存：与 [#getCachedAnimationPose()] 一致，
    /// > 不绕过 `poseRateLimiter` 重新求值，避免高刷屏下每次调用都触发整套合成。
    public Pose getPose() {
        return cachedAnimationPose;
    }

    public Pose getCachedPose() {
        return cachedPose;
    }

    public Pose getCachedAnimationPose() {
        return cachedAnimationPose;
    }

    public void updateItem(ItemStack stack) {
        this.itemStack = stack;
        context.updateItem(stack);
    }

    public ItemStack currentItem() {
        return itemStack;
    }

    public String getControllerState() {
        return controller.getBaseStateId();
    }

    public float getActionPlaybackSpeed() {
        return 1.0f;
    }

    public String getActionSectionName() {
        return controller.getFireSectionName();
    }

    private StandardWeaponController createController(GunDisplayInstance display) {
        Identifier controllerId = display.getControllerId();
        StandardWeaponControlData definition = controllerId != null ? ClientAssetsManager.INSTANCE.getAnimationController(controllerId) : null;
        if (definition == null) {
            LogUtils.getLogger().warn("[TACZ] Animation controller missing: {}", controllerId);
            definition = new StandardWeaponControlData();
        }
        /// > montage 由 controller 经 display 按需懒加载并缓存，这里不再预加载。
        return new StandardWeaponController(definition, eventSink, context);
    }
}
