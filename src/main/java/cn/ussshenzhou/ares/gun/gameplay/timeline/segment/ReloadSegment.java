package cn.ussshenzhou.ares.gun.gameplay.timeline.segment;

import cn.ussshenzhou.ares.gun.capability.GunHolderState;
import cn.ussshenzhou.ares.gun.gameplay.reload.ReloadPolicy;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionGate;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.SegmentToken;
import cn.ussshenzhou.ares.gun.gameplay.timeline.StepSequence;
import cn.ussshenzhou.ares.gun.gameplay.timeline.Timer;
import javax.annotation.Nullable;

/// > 换弹片段：投影 {@link GunHolderState#RELOADING} 并收紧门禁。
/// > <p>
/// > 本片段是<b>纯计时机构，零副作用</b>：
/// > <ul>
/// >   <li>服务端：用 {@link StepSequence} 驱动编排层注入的 Step 任务链（intro/loop/end 或单段弹匣），
/// >       相位时长与副作用全在 Step 里，片段只推进计时与流转。打断（{@link #cancel()}）硬停序列，
/// >       已产生的副作用保留、未到点的不触发。</li>
/// >   <li>客户端：纯 {@link Timer} ms 倒计时（表现），无副作用。</li>
/// > </ul>
/// > 打断策略由 {@link ReloadPolicy}（数据推导）提供，不再依赖有状态的 behavior 实例。
/// @author TACZ, USS_Shenzhou
public class ReloadSegment extends ActionSegment {

    public static final String ID = "reload";
    public static final SegmentToken<ReloadSegment> TOKEN = SegmentToken.of(ID, ReloadSegment.class);

    /// > 服务端：副作用任务链驱动器（客户端为 null）。
    @Nullable
    private final StepSequence sequence;
    /// > 客户端：纯倒计时（服务端为 null）。
    @Nullable
    private final Timer timer;
    private final ReloadPolicy policy;
    /// > 序列 / 倒计时自然结束时触发的完成回调（可为 null）。<b>仅自然结束触发，{@link #cancel()} 打断不触发。</b>
    /// > <ul>
    /// >   <li>服务端：脚本编排路径在链跑完后统一触发 {@code ON_RELOAD_COMPLETE} 钩子与完成事件（响应层接力）。</li>
    /// >   <li>客户端：换弹完成后接续表现编排（如起自愈拉栓屏蔽锁 {@code AutoChamberRequestSegment}，
    /// >       避免服务端补膛尚未同步回来时被 {@code tryAutoChamber} 立即多补一次拉栓）。</li>
    /// > </ul>
    @Nullable
    private final Runnable onComplete;

    private ReloadSegment(@Nullable StepSequence sequence, @Nullable Timer timer, ReloadPolicy policy,
                          @Nullable Runnable onComplete) {
        this.sequence = sequence;
        this.timer = timer;
        this.policy = policy;
        this.onComplete = onComplete;
    }

    /// > 服务端换弹片段：用 StepSequence 驱动注入的 Step 任务链。
    public static ReloadSegment server(StepSequence.Step initialStep, ReloadPolicy policy) {
        return server(initialStep, policy, null);
    }

    /// > 服务端换弹片段（带完成回调）：用 StepSequence 驱动注入的 Step 任务链，序列自然结束时触发
    /// > {@code onComplete}（打断不触发）。脚本编排路径用它接力响应层（完成钩子 / 事件）。
    public static ReloadSegment server(StepSequence.Step initialStep, ReloadPolicy policy,
                                       @Nullable Runnable onComplete) {
        StepSequence sequence = new StepSequence();
        sequence.start(initialStep);
        return new ReloadSegment(sequence, null, policy, onComplete);
    }

    /// > 客户端换弹片段：纯 ms 倒计时表现。
    public static ReloadSegment client(long durationMs, ReloadPolicy policy) {
        return client(durationMs, policy, null);
    }

    /// > 客户端换弹片段（带完成回调）：纯 ms 倒计时表现，倒计时自然结束时触发 {@code onComplete}
    /// > （{@link #cancel()} 打断不触发）。供客户端在换弹完成后接续表现编排（如起自愈拉栓屏蔽锁）。
    public static ReloadSegment client(long durationMs, ReloadPolicy policy, @Nullable Runnable onComplete) {
        Timer timer = new Timer();
        timer.start(Math.max(durationMs, 1));
        return new ReloadSegment(null, timer, policy, onComplete);
    }

    @Override
    public SegmentToken<ReloadSegment> token() {
        return TOKEN;
    }

    @Override
    public boolean tick(long deltaMs) {
        if (sequence != null) {
            if (!sequence.tick(deltaMs)) {
                if (onComplete != null) {
                    onComplete.run();
                }
                finish();
                return false;
            }
            return true;
        }
        if (timer != null && timer.tick(deltaMs)) {
            if (onComplete != null) {
                onComplete.run();
            }
            finish();
            return false;
        }
        return true;
    }

    @Override
    public void constrain(ActionGate.Builder gate) {
        gate.denyShoot().denyReload().denySprint().denyInspect().denySwitchMode();
    }

    @Nullable
    @Override
    public GunHolderState projectedState() {
        return GunHolderState.RELOADING;
    }

    @Override
    public String debugInfo() {
        if (sequence != null) {
            return String.format("server %dms left", sequence.remainingMs());
        }
        return String.format("%dms left", timer != null ? timer.remainingMs() : 0);
    }

    @Override
    public void cancel() {
        super.cancel();
        /// > 硬停：已产生的副作用保留，未到点的 Step 不触发（保持原打断语义）
        if (sequence != null) {
            sequence.cancel();
        }
        if (timer != null) {
            timer.stop();
        }
    }

    public boolean canFireInterrupt() {
        return policy.canFireInterrupt();
    }

    public boolean canSprintInterrupt() {
        return policy.canSprintInterrupt();
    }
}
