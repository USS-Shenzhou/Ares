package cn.ussshenzhou.ares.gun.client.animation.statemachine.source;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.time.AnimationClock;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.time.AnimationClocks;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.TimelineEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData;
import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.IEvaluationContext;
import com.maydaymemory.mae.basic.Keyframe;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.control.montage.AnimationMontage;
import com.maydaymemory.mae.control.montage.AnimationMontageRunner;
import com.maydaymemory.mae.control.montage.AnimationMontageSection;
import cn.ussshenzhou.ares.gun.client.animation.context.AnimationEventSink;
import cn.ussshenzhou.ares.gun.client.animation.montage.MontageContext;
import cn.ussshenzhou.ares.gun.client.animation.statemachine.GunMolangContext;
import net.minecraft.resources.Identifier;

/// > 由 MAE [AnimationMontageRunner] 驱动的播放后端，等价于把原 `MontageSlotRuntime`
/// > 改造成实现 [PoseSource]。
/// >
/// > 蒙太奇的播放进度、section 切换、轨道分层混合、notify/clip 触发全部交给 MAE，本类负责：
/// > 按预期时长计算播放速度、把 sound clip 转发到事件 sink、缓存末帧姿态。
/// >
/// > **末帧缓存**：非循环 montage 自然播放结束的那一帧，runner 进度精确停在 section 末端，
/// > 轨道按开区间判定会返回 null（即 [DummyPose]）。状态机在「源状态恰好播完」时会缓存
/// > 转出瞬间的 fromPose 做插值，若此时取到 DummyPose 会产生跳变。因此每帧求值时缓存最后一个
/// > 有效姿态，结束后 [#evaluate] 返回该缓存帧，使过渡插值起点稳定。
/// @author TACZ, USS_Shenzhou
public class MontageRunnerSource implements PoseSource {
    private static final AnimationClock CLOCK = AnimationClocks.client();
    private static final ZYXBoneTransformFactory BONE_TRANSFORM_FACTORY = new ZYXBoneTransformFactory();

    private final AnimationMontage<MontageContext> montage;
    private final MontageContext context;
    private final AnimationMontageRunner<MontageContext> runner;
    /// > timeline 指令执行上下文（与状态机共享，含真实 entity + 共享 variable）；为 null 时仅消费 notify 不可用。
    private final GunMolangContext timelineContext;
    private float playbackSpeed = 1.0f;
    private Pose lastPose = DummyPose.INSTANCE;

    private MontageRunnerSource(AnimationMontage<MontageContext> montage, MontageContext context,
                                GunMolangContext timelineContext) {
        this.montage = montage;
        this.context = context;
        this.runner = new AnimationMontageRunner<>(montage, context, BONE_TRANSFORM_FACTORY, ArrayPoseBuilder::new, CLOCK::nowNanos);
        this.timelineContext = timelineContext;
    }

    /// > 创建并立即播放指定 section 的 montage 后端。
    /// >
    /// > @param expectedDurationSeconds 期望播放时长（秒），用于变速对齐 gameplayTicks；<=0 表示原速。\
    /// > @param timelineContext         timeline 指令执行上下文（与状态机共享）；为 null 时不消费 timeline。\
    /// > @return 创建成功的后端；montage 为空或 section 不存在时返回 null。
    public static MontageRunnerSource create(AnimationMontage<MontageContext> montage, Identifier montageId,
                                             AnimationEventSink eventSink,
                                             String section, float expectedDurationSeconds,
                                             GunMolangContext timelineContext) {
        if (montage == null) {
            return null;
        }
        MontageRunnerSource source = new MontageRunnerSource(montage, new MontageContext(montageId, eventSink), timelineContext);
        return source.playSection(section, expectedDurationSeconds) ? source : null;
    }

    private boolean playSection(String sectionName, float expectedDurationSeconds) {
        AnimationMontageSection section = montage.getSection(sectionName);
        if (section == null) {
            return false;
        }
        playbackSpeed = calculatePlaybackSpeed(section, expectedDurationSeconds);
        runner.setSpeed(playbackSpeed);
        /// > start 内部会 setSection 并把进度重置到 section 起点；先 stop 解除 isPlaying 守卫以允许重新开始。
        runner.stop();
        runner.start(sectionName);
        return true;
    }

    @Override
    public void tick() {
        runner.tick();
        emitSoundEvents();
        emitParticleEvents();
        emitTimelineEvents();
    }

    @Override
    public Pose evaluate(IEvaluationContext ctx) {
        /// > 已停止的非循环 montage 不应再产出实时姿态（runner 末端开区间判定可能返回末帧或 null）。
        /// > 结束后返回最后一个有效姿态缓存，供状态机过渡插值起点使用，避免取到 DummyPose 跳变。
        if (!runner.isPlaying()) {
            return lastPose;
        }
        Pose pose = runner.getPose();
        if (pose != DummyPose.INSTANCE) {
            lastPose = pose;
        }
        return pose;
    }

    @Override
    public boolean isEnd() {
        return !runner.isPlaying();
    }

    @Override
    public IProgressControl progress() {
        /// > montage 后端进度由 section 内部管理，不支持外部强设。
        return IProgressControl.NONE;
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
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

    /// > 消费本帧新触发的动画内嵌 timeline 关键帧，执行其指令（如 `action.notify('mag_out')`）。
    /// >
    /// > 注意：与 montage 自身的 notifyTracks 是两套来源——notifyTracks 由 MAE runner 在 `tick()`
    /// > 内部按 section 触发，timeline 则跟随动画文件，由本方法 clip 出来执行。
    /// >
    /// > 执行前把共享 context 的 timeline 定位信息切换为本 montage 的：客户端单线程同步执行、无交错。
    private void emitTimelineEvents() {
        if (timelineContext == null) {
            return;
        }
        Iterable<Keyframe<TimelineEvent>> events = runner.clip(BedrockAnimation.TIMELINE_CHANNEL_NAME);
        if (events == null) {
            return;
        }
        for (Keyframe<TimelineEvent> keyframe : events) {
            if (keyframe != null && keyframe.getValue() != null) {
                timelineContext.configureTimeline(context.eventSink(), context.montageId(), null);
                /// > 同步 anim_time 到关键帧时间：clip 出的事件手动执行，需让 query.anim_time 读到正确值
                /// > （否则会读到上一次姿态求值的残留 animTime）。
                timelineContext.prepareEvaluation(keyframe.getTimeS());
                keyframe.getValue().execute(timelineContext);
            }
        }
    }
}
