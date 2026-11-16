package cn.ussshenzhou.ares.gun.client.renderer.entity;

/* FIXME rendering
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.client.resource.ClientAssetsManager;
import cn.ussshenzhou.ares.gun.client.resource.display.GunDisplayInstance;
import cn.ussshenzhou.ares.gun.entity.BulletEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nonnull;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/// @author TACZ, USS_Shenzhou
public class BulletEntityRenderer extends EntityRenderer<BulletEntity> {
    private static final Identifier TRACER_TEXTURE = Ares.modLoc("textures/entity/tracer.png");

    private static final float BASE_WIDTH = 0.005f;
    private static final float MAX_TRAIL_LENGTH_RATIO = 0.85f;
    private static final float MAX_TRAIL_TO_SHOOTER_RATIO = 0.8f;
    private static final float OFFSET_FADE_RANGE = 128.0f;

    private static final float[] DEFAULT_COLOR = {1.0f, 0.8f, 0.3f};

    public BulletEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@Nonnull BulletEntity bullet, float entityYaw, float partialTicks,
                       @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource bufferSource, int packedLight) {
/// >        if (!bullet.isTracer()) return;

        if (bullet.tickCount < 1) {
            return;
        }

        Entity shooter = bullet.getOwner();
        if (shooter == null) {
            return;
        }

        boolean isFirstPerson = this.entityRenderDispatcher.options.getCameraType().isFirstPerson()
                && shooter instanceof LocalPlayer;

        Vec3 bulletPos = bullet.getPosition(partialTicks);
        double disToEye = bulletPos.distanceTo(shooter.getEyePosition(partialTicks));

        Vec3 motion = bullet.getDeltaMovement();
        if (motion.lengthSqr() < 1e-8) {
            return;
        }

        double trailLength = MAX_TRAIL_LENGTH_RATIO * motion.length();
        trailLength = Math.min(trailLength, disToEye * MAX_TRAIL_TO_SHOOTER_RATIO);
        if (trailLength < 0.01) {
            return;
        }

        /// > 距离两格外才渲染（前 5 tick 内判定）
        double bulletDistance = bulletPos.distanceTo(shooter.getEyePosition());
        if (bullet.tickCount < 5 && bulletDistance <= 2) {
            return;
        }

        float[] color = resolveTracerColor(bullet);
        float width = BASE_WIDTH;
        width *= (float) Math.max(1.0, disToEye / 3.5);

        poseStack.pushPose();
        {
            if (isFirstPerson) {
                applyFirstPersonOffset(poseStack, bullet, disToEye, partialTicks);
            }

            /// > 旋转到子弹飞行方向
            poseStack.mulPose(Axis.YP.rotationDegrees(
                    Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot()) - 180.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(
                    Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot())));

            /// > 平移到轨迹中点，沿 Z 轴拉伸
            poseStack.translate(0, isFirstPerson ? 0 : -0.015, trailLength / 2.0);

            /// > 渲染十字形发光四边形
            renderTracerQuads(poseStack, bufferSource, color, width, (float) trailLength);
        }
        poseStack.popPose();
    }

    private void renderTracerQuads(PoseStack poseStack, MultiBufferSource bufferSource,
                                   float[] color, float halfWidth, float trailLength) {
        RenderType renderType = RenderType.entityTranslucentEmissive(TRACER_TEXTURE);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        int r = (int) (color[0] * 255);
        int g = (int) (color[1] * 255);
        int b = (int) (color[2] * 255);
        int aHead = 230;
        int aTail = 40;

        float halfLen = trailLength / 2.0f;

        /// > 水平面四边形
        consumer.vertex(pose, -halfWidth, 0, halfLen).color(r, g, b, aHead)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(pose, halfWidth, 0, halfLen).color(r, g, b, aHead)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(pose, halfWidth, 0, -halfLen).color(r, g, b, aTail)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(pose, -halfWidth, 0, -halfLen).color(r, g, b, aTail)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(normal, 0, 1, 0).endVertex();

        /// > 垂直面四边形（十字形）
        consumer.vertex(pose, 0, -halfWidth, halfLen).color(r, g, b, aHead)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(normal, 1, 0, 0).endVertex();
        consumer.vertex(pose, 0, halfWidth, halfLen).color(r, g, b, aHead)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(normal, 1, 0, 0).endVertex();
        consumer.vertex(pose, 0, halfWidth, -halfLen).color(r, g, b, aTail)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(normal, 1, 0, 0).endVertex();
        consumer.vertex(pose, 0, -halfWidth, -halfLen).color(r, g, b, aTail)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(normal, 1, 0, 0).endVertex();
    }

    private void applyFirstPersonOffset(PoseStack poseStack, BulletEntity bullet,
                                        double disToEye, float partialTicks) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3f offset = bullet.getFirstPersonRenderOffset();

        if (offset == null) {
            Vector3f muzzleOffset = FirstPersonPoseCache.getMuzzleOffset(
                    bullet.getShooterHand(), bullet.getMuzzleReference());
            if (muzzleOffset != null) {
                offset = new Vector3f(muzzleOffset);
                bullet.setFirstPersonRenderOffset(offset);
                bullet.setCameraXRot(camera.getXRot());
                bullet.setCameraYRot(camera.getYRot());
            } else {
                return;
            }
        }

        /// > 按距离线性衰减偏移
        double offsetReducer = Math.max(0, (256 - disToEye)) / 256;
        if (offsetReducer < 0.001) {
            return;
        }

        /// > 使用子弹生成时缓存的摄像机旋转来变换偏移
        /// > 这样即使玩家移动/转向，偏移方向也保持一致，不会抖动
        poseStack.mulPose(Axis.YN.rotationDegrees(bullet.getCameraYRot() + 180f));
        poseStack.mulPose(Axis.XN.rotationDegrees(bullet.getCameraXRot()));
        poseStack.translate(
                offset.x() * offsetReducer,
                offset.y() * offsetReducer,
                offset.z() * offsetReducer
        );
        poseStack.mulPose(Axis.XP.rotationDegrees(bullet.getCameraXRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(bullet.getCameraYRot() + 180f));
    }

    private float[] resolveTracerColor(BulletEntity bullet) {
        Identifier gunId = bullet.getGunId();
        if (gunId != null) {
            GunDisplayInstance display = ClientAssetsManager.INSTANCE.getGunDisplay(gunId).orElse(null);
            if (display != null) {
                float[] color = display.getTracerColor();
                if (color != null) {
                    return color;
                }
            }
        }
        return DEFAULT_COLOR;
    }

    @Override
    protected int getBlockLightLevel(@Nonnull BulletEntity bullet, @Nonnull BlockPos blockPos) {
        return 15;
    }

    @Override
    @Nonnull
    public Identifier getTextureLocation(@Nonnull BulletEntity entity) {
        return TRACER_TEXTURE;
    }

    @Override
    public boolean shouldRender(@Nonnull BulletEntity bullet, @Nonnull Frustum camera,
                                double pCamX, double pCamY, double pCamZ) {
        AABB aabb = bullet.getBoundingBoxForCulling().inflate(0.5);
        if (aabb.hasNaN() || aabb.getSize() == 0) {
            aabb = new AABB(
                    bullet.getX() - 2.0, bullet.getY() - 2.0, bullet.getZ() - 2.0,
                    bullet.getX() + 2.0, bullet.getY() + 2.0, bullet.getZ() + 2.0
            );
        }
        return camera.isVisible(aabb);
    }
}
*/
