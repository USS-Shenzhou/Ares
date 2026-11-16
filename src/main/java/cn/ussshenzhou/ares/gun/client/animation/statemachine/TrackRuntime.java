package cn.ussshenzhou.ares.gun.client.animation.statemachine;

import com.mojang.logging.LogUtils;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.montage.AnimationMontage;
import cn.ussshenzhou.ares.gun.client.animation.context.AnimationEventSink;
import cn.ussshenzhou.ares.gun.client.animation.context.WeaponAnimationContext;
import cn.ussshenzhou.ares.gun.client.animation.montage.MontageContext;
import cn.ussshenzhou.ares.gun.client.animation.statemachine.source.AnimationRunnerSource;
import cn.ussshenzhou.ares.gun.client.animation.statemachine.source.IProgressControl;
import cn.ussshenzhou.ares.gun.client.animation.statemachine.source.MontageRunnerSource;
import cn.ussshenzhou.ares.gun.client.animation.statemachine.source.PoseSource;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/// > 管理一组命名轨道（track），每个轨道持有一个 {@link PoseSource} 播放后端。
/// > <p>
/// > 这是状态机播放动画的载体：状态机本身只负责通过条件选择输出哪个轨道的姿态，
/// > 而“播放什么动画、如何播放”由动作指令（Molang 绑定方法）声明，runtime 不内置生命周期。
/// > 后端可以是常规 runner（loop/once/manual）或 montage runner（变速 + notify + 多轨）：
/// > <ul>
/// >   <li>loop：循环播放，按真实时间推进。</li>
/// >   <li>once：播放一次后停止。</li>
/// >   <li>manual：相位不随时间推进，由 {@link #setLocomotionPhase} 每 tick 设置；
/// >       所有 manual 轨道在 {@link #tick()} 中统一推进，独立于状态机当前状态，
/// >       避免过渡期间相位冻结导致的跳变。</li>
/// >   <li>montage：由蒙太奇驱动的一次性动作，支持变速对齐 gameplayTicks、notify 时间点、多轨混合。</li>
/// > </ul>
/// @author TACZ, USS_Shenzhou
public class TrackRuntime {
    private static class Track {
        private final PoseSource source;
        /// > 当前播放的动画名（仅常规 runner 后端记录），供 ensureLoop 幂等判断；montage 后端为 null。
        private final String animationName;
        /// > manual 模式下的相位参数（locomotion），仅常规 runner 后端有效。
        private boolean hasLocomotion;
        private float cycleDistance = 2.0f;
        private float scale = 1.0f;

        private Track(PoseSource source) {
            this(source, null);
        }

        private Track(PoseSource source, String animationName) {
            this.source = source;
            this.animationName = animationName;
        }

        private boolean isManual() {
            return source instanceof AnimationRunnerSource runnerSource
                    && runnerSource.mode() == AnimationRunnerSource.Mode.MANUAL;
        }
    }

    private final WeaponAnimationContext weaponContext;
    private final Function<String, BedrockAnimation> animationResolver;
    private final Function<Identifier, AnimationMontage<MontageContext>> montageResolver;
    private final AnimationEventSink eventSink;
    /// > 动画 timeline 指令的执行上下文（与状态机共享）。供 timeline 中 action.notify/play 等在同一上下文求值。
    private final GunMolangContext timelineContext;
    private final Map<String, Track> tracks = new HashMap<>();

    public TrackRuntime(WeaponAnimationContext weaponContext,
                        Function<String, BedrockAnimation> animationResolver,
                        Function<Identifier, AnimationMontage<MontageContext>> montageResolver,
                        AnimationEventSink eventSink,
                        GunMolangContext timelineContext) {
        this.weaponContext = weaponContext;
        this.animationResolver = animationResolver;
        this.montageResolver = montageResolver != null ? montageResolver : id -> null;
        this.eventSink = eventSink != null ? eventSink : AnimationEventSink.NONE;
        this.timelineContext = timelineContext;
    }

    /// > 在指定轨道循环播放动画。
    public void playLoop(String track, String animation) {
        PoseSource source = AnimationRunnerSource.create(resolveAnimation(track, animation), AnimationRunnerSource.Mode.LOOP, eventSink, track, timelineContext);
        if (source != null) {
            tracks.put(track, new Track(source, animation));
        }
    }

    /// > 幂等循环播放：若该轨道已在播放同一动画则跳过（不重置相位）；否则启动循环。
    /// > 供条件性常驻轨道（如空挂静态姿势）在 onUpdate 中每帧调用而不重置动画。
    public void ensureLoop(String track, String animation) {
        Track existing = tracks.get(track);
        if (existing != null && animation != null && animation.equals(existing.animationName)) {
            return;
        }
        playLoop(track, animation);
    }

    /// > 幂等停止：仅当轨道存在时移除（与 {@link #stop} 等价，命名对称供 onUpdate 使用）。
    public void ensureStop(String track) {
        tracks.remove(track);
    }

    /// > 在指定轨道播放一次动画，播完停止。
    public void playOnce(String track, String animation) {
        putSource(track, AnimationRunnerSource.create(resolveAnimation(track, animation), AnimationRunnerSource.Mode.ONCE, eventSink, track, timelineContext));
    }

    /// > 在指定轨道播放手动相位动画。相位由 {@link #setLocomotionPhase} 设置，不随时间推进。
    public void playManual(String track, String animation) {
        putSource(track, AnimationRunnerSource.create(resolveAnimation(track, animation), AnimationRunnerSource.Mode.MANUAL, eventSink, track, timelineContext));
    }

    /// > 在指定轨道播放一次动画并按期望时长变速对齐玩法时长（如 reload）。播完停止。
    /// > <p>
    /// > speed = 动画全长 / expectedDurationSeconds；expectedDurationSeconds&lt;=0 时原速。
    /// > notify 跟随动画 timeline，触发时机随 speed 缩放，与 montage 变速行为一致。
    public void playScaled(String track, String animation, float expectedDurationSeconds) {
        BedrockAnimation bedrockAnimation = resolveAnimation(track, animation);
        float speed = 1.0f;
        if (bedrockAnimation != null && expectedDurationSeconds > 0.0f) {
            float length = bedrockAnimation.getSpecifiedEndTimeS();
            if (length > 0.0f) {
                speed = length / expectedDurationSeconds;
            }
        }
        putSource(track, AnimationRunnerSource.create(bedrockAnimation, AnimationRunnerSource.Mode.ONCE, eventSink, track, timelineContext, speed));
    }

    /// > 在指定轨道播放 montage 驱动的一次性动作。
    /// >
    /// > @param expectedDurationSeconds 期望时长（秒），用于变速对齐 gameplayTicks；&lt;=0 表示原速。
    public void playMontage(String track, Identifier montageId, String section, float expectedDurationSeconds) {
        AnimationMontage<MontageContext> montage = montageResolver.apply(montageId);
        if (montage == null) {
            LogUtils.getLogger().warn("[TACZ] Animation track montage missing: track={}, montage={}", track, montageId);
            return;
        }
        String resolvedSection = section != null && !section.isBlank() ? section : "main";
        MontageRunnerSource source = MontageRunnerSource.create(montage, montageId, eventSink, resolvedSection, expectedDurationSeconds, timelineContext);
        if (source == null) {
            LogUtils.getLogger().warn("[TACZ] Animation track montage section missing: track={}, montage={}, section={}", track, montageId, resolvedSection);
            return;
        }
        tracks.put(track, new Track(source));
    }

    /// > 设置 manual 轨道的 locomotion 相位参数。后续每 tick 据此推进相位。
    /// > 仅对常规 runner 的 manual 后端有效；montage 后端不支持外部强设进度，调用将被忽略并告警。
    public void setLocomotionPhase(String track, float cycleDistance, float scale) {
        Track t = tracks.get(track);
        if (t == null) {
            return;
        }
        if (!t.isManual()) {
            LogUtils.getLogger().warn("[TACZ] Animation set_locomotion_phase on non-manual track ignored: track={}", track);
            return;
        }
        t.hasLocomotion = true;
        t.cycleDistance = cycleDistance;
        t.scale = scale;
        applyLocomotion(t);
    }

    /// > 停止并移除指定轨道。
    public void stop(String track) {
        tracks.remove(track);
    }

    /// > 每 tick 调用：推进各后端时间，并同步所有 manual 轨道的相位。
    /// > manual 相位同步独立于状态机状态，保证过渡期间相位也是实时的。
    /// > <p>
    /// > 用快照遍历：后端 tick 会消费动画 timeline 指令（action.play/stop_track 等），可能在迭代过程中
    /// > 增删轨道，直接遍历 {@code tracks.values()} 会抛 {@link java.util.ConcurrentModificationException}。
    public void tick() {
        for (Track track : tracks.values().toArray(new Track[0])) {
            if (track.isManual()) {
                if (track.hasLocomotion) {
                    applyLocomotion(track);
                }
            } else {
                track.source.tick();
            }
        }
    }

    /// > 计算指定轨道当前的输出姿态；轨道不存在时返回 {@link DummyPose#INSTANCE}。
    /// > <p>
    /// > 传入与状态机共享的 {@link GunMolangContext} 作为求值上下文，使动画内嵌的动态关键帧
    /// > （骨骼变换写成 Molang 表达式，如随 {@code query.anim_time} / 变量实时偏移）能读到运行时量。
    /// > 静态关键帧动画不读 context，开销可忽略；求值频率由上层限频兜底。
    public Pose evaluatePose(String track) {
        Track t = tracks.get(track);
        return t != null ? t.source.evaluate(timelineContext) : DummyPose.INSTANCE;
    }

    /// > 指定轨道的动画是否已播放结束（用于 once / montage 轨道的过渡条件）。
    /// > 轨道不存在视为已结束。
    public boolean isEnd(String track) {
        Track t = tracks.get(track);
        return t == null || t.source.isEnd();
    }

    /// > 指定轨道当前 montage 后端的播放速度；非 montage 轨道返回 1.0。
    public float getPlaybackSpeed(String track) {
        Track t = tracks.get(track);
        if (t != null && t.source instanceof MontageRunnerSource montageSource) {
            return montageSource.getPlaybackSpeed();
        }
        return 1.0f;
    }

    private void applyLocomotion(Track track) {
        IProgressControl progress = track.source.progress();
        if (!progress.isSupported()) {
            return;
        }
        float phase = weaponContext.locomotionProgress(track.cycleDistance, track.scale);
        long duration = progress.getMaxProgress();
        progress.setProgress((long) (phase * duration));
    }

    private void putSource(String track, PoseSource source) {
        if (source == null) {
            return;
        }
        tracks.put(track, new Track(source));
    }

    private BedrockAnimation resolveAnimation(String track, String animation) {
        BedrockAnimation bedrockAnimation = animationResolver.apply(animation);
        if (bedrockAnimation == null) {
            LogUtils.getLogger().warn("[TACZ] Animation track animation missing: track={}, animation={}", track, animation);
        }
        return bedrockAnimation;
    }
}
