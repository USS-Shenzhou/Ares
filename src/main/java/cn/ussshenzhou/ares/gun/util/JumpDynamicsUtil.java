package cn.ussshenzhou.ares.gun.util;

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import cn.ussshenzhou.ares.gun.client.model.BedrockGunModel;
import cn.ussshenzhou.ares.gun.util.math.SecondOrderDynamics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/// @author TACZ, USS_Shenzhou
public class JumpDynamicsUtil {
    /// > 用于跳跃延滞动画的平滑
    private static final SecondOrderDynamics JUMPING_DYNAMICS = new SecondOrderDynamics(0.28f, 1f, 0.65f, new float[]{0});
    private static final float JUMPING_Y_SWAY = 2f;
    private static final float JUMPING_SWAY_TIME = 0.3f;
    private static final float LANDING_SWAY_TIME = 0.15f;
    private static float jumpingSwayProgress = 0;
    private static boolean lastOnGround = false;
    private static long jumpingTimeStamp = -1;

    public static void applyJumpingSway(BedrockGunModel model, float aimingProgress, float partialTicks) {
        if (jumpingTimeStamp == -1) {
            jumpingTimeStamp = System.currentTimeMillis();
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            double posY = Mth.lerp(partialTicks, Minecraft.getInstance().player.yOld, Minecraft.getInstance().player.getY());
            float velocityY = (float) (posY - Minecraft.getInstance().player.yOld) / partialTicks;
            if (player.onGround()) {
                if (!lastOnGround) {
                    jumpingSwayProgress = velocityY / -0.1f;
                    if (jumpingSwayProgress > 1) {
                        jumpingSwayProgress = 1;
                    }
                    lastOnGround = true;
                } else {
                    jumpingSwayProgress -= (System.currentTimeMillis() - jumpingTimeStamp) / (LANDING_SWAY_TIME * 1000);
                    if (jumpingSwayProgress < 0) {
                        jumpingSwayProgress = 0;
                    }
                }
            } else {
                if (lastOnGround) {
                    /// > 0.42 是玩家自然起跳的速度
                    jumpingSwayProgress = velocityY / 0.42f;
                    if (jumpingSwayProgress > 1) {
                        jumpingSwayProgress = 1;
                    }
                    lastOnGround = false;
                } else {
                    jumpingSwayProgress -= (System.currentTimeMillis() - jumpingTimeStamp) / (JUMPING_SWAY_TIME * 1000);
                    if (jumpingSwayProgress < 0) {
                        jumpingSwayProgress = 0;
                    }
                }
            }
        }
        jumpingTimeStamp = System.currentTimeMillis();
        JUMPING_DYNAMICS.target(0, JUMPING_Y_SWAY * jumpingSwayProgress);
        float ySway = JUMPING_DYNAMICS.get(0) * (1 - 0.8f * aimingProgress);
        BoneState rootBone = model.getGunRootBone();
        if (rootBone == null) {
            return;
        }
        rootBone.y -= ySway;
    }
}
