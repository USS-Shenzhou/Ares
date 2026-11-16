package cn.ussshenzhou.ares.gun.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;

/// > 伤害管线命中上下文抽象。
/// > <p>
/// > 把 {@link cn.ussshenzhou.ares.gun.gameplay.DamageHelper} 从「弹丸实体」解耦出来：飞行弹丸
/// > （{@link AbstractGunProjectile}）与瞬时射线（激光 hitscan）都实现此接口，从而共用整条
/// > 伤害管线（部位判定 / 距离衰减 / 蓄力 / 穿透 / 事件 / 钩子 / 击杀 / 击退），无需为激光重写。
/// @author TACZ, USS_Shenzhou
public interface IProjectileHitContext {
    /// > 射击者（可空）。
    @Nullable
    Entity getOwner();

    /// > 基础伤害。
    float getDamage();

    /// > 爆头倍率。
    float getHeadshotMultiplier();

    /// > 四肢倍率。
    float getLimbMultiplier();

    /// > 蓄力倍率。
    float getChargeMultiplier();

    /// > 穿透衰减系数（每穿透一个目标伤害乘一次）。
    float getPierceDecay();

    /// > 当前已穿透计数。
    int getPierceCount();

    /// > 穿透计数递增。
    void incrementPierceCount();

    /// > 击退强度。
    float getKnockback();

    /// > 已飞行/命中距离，用于距离衰减。
    float getDistanceTraveled();

    /// > 枪械 ID（可空）。
    @Nullable
    Identifier getGunId();

    /// > 弹药 ID（可空）。
    @Nullable
    Identifier getAmmoId();

    /// > 命中方向（已归一化），用于击退。替代实弹的 {@code getDeltaMovement().normalize()}。
    Vec3 getHitDirection();

    /// > {@link net.minecraft.world.damagesource.DamageSource} 的 projectile 参数：实弹返回自身，激光返回 shooter 或 null。
    @Nullable
    Entity getDamageSourceEntity();

    /// > 钩子上下文需要的弹丸实体：实弹返回自身，激光返回 null（无实体）。
    @Nullable
    AbstractGunProjectile getProjectileEntity();
}
