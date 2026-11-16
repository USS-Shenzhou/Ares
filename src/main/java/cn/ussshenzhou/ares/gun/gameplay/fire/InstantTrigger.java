package cn.ussshenzhou.ares.gun.gameplay.fire;

import cn.ussshenzhou.ares.gun.gameplay.ShootResult;

/// > 即时触发：按下扳机立即发射。
/// @author TACZ, USS_Shenzhou
public class InstantTrigger implements TriggerStrategy {
    private final FireCallback callback;

    public InstantTrigger(FireCallback callback) {
        this.callback = callback;
    }

    @Override
    public ShootResult onPress(float yaw, float pitch) {
        return callback.fire(yaw, pitch, 1.0f, 1.0f, 0);
    }

    @Override
    public ShootResult onRelease(float yaw, float pitch) {
        return ShootResult.SUCCESS;
    }

    @Override
    public void tick() {}

    @Override
    public void reset() {}

    @Override
    public boolean isActive() {
        return false;
    }
}
