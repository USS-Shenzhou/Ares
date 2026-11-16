package cn.ussshenzhou.ares.client.animation.controller;

import com.mojang.logging.LogUtils;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.EulerAdditiveBlender;
import com.maydaymemory.mae.blend.InterpolatorBlender;
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender;
import com.maydaymemory.mae.blend.SimpleInterpolatorBlender;
import com.maydaymemory.mae.control.montage.AnimationMontage;
import cn.ussshenzhou.ares.client.animation.context.AnimationEventSink;
import cn.ussshenzhou.ares.client.animation.context.WeaponAnimationContext;
import cn.ussshenzhou.ares.client.animation.layer.AnimationLayer;
import cn.ussshenzhou.ares.client.animation.layer.ControllerLayerBinding;
import cn.ussshenzhou.ares.client.animation.montage.MontageContext;
import cn.ussshenzhou.ares.client.animation.mounted.HostOverlayBridge;
import cn.ussshenzhou.ares.client.animation.mounted.HostOverlayRuntime;
import cn.ussshenzhou.ares.client.animation.mounted.MountedOverlayResolved;
import cn.ussshenzhou.ares.client.animation.statemachine.GunStateMachineGraph;
import cn.ussshenzhou.ares.client.animation.statemachine.GunStateMachineRuntime;
import cn.ussshenzhou.ares.client.resource.ClientAssetsManager;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/// > 标准武器控制器。主姿态（draw/idle/reload/inspect/holster）由数据驱动的 base 状态机驱动，
/// > movement 作为叠加层，fire 作为独立 additive 槽位。
/// >
/// > 主状态转移、过渡混合、互斥与打断全部下沉到 base 状态机（见 `animations/statemachines/<id>/base.json`），
/// > controller 只负责把玩法事件转换为状态机信号（request / fire pulse），并向 [WeaponAnimationContext]
/// > 同步 gameplay 状态供状态机过渡条件查询。
/// @author TACZ, USS_Shenzhou
public class StandardWeaponController implements WeaponAnimationController, HostOverlayBridge {
    private static final EulerAdditiveBlender ADDITIVE_BLENDER = new SimpleEulerAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);
    private static final InterpolatorBlender INTERPOLATOR_BLENDER = new SimpleInterpolatorBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);

    private final StandardWeaponControlData definition;
    private final AnimationEventSink eventSink;
    private final WeaponAnimationContext context;
    private final Map<String, AnimationLayer> layers = new HashMap<>();
    private final GunStateMachineRuntime baseStateMachine;
    /// > host_overlay：下挂安装且命中 profile 时挂载，叠加在主姿态上。
    private HostOverlayRuntime hostOverlay;

    public StandardWeaponController(StandardWeaponControlData definition,
                                    AnimationEventSink eventSink,
                                    WeaponAnimationContext context) {
        this.definition = definition;
        this.eventSink = eventSink != null ? eventSink : AnimationEventSink.NONE;
        this.context = context;
        loadLayers();
        this.baseStateMachine = createBaseStateMachine();
    }

    @Override
    public void draw() {
        /// > draw 由 base 状态机入口状态在构造时自动播放，这里无需重复触发。
    }

    @Override
    public void putAway() {
        context.requestAction("holster");
    }

    @Override
    public void fire() {
        /// > 只写入真实出膛信号；具体开火动画由内容包的 fire 状态机消费并播放。
        context.pulseFire();
    }

    public void reload(boolean empty) {
        reload(empty, 0, -1);
    }

    public void reload(boolean empty, int gameplayTicks) {
        reload(empty, gameplayTicks, -1);
    }

    /// > @param shellLoopsOverride 脚本编排换弹给定的目标逐发循环数；< 0 表示无覆盖（走数据推导）。
    public void reload(boolean empty, int gameplayTicks, int shellLoopsOverride) {
        String request = empty ? "reload_empty" : "reload_tactical";
        context.setReloadExpectedSeconds(gameplayTicks > 0 ? gameplayTicks / 20.0f : 0.0f);
        context.beginShellReload(shellLoopsOverride);
        context.requestAction(request);
    }

    public void inspect() {
        context.requestAction("inspect");
    }

    public void switchFireMode() {
        context.requestAction("fire_mode_switch");
    }

    /// > 挂载 host overlay（下挂安装且命中 combo profile 时由上层调用）。初始为持枪姿态（无操作差量）。
    public void attachHostOverlay(MountedOverlayResolved resolved) {
        if (resolved == null || context == null || context.display() == null) {
            return;
        }
        hostOverlay = new HostOverlayRuntime(resolved, context.display().getGunModel(), eventSink);
    }

    /// > 卸载 host overlay（下挂卸载或切回主枪模式时调用）。
    public void detachHostOverlay() {
        hostOverlay = null;
    }

    /// > 切换 host overlay 操作差量段（下挂 reload/inspect 时同步驱动主枪手部）。
    public void playHostOverlayOperation(String actionName, float expectedDurationSeconds) {
        if (hostOverlay != null) {
            hostOverlay.playOperation(actionName, expectedDurationSeconds);
        }
    }

    @Override
    public void playHostOverlayOnce(String animation) {
        if (hostOverlay != null) {
            hostOverlay.playOperationAnimation(animation, 0.0f);
        }
    }

    @Override
    public void playHostOverlayScaled(String animation, float expectedDurationSeconds) {
        if (hostOverlay != null) {
            hostOverlay.playOperationAnimation(animation, expectedDurationSeconds);
        }
    }

    @Override
    public void stopHostOverlay(String actionName) {
        clearHostOverlayOperation(actionName);
    }

    /// > 操作差量归零（动作结束回 idle 持枪）。仅当当前 operation 仍是 `actionName` 时才清除，
    /// > 避免多动作竞态下误打断后续动作。`actionName` 为空时无条件清除。
    public void clearHostOverlayOperation(String actionName) {
        if (hostOverlay != null) {
            hostOverlay.clearOperation(actionName);
        }
    }

    /// > 播放模式切换过渡段（若该 combo 配了过渡动画）。返回过渡动画时长（秒），无动画返回 0。
    public float playHostOverlayTransition(boolean toUnderBarrel) {
        return hostOverlay != null ? hostOverlay.playTransition(toUnderBarrel) : 0.0f;
    }

    public boolean hasHostOverlay() {
        return hostOverlay != null;
    }

    @Override
    public void tick(float partialTicks) {
        layers.values().forEach(AnimationLayer::tick);
        if (baseStateMachine != null) {
            baseStateMachine.tick();
        }
        if (hostOverlay != null) {
            /// > 过渡权重由 context 镜像的 operator 过渡进度驱动：切到下挂 0→1，切回主枪 1→0。
            hostOverlay.setTransitionWeight(resolveTransitionWeight());
            hostOverlay.tick();
        }
    }

    /// > 解析当前持枪姿态过渡权重。下挂模式：进度即权重（0→1 淡入）；主枪模式但仍在过渡：反向（1→0 淡出）；
    /// > 非过渡态：下挂满额 1、主枪 0。
    private float resolveTransitionWeight() {
        if (context == null) {
            return 1.0f;
        }
        boolean underBarrel = context.isUnderBarrelMode();
        if (!context.isModeSwitching()) {
            return underBarrel ? 1.0f : 0.0f;
        }
        float progress = context.modeTransitionProgress();
        /// > 切到下挂：0→1；切回主枪：1→0。
        return underBarrel ? progress : (1.0f - progress);
    }

    @Override
    public Pose evaluatePose() {
        Pose pose = baseStateMachine != null ? baseStateMachine.evaluatePose() : DummyPose.INSTANCE;
        /// > 持枪姿态在 movement 之前注入：让走路 bob 能 additive 叠在「已倾斜的持枪基准」上而不被覆盖。
        pose = applyGripOverlay(pose);
        pose = applyContinuousLayers(pose);
        /// > 操作差量在 movement 之后注入：手离开护木去操作下挂的相对位移叠加在最终持枪+走路姿态上。
        pose = applyOperationOverlay(pose);
        /// > 模式切换过渡段差量（若有），在 movement 之后 additive 叠加。
        pose = applyTransitionOverlay(pose);
        return pose;
    }

    /// > 当前 base 状态机激活状态 id，供调试展示。
    public String getBaseStateId() {
        return baseStateMachine != null ? baseStateMachine.currentStateId() : "none";
    }

    /// > fire 层当前状态 id（无 fire 层时返回 none），供调试展示。
    public String getFireSectionName() {
        AnimationLayer fireLayer = layers.get("fire");
        return fireLayer != null ? fireLayer.currentStateId() : "none";
    }

    private void loadLayers() {
        if (definition.layers == null) {
            return;
        }
        for (Map.Entry<String, ControllerLayerBinding> entry : definition.layers.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            layers.put(entry.getKey(), new AnimationLayer(entry.getKey(), entry.getValue(), context,
                    this::resolveAnimation, this::resolveMontage, eventSink));
        }
    }

    private GunStateMachineRuntime createBaseStateMachine() {
        if (definition.base == null) {
            LogUtils.getLogger().warn("[TACZ] Animation controller missing base state machine id");
            return null;
        }
        GunStateMachineGraph baseGraph = ClientAssetsManager.INSTANCE.getAnimationStateMachine(definition.base);
        if (baseGraph == null) {
            LogUtils.getLogger().warn("[TACZ] Animation base state machine missing: {}", definition.base);
            return null;
        }
        return new GunStateMachineRuntime(baseGraph, context, this::resolveAnimation, this::resolveMontage, eventSink);
    }

    private Pose applyContinuousLayers(Pose basePose) {
        Pose pose = basePose;
        for (AnimationLayer layer : layers.values()) {
            Pose layerPose = layer.evaluatePose();
            if (layerPose == DummyPose.INSTANCE) {
                continue;
            }
            if ("additive".equals(layer.blendMode())) {
                pose = ADDITIVE_BLENDER.blend(pose, layerPose);
            } else {
                pose = INTERPOLATOR_BLENDER.blend(pose, layerPose, layer.blendWeight());
            }
        }
        return pose;
    }

    /// > 持枪姿态层注入（movement 之前）。grip 统一走 additive 增量缩放（layered 路线已弃用）：
    /// > 持枪姿态按过渡权重从「零增量」插到满额，weight=0 时不影响 host。
    /// >
    /// > 门控：下挂模式生效；切回主枪的过渡尚未结束时也需继续叠加（权重 1→0 淡出），故 isModeSwitching 时同样生效。
    private Pose applyGripOverlay(Pose basePose) {
        if (hostOverlay == null || context == null) {
            return basePose;
        }
        if (!context.isUnderBarrelMode() && !context.isModeSwitching()) {
            return basePose;
        }
        Pose gripPose = hostOverlay.scaledGripPose();
        if (gripPose == DummyPose.INSTANCE) {
            return basePose;
        }
        return ADDITIVE_BLENDER.blend(basePose, gripPose);
    }

    /// > 操作差量层注入（movement 之后）。additive：手离开护木去操作下挂的相对位移。仅下挂模式生效。
    private Pose applyOperationOverlay(Pose basePose) {
        if (hostOverlay == null || context == null || !context.isUnderBarrelMode()) {
            return basePose;
        }
        Pose operationPose = hostOverlay.operationPose();
        if (operationPose == DummyPose.INSTANCE) {
            return basePose;
        }
        return ADDITIVE_BLENDER.blend(basePose, operationPose);
    }

    /// > 模式切换过渡段差量注入（movement 之后）。additive：过渡专用差量动画。过渡进行中生效（不限模式方向）。
    private Pose applyTransitionOverlay(Pose basePose) {
        if (hostOverlay == null || context == null || !context.isModeSwitching()) {
            return basePose;
        }
        Pose transitionPose = hostOverlay.transitionPose();
        if (transitionPose == DummyPose.INSTANCE) {
            return basePose;
        }
        return ADDITIVE_BLENDER.blend(basePose, transitionPose);
    }

    private BedrockAnimation resolveAnimation(String name) {
        return context != null && context.display() != null ? context.display().getAnimations().get(name) : null;
    }

    /// > 懒加载并缓存指定 montage 的编译结果。缓存挂在 display 上，跨该 display 派生的所有动画实例复用。
    private AnimationMontage<MontageContext> resolveMontage(Identifier montageId) {
        if (context == null || context.display() == null) {
            return null;
        }
        return context.display().getOrCompileMontage(montageId, this::resolveAnimation);
    }

}
