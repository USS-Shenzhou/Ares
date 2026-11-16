package cn.ussshenzhou.ares.gun.gameplay.timeline.segment;

import cn.ussshenzhou.ares.gun.gameplay.ShootResult;
import cn.ussshenzhou.ares.gun.gameplay.fire.TriggerStrategy;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.SegmentToken;
import cn.ussshenzhou.ares.gun.gameplay.timeline.Timer;

/// > Burst 后续发编排片段：客户端按间隔自驱后续发，服务端保留窗口并由 C2S 逐发驱动。
/// @author TACZ, USS_Shenzhou
public class BurstSequenceSegment extends ActionSegment {

    public static final String ID = "burst_sequence";
    public static final SegmentToken<BurstSequenceSegment> TOKEN = SegmentToken.of(ID, BurstSequenceSegment.class);

    private final long burstIntervalMs;
    private final long tailCooldownMs;
    private final boolean clientDriven;
    private final TriggerStrategy.FireCallback followUpFire;
    private final Runnable afterSequence;

    /// > 后续发节奏 / 尾随闭锁计时（先计后续发间隔，打完后复用为尾随冷却）。
    private final Timer timer = new Timer();

    private int remaining;
    private boolean afterSequenceStarted = false;

    /// > @param burstInterval  后续发间隔（tick，由编排层从 FireModeEntry 推导）
    /// >
    /// > @param tailCooldownMs 全部打完后的尾随闭锁窗口（毫秒，由编排层传入）
    public BurstSequenceSegment(int followUpCount, int burstInterval, long tailCooldownMs, boolean clientDriven,
                                TriggerStrategy.FireCallback followUpFire, Runnable afterSequence) {
        this.remaining = Math.max(followUpCount, 0);
        this.burstIntervalMs = Math.max(burstInterval, 1) * 50L;
        this.tailCooldownMs = Math.max(tailCooldownMs, 0);
        this.clientDriven = clientDriven;
        this.followUpFire = followUpFire;
        this.afterSequence = afterSequence;
        if (clientDriven) {
            this.timer.start(burstIntervalMs);
        }
    }

    @Override
    public SegmentToken<BurstSequenceSegment> token() {
        return TOKEN;
    }

    @Override
    public boolean tick(long deltaMs) {
        if (remaining <= 0) {
            /// > 尾随闭锁窗口：到点结束整条片段
            if (timer.tick(deltaMs) || !timer.isRunning()) {
                finish();
            }
            return isActive();
        }
        if (!clientDriven) {
            return true;
        }
        if (!timer.tick(deltaMs)) {
            return true;
        }
        ShootResult result = followUpFire.fire(0, 0, 1.0f, 1.0f, 0);
        if (result == ShootResult.SUCCESS) {
            CooldownSegment.driveShotFired(segments());
        }
        /// > 旧客户端 burst 自驱语义：到点即消耗一次后续发额度，哪怕本地预测发现无弹。
        remaining--;
        if (remaining <= 0) {
            beginAfterSequence();
            timer.start(tailCooldownMs);
        } else {
            timer.start(burstIntervalMs);
        }
        return result == ShootResult.SUCCESS || isActive();
    }

    /// > 服务端 C2S 驱动的 burst 后续发入口。
    public ShootResult fireServerFollowUp(float yaw, float pitch) {
        if (remaining <= 0 || clientDriven) {
            return ShootResult.BLOCKED;
        }
        ShootResult result = followUpFire.fire(yaw, pitch, 1.0f, 1.0f, 0);
        if (result == ShootResult.SUCCESS) {
            CooldownSegment.driveShotFired(segments());
            remaining--;
            if (remaining <= 0) {
                beginAfterSequence();
                timer.start(tailCooldownMs);
            }
        }
        return result;
    }

    public boolean isBursting() {
        return remaining > 0;
    }

    public boolean blocksFire() {
        return remaining > 0 || timer.isRunning();
    }

    private void beginAfterSequence() {
        if (afterSequenceStarted) {
            return;
        }
        afterSequenceStarted = true;
        afterSequence.run();
    }

    @Override
    public String debugInfo() {
        if (remaining > 0) {
            return "burst remaining=" + remaining + " next=" + timer.remainingMs() + "ms";
        }
        return "cooldown " + timer.remainingMs() + "ms";
    }

    @Override
    public void cancel() {
        super.cancel();
        timer.stop();
    }
}
