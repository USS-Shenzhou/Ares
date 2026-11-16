package cn.ussshenzhou.ares.gun.gameplay.fire;

import cn.ussshenzhou.ares.gun.gameplay.ShootResult;

/// > 蓄力触发：按住蓄力，松手时根据蓄力比例发射。
/// > 弹药检查由外层 canFire 负责，本类只管时序。
/// @author TACZ, USS_Shenzhou
public class ChargeTrigger implements TriggerStrategy {
    private final FireCallback callback;
    private final int maxChargeTicks;
    private final float minRatio;
    private final float minMultiplier;

    private boolean charging = false;
    private int chargeTicks = 0;

    public ChargeTrigger(FireCallback callback,
                         int maxChargeTicks, float minRatio, float minMultiplier) {
        this.callback = callback;
        this.maxChargeTicks = Math.max(maxChargeTicks, 1);
        this.minRatio = minRatio;
        this.minMultiplier = minMultiplier;
    }

    @Override
    public ShootResult onPress(float yaw, float pitch) {
        if (charging) {
            return ShootResult.BLOCKED;
        }
        charging = true;
        chargeTicks = 0;
        return ShootResult.ACCEPTED;
    }

    @Override
    public ShootResult onRelease(float yaw, float pitch) {
        if (!charging) {
            return ShootResult.SUCCESS;
        }
        charging = false;

        float ratio = Math.min((float) chargeTicks / maxChargeTicks, 1.0f);
        chargeTicks = 0;

        if (ratio < minRatio) {
            return ShootResult.BLOCKED;
        }

        float mult = Math.max(ratio, minMultiplier);
        return callback.fire(yaw, pitch, mult, mult, 0);
    }

    @Override
    public void tick() {
        if (charging) {
            chargeTicks++;
        }
    }

    @Override
    public int chargeInput() {
        return chargeTicks;
    }

    @Override
    public void injectChargeInput(int raw) {
        /// > 宽松防呆：只归零负数，不 clamp 上限（信任上报值；上限裁剪由既有 Math.min(ratio,1) 处理）。
        this.chargeTicks = Math.max(raw, 0);
    }

    @Override
    public void reset() {
        charging = false;
        chargeTicks = 0;
    }

    @Override
    public boolean isActive() {
        return charging;
    }
}
