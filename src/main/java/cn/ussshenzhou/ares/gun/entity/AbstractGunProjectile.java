package cn.ussshenzhou.ares.gun.entity;

import cn.ussshenzhou.ares.gun.gameplay.MuzzleReference;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import javax.annotation.Nullable;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW spawn data uses NeoForge IEntityWithComplexSpawn with RegistryFriendlyByteBuf, getAddEntityPacket is removed, and
// saves use ValueInput/ValueOutput.
public abstract class AbstractGunProjectile extends Projectile implements IEntityWithComplexSpawn, IProjectileHitContext {
    protected float damage;
    protected float speed;
    protected float gravity;
    protected int pierce;
    protected int lifespan = 200;
    protected int age;
    protected boolean isTracer;
    @Nullable
    protected Identifier gunId;
    @Nullable
    protected Identifier ammoId;
    protected final IntSet hitEntityIds = new IntOpenHashSet();

    /// > 伤害管线新增字段
    protected Vec3 startPos;
    protected float distanceTraveled;
    protected float penetration;
    protected int pierceCount;
    protected float pierceDecay = 0.7f;
    protected float headshotMultiplier = 1.5f;
    protected float limbMultiplier = 0.75f;
    protected float knockback;
    protected float chargeMultiplier = 1.0f;

    /// > 射击来源手（主/副）。仅用于客户端第一人称曳光弹按手取对应枪口偏移；
    /// > 服务端权威写入，随 spawn data 同步。默认主手，单枪/旧路径零影响。
    protected InteractionHand shooterHand = InteractionHand.MAIN_HAND;
    protected MuzzleReference muzzleReference = MuzzleReference.primary();

    protected AbstractGunProjectile(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    public void shoot(float yaw, float pitch, float speed) {
        this.speed = speed;
        this.startPos = this.position();
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        this.setDeltaMovement(new Vec3(x, y, z).normalize().scale(speed));
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        Entity entity = this.level().getEntity(additionalData.readInt());
        if (entity != null) {
            this.setOwner(entity);
        }

        /// > 跳过服务端写入的旋转值（shoot() 未设置旋转，可能是 0）
        additionalData.readFloat();
        additionalData.readFloat();
        double mx = additionalData.readDouble();
        double my = additionalData.readDouble();
        double mz = additionalData.readDouble();
        setDeltaMovement(mx, my, mz);
        this.gravity = additionalData.readFloat();
        this.shooterHand = additionalData.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        this.muzzleReference = MuzzleReference.read(additionalData);

        /// > 从 motion 精确计算初始旋转，同时初始化旧值，避免首帧大角度插值
        double horizontalDist = new Vec3(mx, my, mz).horizontalDistance();
        float yRot = (float) Math.toDegrees(Mth.atan2(mx, mz));
        float xRot = (float) Math.toDegrees(Mth.atan2(my, horizontalDist));
        setXRot(xRot);
        setYRot(yRot);
        this.xRotO = xRot;
        this.yRotO = yRot;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        Entity entity = getOwner();
        buffer.writeInt(entity != null ? entity.getId() : 0);

        buffer.writeFloat(getXRot());
        buffer.writeFloat(getYRot());
        buffer.writeDouble(getDeltaMovement().x);
        buffer.writeDouble(getDeltaMovement().y);
        buffer.writeDouble(getDeltaMovement().z);
        buffer.writeFloat(this.gravity);
        buffer.writeBoolean(this.shooterHand == InteractionHand.OFF_HAND);
        this.muzzleReference.write(buffer);
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        if (age > lifespan) {
            onLifespanExpired();
            return;
        }
        updateMovement();
    }

    /// > 寿命耗尽时的处理。默认直接移除。
    /// > 子类可覆写以实现到期爆炸等行为。
    protected void onLifespanExpired() {
        this.discard();
    }

    /// > 每 tick 的运动与碰撞处理。
    /// > 由各子类定义具体运动模型（高速直线、抛物弹跳、制导等）。
    protected abstract void updateMovement();

    @Override
    protected void onHitEntity(EntityHitResult result) {
        onProjectileHitEntity(result);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        onProjectileHitBlock(result);
    }

    protected abstract void onProjectileHitEntity(EntityHitResult result);

    protected abstract void onProjectileHitBlock(BlockHitResult result);

    /// > 禁用原版位置插值，避免服务端同步包与客户端自行模拟的位置互相冲突导致抽搐
    // FIXME REVIEW 26.x replaced lerpTo with the final moveOrInterpolateTo and an InterpolationHandler; vanilla interpolation is not disabled yet.
    /*
    @Override
    public void lerpTo(double pX, double pY, double pZ, float pYRot, float pXRot, int pLerpSteps, boolean pTeleport) {
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
    }
    */

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloatOr("Damage", 0);
        this.speed = tag.getFloatOr("Speed", 0);
        this.gravity = tag.getFloatOr("Gravity", 0);
        this.pierce = tag.getIntOr("Pierce", 0);
        this.lifespan = tag.getIntOr("Lifespan", 0);
        this.age = tag.getIntOr("Age", 0);
        this.penetration = tag.getFloatOr("Penetration", 0);
        this.pierceCount = tag.getIntOr("PierceCount", 0);
        this.pierceDecay = tag.getFloatOr("PierceDecay", 0);
        this.distanceTraveled = tag.getFloatOr("DistanceTraveled", 0);
        this.headshotMultiplier = tag.getFloatOr("HeadshotMultiplier", 0);
        this.limbMultiplier = tag.getFloatOr("LimbMultiplier", 0);
        this.knockback = tag.getFloatOr("Knockback", 0);
        this.chargeMultiplier = tag.getFloatOr("ChargeMultiplier", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", this.damage);
        tag.putFloat("Speed", this.speed);
        tag.putFloat("Gravity", this.gravity);
        tag.putInt("Pierce", this.pierce);
        tag.putInt("Lifespan", this.lifespan);
        tag.putInt("Age", this.age);
        tag.putFloat("Penetration", this.penetration);
        tag.putInt("PierceCount", this.pierceCount);
        tag.putFloat("PierceDecay", this.pierceDecay);
        tag.putFloat("DistanceTraveled", this.distanceTraveled);
        tag.putFloat("HeadshotMultiplier", this.headshotMultiplier);
        tag.putFloat("LimbMultiplier", this.limbMultiplier);
        tag.putFloat("Knockback", this.knockback);
        tag.putFloat("ChargeMultiplier", this.chargeMultiplier);
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setProjectileGravity(float gravity) {
        this.gravity = gravity;
    }

    public void setPierce(int pierce) {
        this.pierce = pierce;
    }

    public void setLifespan(int lifespan) {
        this.lifespan = lifespan;
    }

    public boolean isTracer() {
        return isTracer;
    }

    public void setTracer(boolean tracer) {
        this.isTracer = tracer;
    }

    @Nullable
    public Identifier getGunId() {
        return gunId;
    }

    public void setGunId(@Nullable Identifier gunId) {
        this.gunId = gunId;
    }

    @Nullable
    public Identifier getAmmoId() {
        return ammoId;
    }

    public void setAmmoId(@Nullable Identifier ammoId) {
        this.ammoId = ammoId;
    }

    // FIXME REVIEW getGravity/setGravity renamed to getProjectileGravity/setProjectileGravity because Entity.getGravity is final
    // in 26.x.
    public float getProjectileGravity() {
        return gravity;
    }

    public float getSpeed() {
        return speed;
    }

    public float getDistanceTraveled() {
        return distanceTraveled;
    }

    public float getPenetration() {
        return penetration;
    }

    public void setPenetration(float penetration) {
        this.penetration = penetration;
    }

    public int getPierceCount() {
        return pierceCount;
    }

    public void incrementPierceCount() {
        this.pierceCount++;
    }

    public float getPierceDecay() {
        return pierceDecay;
    }

    public void setPierceDecay(float pierceDecay) {
        this.pierceDecay = pierceDecay;
    }

    public float getHeadshotMultiplier() {
        return headshotMultiplier;
    }

    public void setHeadshotMultiplier(float headshotMultiplier) {
        this.headshotMultiplier = headshotMultiplier;
    }

    public float getLimbMultiplier() {
        return limbMultiplier;
    }

    public void setLimbMultiplier(float limbMultiplier) {
        this.limbMultiplier = limbMultiplier;
    }

    public float getKnockback() {
        return knockback;
    }

    public void setKnockback(float knockback) {
        this.knockback = knockback;
    }

    public InteractionHand getShooterHand() {
        return shooterHand;
    }

    public void setShooterHand(InteractionHand shooterHand) {
        this.shooterHand = shooterHand != null ? shooterHand : InteractionHand.MAIN_HAND;
    }

    public MuzzleReference getMuzzleReference() {
        return muzzleReference;
    }

    public void setMuzzleReference(MuzzleReference muzzleReference) {
        this.muzzleReference = muzzleReference != null ? muzzleReference : MuzzleReference.primary();
    }

    public float getChargeMultiplier() {
        return chargeMultiplier;
    }

    public void setChargeMultiplier(float chargeMultiplier) {
        this.chargeMultiplier = chargeMultiplier;
    }

    /// > ===== IProjectileHitContext =====
    /// > 飞行弹丸的命中方向即当前运动方向；DamageSource 的 projectile 参数与钩子弹丸实体均为自身。

    @Override
    public Vec3 getHitDirection() {
        return getDeltaMovement().normalize();
    }

    @Override
    public Entity getDamageSourceEntity() {
        return this;
    }

    @Override
    public AbstractGunProjectile getProjectileEntity() {
        return this;
    }
}
