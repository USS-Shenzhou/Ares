package cn.ussshenzhou.ares.gun.entity;

import cn.ussshenzhou.ares.gun.gameplay.DamageHelper;
import cn.ussshenzhou.ares.gun.init.ModEntities;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW the hit-block hook is removed.
public class BulletEntity extends AbstractGunProjectile {

    /// > 客户端曳光弹渲染用（不同步，仅客户端使用）
    private Vector3f firstPersonRenderOffset;
    private float cameraXRot;
    private float cameraYRot;
    private Vec3 initialDeltaMovement;

    public BulletEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    public BulletEntity(Level level) {
        super(ModEntities.BULLET.get(), level);
    }

    @Override
    protected void onProjectileHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide()) {
            /// > 查找射击者手持的枪械 ItemStack（用于事件传递）
            ItemStack gunStack = ItemStack.EMPTY;
            if (this.getOwner() instanceof net.minecraft.world.entity.LivingEntity living) {
                gunStack = living.getMainHandItem();
            }
            DamageHelper.applyDamage(this, result.getEntity(), result.getLocation(), gunStack);
            if (--this.pierce <= 0) {
                this.discard();
            }
        }
    }

    @Override
    protected void onProjectileHitBlock(BlockHitResult result) {
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    /// > 高速动能弹运动模型：直线飞行 + 重力 + 射线扫描穿透 + 伤害管线。
    @Override
    protected void updateMovement() {
        Vec3 motion = this.getDeltaMovement();

        /// > 累加飞行距离（每 tick 的位移长度，单位：格/tick → 乘 20 后约等于 m/s）
        distanceTraveled += (float) motion.length();

        /// > 旋转：直接从运动向量精确计算，不做 lerpRotation 平滑
        /// > lerpRotation 每 tick 只收敛 20%，会导致渲染插值时角度在精确值和滞后值之间振荡
        double horizontalDist = motion.horizontalDistance();
        float newYRot = (float) Math.toDegrees(Mth.atan2(motion.x, motion.z));
        float newXRot = (float) Math.toDegrees(Mth.atan2(motion.y, horizontalDist));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.setYRot(newYRot);
        this.setXRot(newXRot);

        Vec3 pos = this.position();
        Vec3 nextPos = pos.add(motion);

        /// > 方块碰撞检测
        BlockHitResult blockHit = this.level().clip(new ClipContext(
                pos, nextPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));

        /// > 实体碰撞检测：扫描运动路径 AABB 范围内的实体
        Vec3 traceEnd = (blockHit.getType() == HitResult.Type.BLOCK) ? blockHit.getLocation() : nextPos;
        List<EntityHitResult> entityHits = findEntityHits(pos, traceEnd);

        /// > 按距离排序
        entityHits.sort(Comparator.comparingDouble(h -> h.getLocation().distanceToSqr(pos)));

        /// > 处理实体碰撞（穿透支持）
        for (EntityHitResult entityHit : entityHits) {
            /// > 如果方块碰撞点更近，先处理方块
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                double blockDistSq = blockHit.getLocation().distanceToSqr(pos);
                double entityDistSq = entityHit.getLocation().distanceToSqr(pos);
                if (blockDistSq < entityDistSq) {
                    onHitBlock(blockHit);
                    this.setPos(blockHit.getLocation());
                    applyGravityAndFriction();
                    return;
                }
            }

            onHitEntity(entityHit);
            hitEntityIds.add(entityHit.getEntity().getId());

            if (this.isRemoved()) {
                return;
            }
        }

        /// > 处理方块碰撞
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            onHitBlock(blockHit);
            this.setPos(blockHit.getLocation());
            applyGravityAndFriction();
            return;
        }

        /// > 位置更新
        this.setPos(nextPos.x, nextPos.y, nextPos.z);
        applyGravityAndFriction();
    }

    private void applyGravityAndFriction() {
        Vec3 motion = this.getDeltaMovement();
        float currentGravity = this.gravity;
        float friction = 0.0F;
        if (this.isInWater()) {
            friction = 0.4F;
            currentGravity *= 0.6F;
        }
        this.setDeltaMovement(motion.scale(1 - friction).add(0, -currentGravity, 0));
    }

    private List<EntityHitResult> findEntityHits(Vec3 from, Vec3 to) {
        AABB searchBox = new AABB(from, to).inflate(1.0);
        List<Entity> candidates = this.level().getEntities(this, searchBox, e -> {
            if (e.isSpectator() || !e.isAlive() || !e.isPickable()) {
                return false;
            }
            if (e.equals(this.getOwner())) {
                return false;
            }
            if (hitEntityIds.contains(e.getId())) {
                return false;
            }
            return true;
        });

        List<EntityHitResult> results = new ArrayList<>();
        for (Entity entity : candidates) {
            AABB entityBB = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> hitVec = entityBB.clip(from, to);
            if (hitVec.isPresent()) {
                results.add(new EntityHitResult(entity, hitVec.get()));
            }
        }
        return results;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        super.writeSpawnData(buf);
        buf.writeBoolean(this.isTracer);
        buf.writeBoolean(this.gunId != null);
        if (this.gunId != null) {
            buf.writeIdentifier(this.gunId);
        }
        buf.writeBoolean(this.ammoId != null);
        if (this.ammoId != null) {
            buf.writeIdentifier(this.ammoId);
        }

    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        super.readSpawnData(buf);
        this.isTracer = buf.readBoolean();
        if (buf.readBoolean()) {
            this.gunId = buf.readIdentifier();
        }
        if (buf.readBoolean()) {
            this.ammoId = buf.readIdentifier();
        }

    }

    public Vector3f getFirstPersonRenderOffset() {
        return firstPersonRenderOffset;
    }

    public void setFirstPersonRenderOffset(Vector3f offset) {
        this.firstPersonRenderOffset = offset;
    }

    public Vec3 getInitialDeltaMovement() {
        return initialDeltaMovement;
    }

    public float getCameraXRot() {
        return cameraXRot;
    }

    public void setCameraXRot(float cameraXRot) {
        this.cameraXRot = cameraXRot;
    }

    public float getCameraYRot() {
        return cameraYRot;
    }

    public void setCameraYRot(float cameraYRot) {
        this.cameraYRot = cameraYRot;
    }
}
