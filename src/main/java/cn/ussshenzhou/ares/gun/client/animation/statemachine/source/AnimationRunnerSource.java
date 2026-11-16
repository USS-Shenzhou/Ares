package cn.ussshenzhou.ares.gun.client.animation.statemachine.source;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.time.AnimationClock;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.time.AnimationClocks;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.TimelineEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData;
import com.maydaymemory.mae.basic.IEvaluationContext;
import com.maydaymemory.mae.basic.Keyframe;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.runner.AnimationContext;
import com.maydaymemory.mae.control.runner.AnimationRunner;
import com.maydaymemory.mae.control.runner.IAnimationContext;
import com.maydaymemory.mae.control.runner.LoopingState;
import com.maydaymemory.mae.control.runner.PlayingState;
import com.maydaymemory.mae.control.runner.StopState;
import cn.ussshenzhou.ares.gun.client.animation.context.AnimationEventSink;
import cn.ussshenzhou.ares.gun.client.animation.statemachine.GunMolangContext;
import net.minecraft.resources.Identifier;

/// > 由常规 [AnimationRunner] 驱动的播放后端。
/// >
/// > 支持三种模式（沿用原 `TrackRuntime` 行为）：
/// > - [Mode#LOOP]：循环播放，按真实时间推进。
/// > - [Mode#ONCE]：播放一次后停止。
/// > - [Mode#MANUAL]：相位不随时间推进，由外部通过 [#progress()] 每 tick 强设。
/// >
/// > 动画文件内嵌的 timeline 关键帧（如 `action.notify('mag_out')`）在 [#tick()] 推进后被消费：
/// > 取出本帧新触发的关键帧并执行其指令。MANUAL 模式不按时间推进、不消费 timeline（locomotion 轨无 timeline）。
/// @author TACZ, USS_Shenzhou
public class AnimationRunnerSource implements PoseSource {
    private static final AnimationClock CLOCK = AnimationClocks.client();

    public enum Mode {
        LOOP,
        ONCE,
        MANUAL
    }

    private final AnimationRunner runner;
    private final Mode mode;
    private final IProgressControl progressControl;
    /// > timeline 指令执行上下文（与状态机共享，含真实 entity + 共享 variable）；为 null 时不消费 timeline。
    private final GunMolangContext timelineContext;
    private final AnimationEventSink timelineSink;
    private final String trackName;

    private AnimationRunnerSource(AnimationRunner runner, Mode mode, AnimationEventSink eventSink,
                                  String trackName, GunMolangContext timelineContext) {
        this.runner = runner;
        this.mode = mode;
        this.progressControl = new RunnerProgressControl(runner.getAnimationContext());
        /// > MANUAL 轨（locomotion）无 timeline 且不按时间推进，无需消费。
        if (eventSink != null && timelineContext != null && mode != Mode.MANUAL) {
            this.timelineContext = timelineContext;
            this.timelineSink = eventSink;
            this.trackName = trackName;
        } else {
            this.timelineContext = null;
            this.timelineSink = null;
            this.trackName = null;
        }
    }

    /// > 创建一个常规 runner 后端。动画缺失时返回 null。
    public static AnimationRunnerSource create(BedrockAnimation animation, Mode mode) {
        return create(animation, mode, null, null, null);
    }

    /// > 创建一个常规 runner 后端，并接入 timeline 事件消费。动画缺失时返回 null。
    /// >
    /// > @param eventSink       timeline 关键帧（notify）转发目标；为 null 时不消费 timeline。
    /// >
    /// > @param trackName       轨道名，用于 notify 事件定位。
    /// >
    /// > @param timelineContext timeline 指令执行上下文（与状态机共享）；为 null 时不消费 timeline。
    public static AnimationRunnerSource create(BedrockAnimation animation, Mode mode,
                                               AnimationEventSink eventSink, String trackName,
                                               GunMolangContext timelineContext) {
        return create(animation, mode, eventSink, trackName, timelineContext, 1.0f);
    }

    /// > 创建一个常规 runner 后端，可指定播放速度（用于 ONCE 模式按期望时长变速对齐）。动画缺失时返回 null。
    /// >
    /// > @param speed 播放速度倍率，仅对 ONCE 模式生效；<=0 视为 1.0 原速。
    public static AnimationRunnerSource create(BedrockAnimation animation, Mode mode,
                                               AnimationEventSink eventSink, String trackName,
                                               GunMolangContext timelineContext, float speed) {
        if (animation == null) {
            return null;
        }
        AnimationRunner runner = new AnimationRunner(animation, new AnimationContext(animation.getSpecifiedEndTimeS()));
        switch (mode) {
            case LOOP -> runner.setState(new LoopingState(CLOCK));
            case ONCE -> {
                runner.getAnimationContext().setProgress(0L);
                PlayingState state = new PlayingState(CLOCK, StopState::new);
                if (speed > 0.0f && speed != 1.0f) {
                    state.setSpeed(speed);
                }
                runner.setState(state);
            }
            case MANUAL ->
                /// > manual 轨道不使用基于时间的状态，相位完全由外部设置。
                    runner.getAnimationContext().setProgress(0L);
        }
        return new AnimationRunnerSource(runner, mode, eventSink, trackName, timelineContext);
    }

    public Mode mode() {
        return mode;
    }

    @Override
    public void tick() {
        /// > manual 轨道相位由外部强设，不按时间推进，也不消费 timeline。
        if (mode != Mode.MANUAL) {
            runner.tick();
            emitTimelineEvents();
            emitSoundEvents();
            emitParticleEvents();
        }
    }

    @Override
    public Pose evaluate(IEvaluationContext ctx) {
        return runner.evaluate(ctx);
    }

    @Override
    public boolean isEnd() {
        /// > loop / manual 永不结束；once 由 context 判定。
        return mode == Mode.ONCE && runner.getAnimationContext().isEnd();
    }

    @Override
    public IProgressControl progress() {
        return progressControl;
    }

    /// > 消费本帧新触发的 timeline 关键帧，执行其指令（如 `action.notify('mag_out')`）。
    /// >
    /// > 执行前把共享 context 的 timeline 定位信息（sink/montageId/track）瞬时切换为本后端的：
    /// > 客户端单线程同步执行、无交错，切换与执行原子完成。
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
                timelineContext.configureTimeline(timelineSink, null, trackName);
                /// > 同步 anim_time 到关键帧时间：clip 出的事件手动执行，需让 query.anim_time 读到正确值
                /// > （否则会读到上一次姿态求值的残留 animTime）。
                timelineContext.prepareEvaluation(keyframe.getTimeS());
                keyframe.getValue().execute(timelineContext);
            }
        }
    }

    private void emitSoundEvents() {
        Iterable<Keyframe<Identifier>> sounds = runner.clip(BedrockAnimation.SOUND_CHANNEL_NAME);
        if (sounds == null) {
            return;
        }
        for (Keyframe<Identifier> keyframe : sounds) {
            if (keyframe != null && keyframe.getValue() != null) {
                timelineSink.onSound(keyframe.getValue());
            }
        }
    }

    /// > 消费本帧新触发的 `particle_effects` 关键帧，转发到事件 sink。
    /// > 与 [#emitSoundEvents()] 同构：仅入队请求，发射器创建由渲染层完成。
    private void emitParticleEvents() {
        if (timelineSink == null) {
            return;
        }
        Iterable<Keyframe<ParticleEffectData>> particles = runner.clip(BedrockAnimation.PARTICLE_CHANNEL_NAME);
        if (particles == null) {
            return;
        }
        for (Keyframe<ParticleEffectData> keyframe : particles) {
            if (keyframe != null && keyframe.getValue() != null) {
                timelineSink.onParticle(keyframe.getValue());
            }
        }
    }

    private static class RunnerProgressControl implements IProgressControl {
        private final IAnimationContext context;

        private RunnerProgressControl(IAnimationContext context) {
            this.context = context;
        }

        @Override
        public boolean isSupported() {
            return true;
        }

        @Override
        public void setProgress(long progress) {
            context.setProgress(progress);
        }

        @Override
        public long getMaxProgress() {
            return context.getMaxProgress();
        }
    }
}
