package cn.ussshenzhou.ares.gun.gameplay;

import cn.ussshenzhou.ares.gun.custom.CommonAssetsManager;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import cn.ussshenzhou.ares.gun.custom.gun.GunIndex;
import cn.ussshenzhou.ares.gun.entity.IProjectileHitContext;
import cn.ussshenzhou.ares.gun.init.ModDamageTypes;
import cn.ussshenzhou.ares.gun.network.message.s2c.S2CGunHitMessage;
import cn.ussshenzhou.t88.network.NetworkHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;

import java.util.List;

/// > 枪械伤害管线核心。
/// >
/// > 计算流程：部位倍率 → 距离衰减 → 蓄力倍率 → 自定义护甲减伤（阶段 2 接入）→ 穿透衰减 → 事件 → 应用伤害 → 附加效果。
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW hooks and Forge events are removed from the damage pipeline.
public class DamageHelper {
    /// > 对命中实体执行完整伤害管线。
    /// >
    /// > @param projectile 命中上下文（飞行弹丸或激光 hitscan）
    /// >
    /// > @param target     命中目标
    /// >
    /// > @param hitPos     命中点坐标
    /// >
    /// > @param gunStack   射击者手持的枪械（用于事件传递，可为空）
    public static void applyDamage(IProjectileHitContext projectile, Entity target, Vec3 hitPos, ItemStack gunStack) {
        Entity shooter = projectile.getOwner();

        /// > 1. 部位判定
        HitZone hitZone = HitZone.CHEST;
        if (target instanceof LivingEntity living) {
            hitZone = HitZoneHelper.determine(living, hitPos);
        }

        /// > 2. 基础伤害（已由 ShootHelper 设置到弹丸上）
        float damage = projectile.getDamage();

        /// > 3. 部位倍率
        float zoneMultiplier = switch (hitZone) {
            case HEAD -> projectile.getHeadshotMultiplier();
            case CHEST -> 1.0f;
            case LIMB -> projectile.getLimbMultiplier();
        };
        damage *= zoneMultiplier;

        /// > 4. 距离衰减
        GunData gunData = resolveGunData(projectile.getGunId());
        if (gunData != null) {
            damage *= calculateFalloff(projectile.getDistanceTraveled(), gunData.getDamageFalloff());
        }

        /// > 5. 蓄力倍率
        damage *= projectile.getChargeMultiplier();

        /// > 6. 自定义护甲减伤（阶段 2 接入点）
        /// > TODO: ArmorResult armorResult = resolveCustomArmor(target, hitZone, projectile.getPenetration());

        /// > 7. 穿透伤害递减
        damage *= (float) Math.pow(projectile.getPierceDecay(), projectile.getPierceCount());

        /// > 9. 构建 DamageSource 并应用伤害
        DamageSource damageSource = hitZone == HitZone.HEAD
                ? ModDamageTypes.gunHeadshot(target.level(), projectile.getDamageSourceEntity(), shooter)
                : ModDamageTypes.gun(target.level(), projectile.getDamageSourceEntity(), shooter);

        // FIXME REVIEW hurt replaced with hurtOrSimulate; knockback below uses the 26.x signature with the DamageSource and
        // damage.
        target.hurtOrSimulate(damageSource, damage);

        /// > 10. 附加效果：击退（脚本可覆盖强度）
        float knockbackStrength = projectile.getKnockback();
        if (knockbackStrength > 0 && target instanceof LivingEntity living) {
            Vec3 knockbackDir = projectile.getHitDirection();
            living.knockback(knockbackStrength, -knockbackDir.x, -knockbackDir.z, damageSource, damage);
        }

        // FIXME REVIEW TACZ sent the hit feedback from a GunHurtEvent.Post listener; the event API is not ported, so it is sent
        // here directly.
        if (shooter instanceof ServerPlayer serverShooter) {
            boolean killed = target instanceof LivingEntity living && living.isDeadOrDying();
            NetworkHelper.sendToPlayer(serverShooter, new S2CGunHitMessage(hitZone == HitZone.HEAD, killed));
        }

        /// > 穿透计数递增
        projectile.incrementPierceCount();
    }

    /// > 根据距离衰减曲线计算伤害倍率。
    /// > 在定义的距离点之间线性插值，超过最后一个点使用最后的倍率。
    public static float calculateFalloff(float distance, @Nullable List<GunData.DamageFalloffPoint> falloff) {
        if (falloff == null || falloff.isEmpty()) {
            return 1.0f;
        }

        /// > 距离小于第一个点，使用第一个点的倍率
        if (distance <= falloff.get(0).getDistance()) {
            return falloff.get(0).getMultiplier();
        }

        /// > 在相邻点之间线性插值
        for (int i = 1; i < falloff.size(); i++) {
            GunData.DamageFalloffPoint prev = falloff.get(i - 1);
            GunData.DamageFalloffPoint curr = falloff.get(i);
            if (distance <= curr.getDistance()) {
                float range = curr.getDistance() - prev.getDistance();
                if (range <= 0) {
                    return curr.getMultiplier();
                }
                float t = (distance - prev.getDistance()) / range;
                return prev.getMultiplier() + t * (curr.getMultiplier() - prev.getMultiplier());
            }
        }

        /// > 超过最后一个点，使用最后的倍率
        return falloff.get(falloff.size() - 1).getMultiplier();
    }

    @Nullable
    private static GunData resolveGunData(@Nullable Identifier gunId) {
        if (gunId == null) {
            return null;
        }
        return CommonAssetsManager.get().getIndex(gunId, GunIndex.TYPE)
                .map(GunIndex::getData).orElse(null);
    }
}
