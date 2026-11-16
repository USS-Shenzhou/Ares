package cn.ussshenzhou.ares.gun.client.animation.statemachine;

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
import cn.ussshenzhou.ares.gun.client.animation.context.AnimationEventSink;
import cn.ussshenzhou.ares.gun.client.animation.context.WeaponAnimationContext;
import cn.ussshenzhou.ares.gun.client.animation.montage.MontageContext;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Function;

/// > 状态机求值上下文。持有 {@link TrackRuntime} 与武器动画上下文。
/// > <p>
/// > 状态/过渡的动作（onEnter/onUpdate/onExit/afterTrigger）通过 {@link GunMolangContext}
/// > 暴露的 Molang 绑定方法操作此处的轨道，runtime 不内置任何播放生命周期。
/// @author TACZ, USS_Shenzhou
public class GunStateContext {
    private static final InterpolatorBlender INTERPOLATOR_BLENDER = new SimpleInterpolatorBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);
    /// > 多轨道合成：按 additive 叠加。状态机输出的 pose 本身即相对 bind 的增量，
    /// > 叠加层（如快慢机拨杆 selector、局部指示）以增量相加，符合动画师制作叠加动画的心智模型。
    /// > 骨骼不相交时与覆盖等价；相交时为两份增量相加，而非整根骨骼覆盖。
    private static final EulerAdditiveBlender ADDITIVE_BLENDER = new SimpleEulerAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);

    private final WeaponAnimationContext weaponContext;
    private final TrackRuntime trackRuntime;
    private final GunMolangContext molangContext = new GunMolangContext(this);

    public GunStateContext(WeaponAnimationContext weaponContext,
                           Function<String, BedrockAnimation> animationResolver,
                           Function<Identifier, AnimationMontage<MontageContext>> montageResolver,
                           AnimationEventSink eventSink) {
        this.weaponContext = weaponContext;
        /// > 把本上下文的共享 molang context 传给 TrackRuntime，供动画 timeline 指令（action.notify/play/...）
        /// > 在与状态机同一个上下文中求值：既能读 gun.* 查询、共享 variable，也能驱动同一组轨道。
        this.trackRuntime = new TrackRuntime(weaponContext, animationResolver, montageResolver, eventSink, molangContext);
    }

    public WeaponAnimationContext weaponContext() {
        return weaponContext;
    }

    public TrackRuntime tracks() {
        return trackRuntime;
    }

    public GunMolangContext molangContext() {
        return molangContext;
    }

    /// > 每 tick 推进轨道（loop/once 时间推进 + manual 相位同步）。
    public void tick() {
        trackRuntime.tick();
    }

    public Pose evaluateTrackPose(String track) {
        return trackRuntime.evaluatePose(track);
    }

    /// > 合成多个轨道的输出姿态：以第一个轨道为基底，后续轨道按 additive 依次叠加。
    /// > 用于让只控制局部骨骼的叠加轨道（如快慢机拨杆 selector）叠在主姿态上。
    /// > 各轨道 pose 均为相对 bind 的增量，叠加即增量相加；骨骼不相交时等价于覆盖。
    public Pose evaluateTrackPoses(List<String> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return DummyPose.INSTANCE;
        }
        Pose result = null;
        for (String track : tracks) {
            Pose pose = trackRuntime.evaluatePose(track);
            if (pose == DummyPose.INSTANCE) {
                continue;
            }
            result = result == null ? pose : ADDITIVE_BLENDER.blend(result, pose);
        }
        return result != null ? result : DummyPose.INSTANCE;
    }

    public Pose blend(Pose fromPose, Pose toPose, float alpha) {
        return INTERPOLATOR_BLENDER.blend(fromPose, toPose, alpha);
    }

    /// > 以 additive 方式把 {@code add} 增量叠加到 {@code base} 上。供脚本状态机做条件混合等
    /// > Molang 表达不了的高级合成。任一为 {@link DummyPose#INSTANCE} 时返回另一个。
    public Pose additive(Pose base, Pose add) {
        if (base == DummyPose.INSTANCE) {
            return add;
        }
        if (add == DummyPose.INSTANCE) {
            return base;
        }
        return ADDITIVE_BLENDER.blend(base, add);
    }
}
