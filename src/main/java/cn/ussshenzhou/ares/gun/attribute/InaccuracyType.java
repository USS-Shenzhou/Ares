package cn.ussshenzhou.ares.gun.attribute;

import com.google.gson.annotations.SerializedName;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

/// @author TACZ, USS_Shenzhou
public enum InaccuracyType {
    /// > 站立不动
    @SerializedName("stand")
    STAND,
    /// > 移动
    @SerializedName("move")
    MOVE,
    /// > 潜行，认为是其他 FPS 游戏中的半蹲
    @SerializedName("sneak")
    SNEAK,
    /// > 趴下，原版确实可以趴下
    @SerializedName("lie")
    LIE,
    /// > 瞄准状态
    @SerializedName("aim")
    AIM,
    /// > 战术拒枪姿态（本体第二瞄位）
    @SerializedName("tactical")
    TACTICAL;

    /// > 获取当前的不准确度状态
    /// >
    /// > @param livingEntity 射手
    /// >
    /// > @return 不准度情况
    public static InaccuracyType getInaccuracyType(LivingEntity livingEntity) {
        float aimingProgress = IGunOperator.asGunHolder(livingEntity)
                .map(IGunOperator::aimingProgress).orElse(0f);
        /// > 瞄准优先级最高
        if (aimingProgress == 1.0f) {
            /// > 拒枪姿态由客户端上报、服务端权威落地，二者口径一致
            boolean tactical = IGunOperator.asGunHolder(livingEntity)
                    .map(IGunOperator::isTacticalStance).orElse(false);
            return tactical ? InaccuracyType.TACTICAL : InaccuracyType.AIM;
        }
        /// > MOJANG 的奇妙设计，趴下的姿势名称是 SWIMMING
        if (!livingEntity.isSwimming() && livingEntity.getPose() == Pose.SWIMMING) {
            return InaccuracyType.LIE;
        }
        if (livingEntity.getPose() == Pose.CROUCHING) {
            return InaccuracyType.SNEAK;
        }
        if (isMove(livingEntity)) {
            return InaccuracyType.MOVE;
        }
        return InaccuracyType.STAND;
    }

    private static boolean isMove(LivingEntity livingEntity) {
        // FIXME REVIEW 26.x Entity has no walkDist/walkDistO (moved to the client-only ClientAvatarState); uses the horizontal
        // getKnownMovement() * 0.6, the factor vanilla used when adding to walkDist every tick.
        double distance = livingEntity.getKnownMovement().horizontalDistance() * 0.6;
        /// > todo
/// >        if (livingEntity instanceof Player player) {
/// >            distance = HitboxHelper.getPlayerVelocity(player).length();
/// >        }
        return distance > 0.05f;
    }

    /// > 是否为瞄准类散布（含拒枪姿态）：准星平滑过渡据此把当前姿态视作「站姿」。
    public boolean isAim() {
        return this == AIM || this == TACTICAL;
    }
}
