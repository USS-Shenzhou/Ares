package cn.ussshenzhou.ares.gun.gameplay.fire;

import cn.ussshenzhou.ares.gun.gameplay.ShootResult;

/// > 延迟触发：按下扳机后开始倒计时，到达后发射。松手取消。
/// > 弹药检查由外层 canFire 负责，本类只管时序。
/// @author TACZ, USS_Shenzhou
public class DelayedTrigger implements TriggerStrategy {
    private final FireCallback callback;
    private final int delayTicks;

    private boolean held = false;
    private int remaining = 0;
    private float savedYaw, savedPitch;

    public DelayedTrigger(FireCallback callback, int delayTicks) {
        this.callback = callback;
        this.delayTicks = Math.max(delayTicks, 1);
    }

    @Override
    public ShootResult onPress(float yaw, float pitch) {
        if (held) {
            return ShootResult.BLOCKED;
        }
        held = true;
        remaining = delayTicks;
        savedYaw = yaw;
        savedPitch = pitch;
        return ShootResult.ACCEPTED;
    }

    @Override
    public ShootResult onRelease(float yaw, float pitch) {
        if (held && remaining > 0) {
            held = false;
            remaining = 0;
        }
        return ShootResult.SUCCESS;
    }

    @Override
    public void tick() {
        if (!held || remaining <= 0) {
            return;
        }
        remaining--;
        if (remaining <= 0) {
            callback.fire(savedYaw, savedPitch, 1.0f, 1.0f, 0);
            held = false;
        }
    }

    @Override
    public void reset() {
        held = false;
        remaining = 0;
    }

    @Override
    public boolean isActive() {
        return held;
    }
}
