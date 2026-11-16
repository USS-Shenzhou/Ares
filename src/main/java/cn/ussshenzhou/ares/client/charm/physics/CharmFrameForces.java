package cn.ussshenzhou.ares.client.charm.physics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/// > 挂饰每帧外力换算（自源挂饰模组移植，仅依赖 JOML）。
/// >
/// > 把视角重力、相机加速度、锚点惯性等「视图空间」外力换算到锚点骨骼局部空间，喂给
/// > [CharmPhysicsState]。本期自由摆动主要用 [#viewPitchGravity]。
/// @author TACZ, USS_Shenzhou
public class CharmFrameForces {
    private static final float VISUAL_GRAVITY_SCALE = 3.0f;

    public static Vector3f localPositionToRender(Matrix4f pose, Vector3f localPosition) {
        Vector4f value = new Vector4f(localPosition.x, localPosition.y, localPosition.z, 1.0f);
        pose.transform(value);
        return new Vector3f(value.x, value.y, value.z);
    }

    public static Vector3f renderDirectionToLocal(Matrix4f pose, Vector3f renderDirection) {
        Vector3f local = renderDirectionToLocalWithoutScale(pose, renderDirection);
        if (finite(local)) {
            return local;
        }
        return renderVectorToLocalWithScale(pose, renderDirection);
    }

    private static Vector3f renderVectorToLocalWithScale(Matrix4f pose, Vector3f renderDirection) {
        Matrix4f inverse = new Matrix4f(pose);
        float determinant = inverse.determinant();
        if (!Float.isFinite(determinant) || Math.abs(determinant) < 1.0E-6f) {
            return new Vector3f(renderDirection);
        }
        inverse.invert();
        Vector4f value = new Vector4f(renderDirection.x, renderDirection.y, renderDirection.z, 0.0f);
        inverse.transform(value);
        return new Vector3f(value.x, value.y, value.z);
    }

    public static Vector3f capturedTotalAcceleration(Matrix4f pose, Vector3f renderGravity, Vector3f renderCameraAcceleration, Vector3f renderAnchorInertia) {
        Vector3f authoredForces = new Vector3f(renderGravity)
                .add(renderCameraAcceleration);
        return renderDirectionToLocal(pose, authoredForces)
                .add(renderVectorToLocalWithScale(pose, renderAnchorInertia));
    }

    public static Vector3f viewPitchGravity(float pitchDegrees, float gravityScale) {
        float gravity = 9.81f * VISUAL_GRAVITY_SCALE * Math.max(0.0f, gravityScale);
        float radians = (float) Math.toRadians(pitchDegrees);
        return new Vector3f(
                0.0f,
                -gravity * (float) Math.cos(radians),
                -gravity * (float) Math.sin(radians)
        );
    }

    private static Vector3f renderDirectionToLocalWithoutScale(Matrix4f pose, Vector3f renderDirection) {
        Vector3f localX = transformedDirection(pose, new Vector3f(1.0f, 0.0f, 0.0f));
        Vector3f localY = transformedDirection(pose, new Vector3f(0.0f, 1.0f, 0.0f));
        Vector3f localZ = transformedDirection(pose, new Vector3f(0.0f, 0.0f, 1.0f));
        if (!normalizeBasis(localX) || !normalizeBasis(localY) || !normalizeBasis(localZ)) {
            return new Vector3f(Float.NaN);
        }
        return new Vector3f(
                renderDirection.dot(localX),
                renderDirection.dot(localY),
                renderDirection.dot(localZ)
        );
    }

    private static Vector3f transformedDirection(Matrix4f pose, Vector3f direction) {
        Vector4f value = new Vector4f(direction.x, direction.y, direction.z, 0.0f);
        pose.transform(value);
        return new Vector3f(value.x, value.y, value.z);
    }

    private static boolean normalizeBasis(Vector3f basis) {
        if (!finite(basis) || basis.lengthSquared() < 1.0E-8f) {
            return false;
        }
        basis.normalize();
        return true;
    }

    private static boolean finite(Vector3f value) {
        return value != null && Float.isFinite(value.x) && Float.isFinite(value.y) && Float.isFinite(value.z);
    }
}
