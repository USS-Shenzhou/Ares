package cn.ussshenzhou.ares.gun.client.animation.montage;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.time.AnimationClock;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.time.AnimationClocks;
import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Keyframe;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.control.montage.AnimationMontage;
import com.maydaymemory.mae.control.montage.AnimationMontageRunner;
import com.maydaymemory.mae.control.montage.AnimationMontageSection;
import net.minecraft.resources.Identifier;

/// > 单个动画槽的运行时，内部由 MAE 的 [AnimationMontageRunner] 驱动。
/// >
/// > 蒙太奇的播放进度、section 切换、轨道分层混合、notify/clip 触发全部交给 MAE，
/// > 本类只负责：按预期时长计算播放速度、把 sound clip 转发到事件 sink、向 controller 暴露查询接口。
/// @author TACZ, USS_Shenzhou
public class MontageSlotRuntime {
    private static final AnimationClock CLOCK = AnimationClocks.client();
    private static final ZYXBoneTransformFactory BONE_TRANSFORM_FACTORY = new ZYXBoneTransformFactory();

    private final AnimationMontage<MontageContext> montage;
    private final MontageContext context;
    private final AnimationMontageRunner<MontageContext> runner;
    private String activeSectionName = "none";
    private float playbackSpeed = 1.0f;

    public MontageSlotRuntime(AnimationMontage<MontageContext> montage, MontageContext context) {
        this.montage = montage;
        this.context = context;
        this.runner = new AnimationMontageRunner<>(montage, context, BONE_TRANSFORM_FACTORY, ArrayPoseBuilder::new, CLOCK::nowNanos);
    }

    public boolean playSection(String sectionName) {
        return playSection(sectionName, 0.0f);
    }

    public boolean playSection(String sectionName, float expectedDurationSeconds) {
        AnimationMontageSection section = montage.getSection(sectionName);
        if (section == null) {
            return false;
        }
        playbackSpeed = calculatePlaybackSpeed(section, expectedDurationSeconds);
        runner.setSpeed(playbackSpeed);
        /// > start 内部会 setSection 并把进度重置到 section 起点；先 stop 解除 isPlaying 守卫以允许重新开始。
        runner.stop();
        runner.start(sectionName);
        activeSectionName = sectionName;
        return true;
    }

    public void tick() {
        runner.tick();
        emitSoundEvents();
        emitParticleEvents();
    }

    public Pose evaluatePose() {
        /// > 已停止的非循环 montage（如 action 播放结束）不应再产出姿态：
        /// > runner 停止时 progress 会被 clamp 在 section 末端，但 float<->纳秒往返转换的精度抖动
        /// > 可能使轨道按开区间判定恰好返回末帧姿态（而非 null），导致结束后仍输出冻结的末帧，
        /// > 进而在 controller 中永久覆盖 movement 等混合层。此处显式以 isPlaying 为准。
        if (!runner.isPlaying()) {
            return DummyPose.INSTANCE;
        }
        /// > MAE runner 以当前 progress 求值；basePose 用 DummyPose，叠加层由 controller 负责。
        return runner.getPose();
    }

    public boolean isPlaying() {
        return runner.isPlaying();
    }

    public void stop() {
        runner.stop();
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public String getActiveSectionName() {
        return runner.isPlaying() ? activeSectionName : "none";
    }

    private float calculatePlaybackSpeed(AnimationMontageSection section, float expectedDurationSeconds) {
        if (expectedDurationSeconds <= 0.0f) {
            return 1.0f;
        }
        float sectionDuration = section.getLength();
        if (sectionDuration <= 0.0f) {
            return 1.0f;
        }
        return sectionDuration / expectedDurationSeconds;
    }

    private void emitSoundEvents() {
        Iterable<Keyframe<Identifier>> sounds = runner.clip(BedrockAnimation.SOUND_CHANNEL_NAME);
        if (sounds == null) {
            return;
        }
        for (Keyframe<Identifier> keyframe : sounds) {
            if (keyframe != null && keyframe.getValue() != null) {
                context.eventSink().onSound(keyframe.getValue());
            }
        }
    }

    /// > 消费本帧新触发的 `particle_effects` 关键帧，转发到事件 sink。
    /// > 与 [#emitSoundEvents()] 同构：仅入队请求，发射器创建由渲染层完成。
    private void emitParticleEvents() {
        Iterable<Keyframe<ParticleEffectData>> particles = runner.clip(BedrockAnimation.PARTICLE_CHANNEL_NAME);
        if (particles == null) {
            return;
        }
        for (Keyframe<ParticleEffectData> keyframe : particles) {
            if (keyframe != null && keyframe.getValue() != null) {
                context.eventSink().onParticle(keyframe.getValue());
            }
        }
    }
}
