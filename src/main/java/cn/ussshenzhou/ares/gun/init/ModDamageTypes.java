package cn.ussshenzhou.ares.gun.init;

import cn.ussshenzhou.ares.Ares;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW the explosion damage type is removed.
public class ModDamageTypes {
    public static final ResourceKey<DamageType> GUN = ResourceKey.create(
            Registries.DAMAGE_TYPE, Ares.modLoc("gun"));

    public static final ResourceKey<DamageType> GUN_HEADSHOT = ResourceKey.create(
            Registries.DAMAGE_TYPE, Ares.modLoc("gun_headshot"));

    public static DamageSource gun(Level level, @Nullable Entity projectile, @Nullable Entity shooter) {
        return new DamageSource(
                level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(GUN),
                projectile, shooter);
    }

    public static DamageSource gunHeadshot(Level level, @Nullable Entity projectile, @Nullable Entity shooter) {
        return new DamageSource(
                level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(GUN_HEADSHOT),
                projectile, shooter);
    }
}
