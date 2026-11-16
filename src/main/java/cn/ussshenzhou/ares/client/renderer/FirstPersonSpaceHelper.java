package cn.ussshenzhou.ares.client.renderer;

import cn.ussshenzhou.ares.client.handler.CameraEventHandler;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/// > 第一人称模型、视图和世界对齐空间之间的变换工具。
/// @author TACZ, USS_Shenzhou
public class FirstPersonSpaceHelper {
    /// > 将模型局部变换组合到第一人称视图基准中。
    public static Matrix4f toViewTransform(Matrix4f viewBase, Matrix4f localTransform) {
        return new Matrix4f(viewBase).mul(localTransform);
    }

    /// > 将配件挂点与其模型局部变换组合到第一人称视图基准中。
    public static Matrix4f toViewTransform(Matrix4f viewBase, Matrix4f mountTransform, Matrix4f localTransform) {
        return new Matrix4f(viewBase).mul(mountTransform).mul(localTransform);
    }

    /// > 从物品模型视图变换取得按世界 FOV 校正后的起点和前向量。
    public static ViewPose resolveWorldFovViewPose(Matrix4f viewTransform) {
        Vector3f position = viewTransform.getTranslation(new Vector3f());
        applyWorldFovDepthCorrection(position);

        Vector3f forward = viewTransform.transformDirection(new Vector3f(0.0f, 0.0f, -1.0f));
        if (forward.lengthSquared() > 1.0e-6f) {
            forward.normalize();
        } else {
            forward.set(0.0f, 0.0f, -1.0f);
        }
        return new ViewPose(position, forward);
    }

    /// > 构造动画粒子所需的第一人称空间转换器。
    public static ParticleSpace createParticleSpace(Matrix4f renderBasePose, Camera camera, float cameraRollRadians) {
        Matrix4f renderBaseInverse = new Matrix4f(renderBasePose).invert();
        Matrix4f cameraRotationInverse = new Matrix4f()
                .rotationX((float) Math.toRadians(camera.xRot()))
                .rotateY((float) Math.toRadians(camera.yRot() + 180f))
                .rotateZ(cameraRollRadians)
                .invert();
        return new ParticleSpace(renderBaseInverse, cameraRotationInverse);
    }

    private static void applyWorldFovDepthCorrection(Vector3f position) {
        double itemRenderFov = CameraEventHandler.ITEM_MODEL_FOV_DYNAMICS.get();
        double levelRenderFov = CameraEventHandler.WORLD_FOV_DYNAMICS.get();
        if (itemRenderFov > 0.0 && levelRenderFov > 0.0) {
            position.z *= (float) (Math.tan(itemRenderFov / 2.0 * Math.PI / 180.0)
                    / Math.tan(levelRenderFov / 2.0 * Math.PI / 180.0));
        }
    }

    public record ViewPose(Vector3f position, Vector3f forward) {
    }

    /// > 固定在同一第一人称渲染帧内的粒子空间映射。
    public static class ParticleSpace {
        private final Matrix4f renderBaseInverse;
        private final Matrix4f cameraRotationInverse;

        private ParticleSpace(Matrix4f renderBaseInverse, Matrix4f cameraRotationInverse) {
            this.renderBaseInverse = renderBaseInverse;
            this.cameraRotationInverse = cameraRotationInverse;
        }

        public Matrix4f toEmitterTransform(Matrix4f viewTransform) {
            return new Matrix4f(renderBaseInverse).mul(viewTransform);
        }

        public Matrix4f toWorldTransform(Matrix4f viewTransform) {
            return new Matrix4f(cameraRotationInverse).mul(viewTransform);
        }
    }
}
