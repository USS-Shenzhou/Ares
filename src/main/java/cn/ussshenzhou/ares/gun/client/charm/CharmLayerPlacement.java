package cn.ussshenzhou.ares.gun.client.charm;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/// > 挂饰分层摆动的几何放置与朝向数学（自源挂饰模组移植，仅依赖 JOML）。
/// >
/// > 把锚点（命中点）与挂饰模型各 layout 骨骼的 pivot 偏移，换算成「模型根 / 固定枢轴 / 动态枢轴 /
/// > 摆体质心」在锚点骨骼局部空间的静止位置；并提供主摆动（swing）与吊环扭转（hinge twist）旋转。
/// @author TACZ, USS_Shenzhou
public class CharmLayerPlacement {
    private static final float MIN_SWING_VECTOR_LENGTH_SQUARED = 1.0E-8f;
    private static final float OPPOSITE_DOT_THRESHOLD = -0.985f;
    private static final float HINGE_TWIST_GAIN = 0.72f;
    private static final float MAX_HINGE_TWIST_RADIANS = 1.35f;

    private final Vector3f modelRootPosition;
    private final Vector3f fixedPivotPosition;
    private final Vector3f dynamicPivotRestPosition;
    private final Vector3f massRestPosition;

    private CharmLayerPlacement(Vector3f modelRootPosition, Vector3f fixedPivotPosition, Vector3f dynamicPivotRestPosition, Vector3f massRestPosition) {
        this.modelRootPosition = new Vector3f(modelRootPosition);
        this.fixedPivotPosition = new Vector3f(fixedPivotPosition);
        this.dynamicPivotRestPosition = new Vector3f(dynamicPivotRestPosition);
        this.massRestPosition = new Vector3f(massRestPosition);
    }

    public static CharmLayerPlacement fromAnchor(Vector3f anchorPosition, Vector3f anchorOffset, Vector3f fixedPivotOffset, Vector3f dynamicPivotOffset, Vector3f massOffset) {
        Vector3f root = new Vector3f(anchorPosition).sub(anchorOffset);
        return new CharmLayerPlacement(
                root,
                new Vector3f(root).add(fixedPivotOffset),
                new Vector3f(root).add(dynamicPivotOffset),
                new Vector3f(root).add(massOffset)
        );
    }

    public static Quaternionf swingRotation(Vector3f pivotPosition, Vector3f restMassPosition, Vector3f currentMassPosition) {
        Vector3f rest = new Vector3f(restMassPosition).sub(pivotPosition);
        Vector3f current = new Vector3f(currentMassPosition).sub(pivotPosition);
        if (!finite(rest) || !finite(current)
                || rest.lengthSquared() < MIN_SWING_VECTOR_LENGTH_SQUARED
                || current.lengthSquared() < MIN_SWING_VECTOR_LENGTH_SQUARED) {
            return new Quaternionf();
        }
        Vector3f restDirection = rest.normalize();
        Vector3f currentDirection = current.normalize();
        float dot = restDirection.dot(currentDirection);
        if (dot < OPPOSITE_DOT_THRESHOLD) {
            float angle = (float) Math.acos(clamp(dot, -1.0f, 1.0f));
            return new Quaternionf().fromAxisAngleRad(stablePerpendicularAxis(restDirection), angle);
        }
        return new Quaternionf().rotationTo(restDirection, currentDirection);
    }

    public static Quaternionf hingeTwistRotation(Vector3f pivotPosition, Vector3f restMassPosition, Vector3f currentMassPosition, Vector3f surfaceNormal) {
        float angle = hingeTwistAngle(pivotPosition, restMassPosition, currentMassPosition, surfaceNormal);
        if (!Float.isFinite(angle)) {
            return new Quaternionf();
        }
        Vector3f rest = new Vector3f(restMassPosition).sub(pivotPosition);
        if (!finite(rest) || rest.lengthSquared() < MIN_SWING_VECTOR_LENGTH_SQUARED) {
            return new Quaternionf();
        }
        return new Quaternionf().fromAxisAngleRad(rest.normalize(), angle);
    }

    public static float hingeTwistAngle(Vector3f pivotPosition, Vector3f restMassPosition, Vector3f currentMassPosition, Vector3f surfaceNormal) {
        Vector3f rest = new Vector3f(restMassPosition).sub(pivotPosition);
        Vector3f current = new Vector3f(currentMassPosition).sub(pivotPosition);
        if (!finite(rest) || !finite(current)
                || rest.lengthSquared() < MIN_SWING_VECTOR_LENGTH_SQUARED
                || current.lengthSquared() < MIN_SWING_VECTOR_LENGTH_SQUARED) {
            return 0.0f;
        }
        Vector3f restDirection = rest.normalize();
        Vector3f currentDirection = current.normalize();
        Vector3f swingPlaneNormal = restDirection.cross(currentDirection, new Vector3f());
        if (swingPlaneNormal.lengthSquared() < MIN_SWING_VECTOR_LENGTH_SQUARED) {
            return 0.0f;
        }
        swingPlaneNormal.normalize();

        Vector3f reference = twistReference(restDirection, surfaceNormal);
        Vector3f perpendicular = restDirection.cross(reference, new Vector3f()).normalize();
        float angle = (float) Math.atan2(swingPlaneNormal.dot(perpendicular), swingPlaneNormal.dot(reference));
        return clamp(angle * HINGE_TWIST_GAIN, -MAX_HINGE_TWIST_RADIANS, MAX_HINGE_TWIST_RADIANS);
    }

    public static Matrix4f dynamicLayerTransform(Vector3f fixedPivotPosition, Vector3f rawFixedPivotOffset, Vector3f rawDynamicPivotOffset, Quaternionf swingRotation, Quaternionf surfaceOrientation, Quaternionf renderRotation, Quaternionf dynamicRotation) {
        Vector3f dynamicDelta = new Vector3f(rawDynamicPivotOffset).sub(rawFixedPivotOffset);
        return new Matrix4f()
                .translate(fixedPivotPosition)
                .rotate(safe(swingRotation))
                .rotate(safe(surfaceOrientation))
                .rotate(safe(renderRotation))
                .translate(dynamicDelta)
                .rotate(safe(dynamicRotation))
                .translate(new Vector3f(rawDynamicPivotOffset).negate());
    }

    public Vector3f modelRootPosition() {
        return new Vector3f(modelRootPosition);
    }

    public Vector3f fixedPivotPosition() {
        return new Vector3f(fixedPivotPosition);
    }

    public Vector3f dynamicPivotRestPosition() {
        return new Vector3f(dynamicPivotRestPosition);
    }

    public Vector3f massRestPosition() {
        return new Vector3f(massRestPosition);
    }

    private static boolean finite(Vector3f value) {
        return value != null && Float.isFinite(value.x) && Float.isFinite(value.y) && Float.isFinite(value.z);
    }

    private static Vector3f stablePerpendicularAxis(Vector3f direction) {
        Vector3f basis = Math.abs(direction.y) < 0.9f ? new Vector3f(0.0f, 1.0f, 0.0f) : new Vector3f(1.0f, 0.0f, 0.0f);
        Vector3f axis = direction.cross(basis, new Vector3f());
        if (axis.lengthSquared() < MIN_SWING_VECTOR_LENGTH_SQUARED) {
            return new Vector3f(1.0f, 0.0f, 0.0f);
        }
        return axis.normalize();
    }

    private static Vector3f twistReference(Vector3f restDirection, Vector3f surfaceNormal) {
        Vector3f reference = finite(surfaceNormal) && surfaceNormal.lengthSquared() > MIN_SWING_VECTOR_LENGTH_SQUARED
                ? new Vector3f(surfaceNormal).normalize()
                : stablePerpendicularAxis(restDirection);
        reference.fma(-reference.dot(restDirection), restDirection);
        if (reference.lengthSquared() < MIN_SWING_VECTOR_LENGTH_SQUARED) {
            return stablePerpendicularAxis(restDirection);
        }
        return reference.normalize();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Quaternionf safe(Quaternionf rotation) {
        if (rotation == null || !Float.isFinite(rotation.x) || !Float.isFinite(rotation.y) || !Float.isFinite(rotation.z) || !Float.isFinite(rotation.w)) {
            return new Quaternionf();
        }
        return new Quaternionf(rotation);
    }
}
