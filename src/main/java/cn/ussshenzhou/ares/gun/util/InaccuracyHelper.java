package cn.ussshenzhou.ares.gun.util;

import net.minecraft.util.RandomSource;

/// > 散布计算工具，将散布值转换为发射方向的角度偏移。
/// @author TACZ, USS_Shenzhou
public class InaccuracyHelper {
    /// > 对发射方向施加散布偏移。
    /// >
    /// > @param yaw        原始偏航角（度）
    /// >
    /// > @param pitch      原始俯仰角（度）
    /// >
    /// > @param inaccuracy 散布值（越大越不准）
    /// >
    /// > @param random     随机源
    /// >
    /// > @return float[2]：[偏移后的 yaw, 偏移后的 pitch]
    public static float[] applyInaccuracy(float yaw, float pitch, float inaccuracy, RandomSource random) {
        if (inaccuracy <= 0) {
            return new float[]{yaw, pitch};
        }
        float offsetYaw = (float) (random.nextGaussian() * inaccuracy);
        float offsetPitch = (float) (random.nextGaussian() * inaccuracy);
        return new float[]{yaw + offsetYaw, pitch + offsetPitch};
    }
}
