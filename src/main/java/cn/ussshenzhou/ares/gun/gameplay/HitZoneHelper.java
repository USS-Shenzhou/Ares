package cn.ussshenzhou.ares.gun.gameplay;

import cn.ussshenzhou.ares.gun.gameplay.HitZone;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/// > 根据命中点 Y 坐标判定身体部位。
/// > <p>
/// > 默认判定（基于实体高度比例）：
/// > <ul>
/// >   <li>HEAD:  眼睛高度 - 0.1 ~ 实体顶部</li>
/// >   <li>CHEST: 实体高度 × 0.4 ~ 眼睛高度 - 0.1</li>
/// >   <li>LIMB:  实体底部 ~ 实体高度 × 0.4</li>
/// > </ul>
/// @author TACZ, USS_Shenzhou
public class HitZoneHelper {
    public static HitZone determine(LivingEntity target, Vec3 hitPos) {
        double entityY = target.getY();
        double entityHeight = target.getBbHeight();
        double hitY = hitPos.y;

        double relativeY = hitY - entityY;

        double eyeOffset = target.getEyeHeight() - 0.1;
        double limbThreshold = entityHeight * 0.4;

        if (relativeY >= eyeOffset) {
            return HitZone.HEAD;
        } else if (relativeY >= limbThreshold) {
            return HitZone.CHEST;
        } else {
            return HitZone.LIMB;
        }
    }
}
