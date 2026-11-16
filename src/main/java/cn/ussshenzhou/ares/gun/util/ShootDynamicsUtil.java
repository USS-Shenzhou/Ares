package cn.ussshenzhou.ares.gun.util;

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.math.Axis;
import cn.ussshenzhou.ares.client.model.BedrockGunModel;
import cn.ussshenzhou.ares.gun.util.math.PerlinNoise;

/// > 第一人称后座动效工具
/// @author TACZ, USS_Shenzhou
public class ShootDynamicsUtil {
    private static final PerlinNoise SHOOT_X_SWAY_NOISE = new PerlinNoise(-0.2f, 0.2f, 400);
    private static final PerlinNoise SHOOT_Y_ROTATION_NOISE = new PerlinNoise(-0.0136f, 0.0136f, 100);
    private static final float SHOOT_Y_SWAY = -0.1f;
    private static final float SHOOT_ANIMATION_TIME = 0.3f;

    public static void applyShootSwayAndRotation(BedrockGunModel model, float aimingProgress, long shootTimeStamp) {
        float progress = 1 - (System.currentTimeMillis() - shootTimeStamp) / (SHOOT_ANIMATION_TIME * 1000);
        if (progress < 0) {
            progress = 0;
        }
        progress = (float) easeOutCubic(progress);
        BoneState rootBone = model.getGunRootBone();
        if (rootBone == null) {
            return;
        }
        rootBone.x += SHOOT_X_SWAY_NOISE.getValue() * progress * (1 - aimingProgress);
        rootBone.y += -SHOOT_Y_SWAY * progress * (1 - aimingProgress);
        rootBone.rotation.mul(Axis.YP.rotation(SHOOT_Y_ROTATION_NOISE.getValue() * progress * (1 - aimingProgress)));
    }

    public static double easeOutCubic(double x) {
        return 1 - Math.pow(1 - x, 3);
    }
}
