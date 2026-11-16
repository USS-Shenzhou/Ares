package cn.ussshenzhou.ares.gun.gameplay;

import cn.ussshenzhou.ares.gun.attribute.GunAttributes;
import cn.ussshenzhou.ares.gun.attribute.InaccuracyType;
import cn.ussshenzhou.ares.gun.custom.attribute.GunPropertiesCaches;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.attachment.entity.ServerPlayerGunCapability;
import cn.ussshenzhou.ares.gun.custom.CommonAssetsManager;
import cn.ussshenzhou.ares.gun.custom.ammo.AmmoData;
import cn.ussshenzhou.ares.gun.custom.ammo.AmmoIndex;
import cn.ussshenzhou.ares.gun.custom.gun.BoltType;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import cn.ussshenzhou.ares.gun.entity.AbstractGunProjectile;
import cn.ussshenzhou.ares.gun.entity.ProjectileContext;
import cn.ussshenzhou.ares.gun.entity.ProjectileProvider;
import cn.ussshenzhou.ares.gun.entity.ProjectileRegistry;
import cn.ussshenzhou.ares.gun.network.message.s2c.S2CShootPacket;
import cn.ussshenzhou.ares.gun.util.AmmoDataHelper;
import cn.ussshenzhou.ares.gun.util.MountedWeaponHelper;
import cn.ussshenzhou.ares.gun.util.InaccuracyHelper;
import cn.ussshenzhou.t88.network.NetworkHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

/// > 射击公共逻辑
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW hooks and Forge events are removed.
public class ShootHelper {
    private static final float DEFAULT_BULLET_GRAVITY = 0.05f;

    /// > 执行一次标准射击（伤害/速度倍率均为 1.0）。
    public static ShootResult fireOneBullet(ServerPlayer player, ItemStack gun, float yaw, float pitch,
                                            BoltType boltType) {
        return fireProjectiles(player, gun, yaw, pitch, 1.0f, 1.0f, boltType,
                InteractionHand.MAIN_HAND, MuzzleReference.primary());
    }

    public static ShootResult fireOneBullet(ServerPlayer player, ItemStack gun, float yaw, float pitch,
                                            BoltType boltType, InteractionHand hand) {
        return fireProjectiles(player, gun, yaw, pitch, 1.0f, 1.0f, boltType, hand, MuzzleReference.primary());
    }

    /// > 执行一次射击，支持伤害和速度倍率（用于蓄力射击等）。
    public static ShootResult fireOneBullet(ServerPlayer player, ItemStack gun, float yaw, float pitch,
                                            float damageMultiplier, float speedMultiplier, BoltType boltType) {
        return fireProjectiles(player, gun, yaw, pitch, damageMultiplier, speedMultiplier, boltType,
                InteractionHand.MAIN_HAND, MuzzleReference.primary());
    }

    public static ShootResult fireOneBullet(ServerPlayer player, ItemStack gun, float yaw, float pitch,
                                            float damageMultiplier, float speedMultiplier, BoltType boltType,
                                            InteractionHand hand) {
        return fireProjectiles(player, gun, yaw, pitch, damageMultiplier, speedMultiplier, boltType,
                hand, MuzzleReference.primary());
    }

    public static ShootResult fireOneBullet(ServerPlayer player, ItemStack gun, float yaw, float pitch,
                                            float damageMultiplier, float speedMultiplier, BoltType boltType,
                                            InteractionHand hand, MuzzleReference muzzleReference) {
        return fireProjectiles(player, gun, yaw, pitch, damageMultiplier, speedMultiplier, boltType,
                hand, muzzleReference);
    }

    /// > 补膛（re-chamber）：从弹匣移一发到膛。
    /// > 弹匣有弹时 AmmoCount-1、chambered=true；无弹则保持 chambered=false。
    public static void reChamber(ItemStack gun) {
        if (AmmoDataHelper.getAmmoCount(gun) > 0) {
            AmmoDataHelper.consumeAmmo(gun, 1);
            AmmoDataHelper.setChambered(gun, true);
        }
    }

    public static ShootResult fireProjectiles(ServerPlayer player, ItemStack gun, float yaw, float pitch,
                                              float damageMultiplier, float speedMultiplier, BoltType boltType,
                                              InteractionHand hand) {
        return fireProjectiles(player, gun, yaw, pitch, damageMultiplier, speedMultiplier, boltType,
                hand, MuzzleReference.primary());
    }

    public static ShootResult fireProjectiles(ServerPlayer player, ItemStack gun, float yaw, float pitch,
                                              float damageMultiplier, float speedMultiplier, BoltType boltType,
                                              InteractionHand hand, MuzzleReference muzzleReference) {
        if (player == null) {
            return ShootResult.BLOCKED;
        }
        boolean usesChamber = boltType.usesChamber();
        /// > 开火前置检查：open 看弹匣存量；closed/manual 看膛内是否就绪
        if (usesChamber) {
            if (!AmmoDataHelper.isChambered(gun)) {
                return ShootResult.NO_AMMO;
            }
        } else {
            if (!AmmoDataHelper.hasAmmo(gun)) {
                return ShootResult.NO_AMMO;
            }
        }

        GunPropertiesCaches cache = GunPropertiesCaches.of(gun);
        float damage = (float) cache.getAttribute(GunAttributes.DAMAGE) * damageMultiplier;
        float speed = (float) cache.getAttribute(GunAttributes.AMMO_SPEED) * speedMultiplier / 20f;
        int pierce = (int) cache.getAttribute(GunAttributes.PIERCE);
        float headshotMultiplier = (float) cache.getAttribute(GunAttributes.HEADSHOT_MULTIPLIER);
        float penetration = (float) cache.getAttribute(GunAttributes.PENETRATION);
        float limbMultiplier = (float) cache.getAttribute(GunAttributes.LIMB_MULTIPLIER);
        float pierceDecay = (float) cache.getAttribute(GunAttributes.PIERCE_DECAY);
        float knockback = (float) cache.getAttribute(GunAttributes.KNOCKBACK);

        InaccuracyType inaccuracyType = InaccuracyType.getInaccuracyType(player);
        float inaccuracy = (float) cache.getInaccuracy(inaccuracyType);

        /// > 从 GunData 读取基础参数（兼容下挂武器：IMountedWeapon 的 UnderBarrelGunData 也是 GunData）
        GunData gunData = MountedWeaponHelper.resolveGunData(gun);
        float gravity = gunData != null ? gunData.getBulletGravity() : DEFAULT_BULLET_GRAVITY;
        int lifespan = gunData != null ? gunData.getBulletLifespan() : 200;

        /// > 解析弹种数据
        AmmoData ammoData = getAmmoData(gun);

        /// > 解析弹丸数量：AmmoData > GunData > 1
        int bulletAmount = resolveBulletAmount(ammoData, gunData);

        /// > 曳光弹判定
        int tracerFrequency = gunData != null ? gunData.getTracerFrequency() : -1;
        boolean isTracerShot = false;
        if (tracerFrequency >= 0) {
            IGunOperator op = IGunOperator.asGunHolder(player).orElse(null);
            if (op instanceof ServerPlayerGunCapability serverCap) {
                int count = serverCap.incrementShotCounter();
                isTracerShot = tracerFrequency == 0 || count % tracerFrequency == 0;
            }
        }

        /// > 获取枪械 ID 和弹药 ID（兼容下挂武器）
        Identifier currentGunId = MountedWeaponHelper.resolveCustomId(gun);
        Identifier currentAmmoId = AmmoDataHelper.getAmmoId(gun);

        /// > 弹药消耗：
        /// > - open：直接从弹匣扣一发。
        /// > - closed/manual：打出膛内那发（chambered → false）。closed 随后即时补膛；
        /// >   manual 等 BoltSegment 窗口结束后补膛。
        if (usesChamber) {
            AmmoDataHelper.setChambered(gun, false);
            if (boltType == BoltType.CLOSED) {
                reChamber(gun);
            }
        } else {
            AmmoDataHelper.consumeAmmo(gun, 1);
        }

        // FIXME REVIEW the projectile type key is removed from GunData/AmmoData, so every shot uses the default bullet provider.
        ProjectileProvider provider = ProjectileRegistry.get(ProjectileRegistry.DEFAULT_KEY);
        ProjectileContext projectileCtx = new ProjectileContext(
                player, player.level(), gun, gunData, ammoData, cache, currentGunId, currentAmmoId,
                hand, muzzleReference != null ? muzzleReference : MuzzleReference.primary(),
                damage, speed, pierce, gravity, lifespan, headshotMultiplier, penetration,
                limbMultiplier, pierceDecay, knockback, isTracerShot);

        for (int i = 0; i < bulletAmount; i++) {
            /// > 每颗弹丸独立散布
            float[] adjusted = InaccuracyHelper.applyInaccuracy(yaw, pitch, inaccuracy, player.getRandom());
            provider.fire(projectileCtx, adjusted[0], adjusted[1]);
        }

        NetworkHelper.sendToPlayersTrackingEntityAndSelf(player, new S2CShootPacket(player.getId(), hand,
                projectileCtx.muzzleReference()));

        return ShootResult.SUCCESS;
    }

    /// > 把 {@link ProjectileContext} 的通用射前字段设置到飞行弹丸上，并按给定角度发射。
    /// > <p>
    /// > 供 {@link ProjectileProvider#fire} 默认实现使用。抽出为静态方法后，实体类与 hitscan
    /// > 类走同一发射入口，{@link #fireProjectiles} 循环不再区分弹丸种类。
    /// >
    /// > @param projectile 待设置的弹丸实体
    /// > @param ctx        射前上下文
    /// > @param yaw        已散布调整的偏航角
    /// > @param pitch      已散布调整的俯仰角
    public static void applyCommonFields(AbstractGunProjectile projectile, ProjectileContext ctx,
                                         float yaw, float pitch) {
        ServerPlayer player = ctx.player();
        projectile.setOwner(player);
        projectile.setDamage(ctx.damage());
        projectile.setPierce(ctx.pierce());
        projectile.setProjectileGravity(ctx.gravity());
        projectile.setLifespan(ctx.lifespan());
        projectile.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        projectile.shoot(yaw, pitch, ctx.speed() > 0 ? ctx.speed() : 4.0f);

        projectile.setGunId(ctx.gunId());
        projectile.setAmmoId(ctx.ammoId());
        projectile.setTracer(ctx.tracer());
        projectile.setShooterHand(ctx.hand());
        projectile.setMuzzleReference(ctx.muzzleReference());
        projectile.setHeadshotMultiplier(ctx.headshotMultiplier());
        projectile.setPenetration(ctx.penetration());
        projectile.setLimbMultiplier(ctx.limbMultiplier());
        projectile.setPierceDecay(ctx.pierceDecay());
        projectile.setKnockback(ctx.knockback());
    }

    @Nullable
    private static AmmoData getAmmoData(ItemStack gun) {
        Identifier ammoId = AmmoDataHelper.getAmmoId(gun);
        if (ammoId == null) {
            return null;
        }
        return CommonAssetsManager.get().getIndex(ammoId, AmmoIndex.TYPE)
                .map(AmmoIndex::getData).orElse(null);
    }

    private static int resolveBulletAmount(@Nullable AmmoData ammoData, @Nullable GunData gunData) {
        if (ammoData != null && ammoData.getBulletAmount() > 0) {
            return ammoData.getBulletAmount();
        }
        if (gunData != null) {
            return gunData.getBulletAmount();
        }
        return 1;
    }
}
