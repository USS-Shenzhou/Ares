package cn.ussshenzhou.ares.client.animation.mounted;

import com.mojang.logging.LogUtils;
import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.blend.InterpolatorBlender;
import com.maydaymemory.mae.blend.LayerBlend;
import com.maydaymemory.mae.blend.SimpleInterpolatorBlender;
import com.maydaymemory.mae.blend.SkeletonBaseLayerBlend;
import com.maydaymemory.mae.blend.SkeletonDescendantAccessorAdapter;
import cn.ussshenzhou.ares.client.animation.context.AnimationEventSink;
import cn.ussshenzhou.ares.client.animation.montage.MontageContext;
import cn.ussshenzhou.ares.client.animation.montage.MontageSlotRuntime;
import cn.ussshenzhou.ares.client.animation.statemachine.source.AnimationRunnerSource;
import cn.ussshenzhou.ares.client.animation.statemachine.source.IProgressControl;
import cn.ussshenzhou.ares.client.animation.statemachine.source.PoseSource;
import cn.ussshenzhou.ares.client.model.BedrockGunModel;

import java.util.ArrayList;
import java.util.List;

/// > host overlay 运行时：把下挂相关的姿态修正注入到 host 主姿态上，分两层（性质不同，注入点也不同）：
/// > - **grip 持枪姿态层**：per-combo 的绝对姿态（如手握在下挂上、整枪倾斜）。用 [SkeletonBaseLayerBlend]
/// >   per-hand 权重 **layered** 混合，由 controller 在 movement 之前注入——这样 movement 的走路 bob 能 additive
/// >   叠在「已倾斜的持枪基准」上而不被覆盖。
/// > - **operation 操作差量层**：per-mounted 的相对增量动画（如换弹时手离开护木去操作下挂再回来）。
/// >   **additive** 混合，由 controller 在 movement 之后注入。idle 时无操作段，增量为 0。
/// >
/// > 两层动画均在资源重载阶段绑定到具体 host 模型骨架（见 [MountedOverlayResolved]），运行时直接使用。
/// @author TACZ, USS_Shenzhou
public class HostOverlayRuntime {
    /// > 用于把绝对持枪姿态按过渡权重从「零增量」插值到「满额」（additive 强度缩放）。
    private static final InterpolatorBlender TRANSITION_BLENDER =
            new SimpleInterpolatorBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);

    private final MountedOverlayResolved resolved;
    private final BedrockGunModel hostModel;
    private final AnimationEventSink eventSink;
    private final SkeletonDescendantAccessorAdapter skeletonAccessor;
    private final OverlayWeightContext weightContext = new OverlayWeightContext();

    /// > grip 持枪姿态层（静态绝对姿态，取动画 t=0 求值；layered 混合）。
    private final Pose gripPose;
    private SkeletonBaseLayerBlend gripLayerBlend;
    private final List<OverlayWeightProvider> gripWeightProviders = new ArrayList<>();

    /// > operation 操作差量层（由下挂状态机触发，普通 runner 优先，旧 montage 兼容）。
    private PoseSource operationSource;
    private String operationActionName = "none";

    /// > 模式切换过渡：持枪姿态整体淡入淡出权重（0=纯 host，1=满额持枪），由 controller 每 tick 从 context 镜像驱动。
    /// > 切到下挂 0→1，切回 1→0；无过渡动画时仅靠此权重做时间淡入淡出。
    private float transitionWeight = 1.0f;
    /// > 可选的过渡段动画（绑定 host 模型，相对持枪姿态的差量），additive 叠加，播放期间随 transitionWeight 推进。
    private BedrockAnimation transitionAnimation;
    private float transitionTimeS = 0.0f;
    private float transitionDurationS = 0.0f;
    private boolean transitionPlaying = false;

    public HostOverlayRuntime(MountedOverlayResolved resolved, BedrockGunModel hostModel, AnimationEventSink eventSink) {
        this.resolved = resolved;
        this.hostModel = hostModel;
        this.eventSink = eventSink != null ? eventSink : AnimationEventSink.NONE;
        this.skeletonAccessor = hostModel != null ? new SkeletonDescendantAccessorAdapter(hostModel.getInstance()) : null;
        /// > 持枪姿态是绝对静态姿态，取动画起点求值一次即可（退化为单帧 idle 也成立）。
        this.gripPose = resolved != null && resolved.gripAnimation != null
                ? resolved.gripAnimation.evaluate(0.0f)
                : DummyPose.INSTANCE;
        buildGripLayerBlend();
    }

    /// > 兼容旧 action name -> montage section 的操作方式。
    public void playOperation(String actionName, float expectedDurationSeconds) {
        if (resolved == null || resolved.operationMontage == null || actionName == null) {
            return;
        }
        if (resolved.operationMontage.getSection(actionName) == null) {
            return;
        }
        MontageSlotRuntime slot = new MontageSlotRuntime(resolved.operationMontage, new MontageContext(null, eventSink));
        slot.playSection(actionName, expectedDurationSeconds);
        operationSource = new MontageOperationSource(slot);
        operationActionName = actionName;
    }

    public void playOperationAnimation(String animation, float expectedDurationSeconds) {
        if (resolved == null || animation == null || animation.isBlank()) {
            return;
        }
        BedrockAnimation bedrockAnimation = resolved.operationAnimations.get(animation);
        if (bedrockAnimation == null) {
            LogUtils.getLogger().warn("[TACZ] host overlay operation animation missing: {}", animation);
            return;
        }
        float speed = 1.0f;
        if (expectedDurationSeconds > 0.0f) {
            float length = bedrockAnimation.getSpecifiedEndTimeS();
            if (length > 0.0f) {
                speed = length / expectedDurationSeconds;
            }
        }
        operationSource = AnimationRunnerSource.create(bedrockAnimation, AnimationRunnerSource.Mode.ONCE, eventSink, "host_overlay", null, speed);
        operationActionName = animation;
    }

    /// > 操作差量归零（动作结束回 idle 持枪）。
    public void clearOperation() {
        operationSource = null;
        operationActionName = "none";
    }

    /// > 条件归零：仅当当前 operation 仍是 `actionName` 指定的那个动作时才清除。
    /// >
    /// > 用于解决多动作竞态：fire 触发的 operation 在 fire 退出时调本方法停止，但若期间
    /// > reload/inspect 已抢先切换了 operation，则当前 action 名不匹配，本次停止被忽略，
    /// > 避免误打断后续动作（下挂仍在播放对应动画时主枪 overlay 不应被清掉）。
    /// >
    /// > `actionName` 为 null/空时退化为无条件清除（向后兼容）。
    public void clearOperation(String actionName) {
        if (actionName == null || actionName.isBlank()) {
            clearOperation();
            return;
        }
        if (actionName.equals(operationActionName)) {
            clearOperation();
        }
    }

    public void tick() {
        if (operationSource != null) {
            operationSource.tick();
        }
        if (transitionPlaying) {
            /// > 过渡段按 tick 推进（每 tick 1/20s），到时长结束。
            transitionTimeS += 1.0f / 20.0f;
            if (transitionDurationS > 0 && transitionTimeS >= transitionDurationS) {
                transitionPlaying = false;
            }
        }
        updateGripWeights();
    }

    /// > 设置持枪姿态整体过渡权重（0~1），由 controller 每 tick 从 context 镜像驱动。
    public void setTransitionWeight(float weight) {
        this.transitionWeight = Math.clamp(weight, 0.0f, 1.0f);
    }

    /// > 开始播放过渡段差量动画（若该 combo 配了过渡动画）。无动画时仅靠 transitionWeight 做淡入淡出。
    /// >
    /// > @param toUnderBarrel true=切到下挂方向，false=切回主枪方向
    /// >
    /// > @return 过渡动画时长（秒），无动画返回 0
    public float playTransition(boolean toUnderBarrel) {
        BedrockAnimation animation = resolved != null
                ? (toUnderBarrel ? resolved.transitionToUnderBarrel : resolved.transitionToPrimary)
                : null;
        if (animation == null) {
            transitionAnimation = null;
            transitionPlaying = false;
            transitionDurationS = 0.0f;
            return 0.0f;
        }
        transitionAnimation = animation;
        transitionTimeS = 0.0f;
        transitionDurationS = animation.getEndTimeS();
        transitionPlaying = true;
        return transitionDurationS;
    }

    /// > 过渡段差量姿态（additive），无过渡段或已结束时返回 DummyPose。
    public Pose transitionPose() {
        if (!transitionPlaying || transitionAnimation == null) {
            return DummyPose.INSTANCE;
        }
        return transitionAnimation.evaluate(transitionTimeS);
    }

    /// > 持枪姿态（绝对），按过渡权重缩放为 additive 增量。weight=0 时退化为零增量（DummyPose），
    /// > weight=1 时为满额持枪姿态。layered 路线已弃用，grip 统一走 additive，因此这里直接做增量缩放。
    public Pose gripPose() {
        return gripPose;
    }

    /// > 按过渡权重缩放后的持枪姿态增量（additive 用）。weight 越小越接近零增量。
    public Pose scaledGripPose() {
        if (gripPose == DummyPose.INSTANCE) {
            return DummyPose.INSTANCE;
        }
        if (transitionWeight >= 1.0f) {
            return gripPose;
        }
        if (transitionWeight <= 0.0f) {
            return DummyPose.INSTANCE;
        }
        /// > 从「零增量(DummyPose)」按 weight 插到满额 gripPose：未覆盖骨骼不出现在输出，不影响 host。
        return TRANSITION_BLENDER.blend(DummyPose.INSTANCE, gripPose, transitionWeight);
    }

    /// > 持枪姿态层混合模式。
    public String gripBlendMode() {
        return resolved != null ? resolved.gripBlendMode : "layered";
    }

    /// > 持枪姿态 per-hand 权重层。layered 模式且分组有效时非 null。
    public LayerBlend gripLayerBlend() {
        return gripLayerBlend;
    }

    /// > 操作差量姿态（additive）。无操作段时返回 DummyPose。
    public Pose operationPose() {
        return operationSource != null ? operationSource.evaluate(null) : DummyPose.INSTANCE;
    }

    public String operationActionName() {
        return operationActionName;
    }

    public boolean isOperationPlaying() {
        return operationSource != null && !operationSource.isEnd();
    }

    /// > 按模板的 grip 分组构建持枪姿态层控制点。非 layered 或缺骨架/分组时不构建（gripLayerBlend 置 null）。
    private void buildGripLayerBlend() {
        gripLayerBlend = null;
        gripWeightProviders.clear();
        if (skeletonAccessor == null || hostModel == null || resolved == null) {
            return;
        }
        if (!"layered".equals(resolved.gripBlendMode) || resolved.gripGroups == null || resolved.gripGroups.isEmpty()) {
            return;
        }
        SkeletonBaseLayerBlend blend = new SkeletonBaseLayerBlend(skeletonAccessor);
        boolean any = false;
        for (MountedOverlayTemplate.GripGroup group : resolved.gripGroups) {
            if (group == null || group.rootBone == null) {
                continue;
            }
            int boneIndex = hostModel.getIndex(group.rootBone);
            if (boneIndex < 0) {
                LogUtils.getLogger().warn("[TACZ] host overlay grip group root_bone not found in host model: {}", group.rootBone);
                continue;
            }
            int depth = Math.max(1, group.depth);
            try {
                blend.addControlPoint(boneIndex, depth, 0.0f);
            } catch (IllegalArgumentException e) {
                LogUtils.getLogger().warn("[TACZ] host overlay grip group control point failed: root_bone={}, {}", group.rootBone, e.getMessage());
                continue;
            }
            gripWeightProviders.add(OverlayWeightProvider.from(group.weight));
            any = true;
        }
        if (any) {
            gripLayerBlend = blend;
            updateGripWeights();
        }
    }

    private void updateGripWeights() {
        if (gripLayerBlend == null) {
            return;
        }
        for (int i = 0; i < gripWeightProviders.size(); i++) {
            float weight = gripWeightProviders.get(i).getWeight(weightContext);
            gripLayerBlend.setControlPointWeight(i, Math.clamp(weight, 0.0f, 1.0f));
        }
    }

    private static class MontageOperationSource implements PoseSource {
        private final MontageSlotRuntime slot;

        private MontageOperationSource(MontageSlotRuntime slot) {
            this.slot = slot;
        }

        @Override
        public void tick() {
            slot.tick();
        }

        @Override
        public Pose evaluate(com.maydaymemory.mae.basic.IEvaluationContext ctx) {
            return slot.evaluatePose();
        }

        @Override
        public boolean isEnd() {
            return !slot.isPlaying();
        }

        @Override
        public IProgressControl progress() {
            return IProgressControl.NONE;
        }
    }
}
