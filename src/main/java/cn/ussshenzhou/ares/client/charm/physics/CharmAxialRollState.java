package cn.ussshenzhou.ares.client.charm.physics;

import cn.ussshenzhou.ares.client.charm.CharmLayerPlacement;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/// > 摆体轴向滚转状态（自源挂饰模组移植，仅依赖 JOML）。
/// >
/// > 在摆体主摆动之上叠加沿吊挂轴的滚转：除了把滚转角拉向可见目标外，还引入由偏心质量 + 局部加速度
/// > 计算的力矩（[#forceTorque]），让带配重的挂饰在甩动时产生自然的轴向旋转。
/// @author TACZ, USS_Shenzhou
public class CharmAxialRollState {
    private static final float MIN_VECTOR_LENGTH_SQUARED = 1.0E-8f;
    private static final float MAX_DELTA_SECONDS = 0.05f;
    private static final float TARGET_STIFFNESS = 5.8f;
    private static final float CENTER_STIFFNESS = 0.04f;
    private static final float ANGULAR_DAMPING = 0.13f;
    private static final float MAX_ANGULAR_SPEED = 24.0f;
    private static final float CONTACT_BIAS_GAIN = 3.4f;
    private static final float TARGET_GAIN = 0.34f;
    private static final float FORCE_TORQUE_GAIN = 0.58f;
    private static final float AXIS_RESTORING_GAIN = 1.6f;
    private static final float MAX_FORCE_TORQUE = 18.0f;
    private static final float TARGET_DEADBAND = 0.003f;

    private float angle;
    private float angularVelocity;
    private boolean initialized;

    public Quaternionf rotation(Vector3f pivotPosition, Vector3f restMassPosition, Vector3f currentMassPosition, Vector3f surfaceNormal, float deltaSeconds, float contactBias) {
        return rotation(pivotPosition, restMassPosition, currentMassPosition, surfaceNormal, deltaSeconds, contactBias, null, null);
    }

    public Quaternionf rotation(Vector3f pivotPosition, Vector3f restMassPosition, Vector3f currentMassPosition, Vector3f surfaceNormal, float deltaSeconds, float contactBias, Vector3f eccentricOffset, Vector3f localAcceleration) {
        float target = CharmLayerPlacement.hingeTwistAngle(pivotPosition, restMassPosition, currentMassPosition, surfaceNormal) * TARGET_GAIN;
        if (Float.isFinite(contactBias)) {
            target = wrapRadians(target + contactBias * CONTACT_BIAS_GAIN);
        }
        if (!Float.isFinite(target)) {
            return new Quaternionf();
        }

        if (!initialized) {
            angle = target;
            angularVelocity = 0.0f;
            initialized = true;
        } else {
            float dt = Math.max(0.001f, Math.min(deltaSeconds, MAX_DELTA_SECONDS));
            float targetDelta = wrapRadians(target - angle);
            if (Math.abs(targetDelta) < TARGET_DEADBAND) {
                targetDelta = 0.0f;
            }

            Vector3f axis = hingeAxis(pivotPosition, restMassPosition);
            float forceTorque = forceTorque(axis, eccentricOffset, localAcceleration);
            float angularAcceleration = targetDelta * TARGET_STIFFNESS
                    - angle * CENTER_STIFFNESS
                    - angularVelocity * ANGULAR_DAMPING
                    + forceTorque;
            angularVelocity = clamp(angularVelocity + angularAcceleration * dt, -MAX_ANGULAR_SPEED, MAX_ANGULAR_SPEED);
            angle = wrapRadians(angle + angularVelocity * dt);
        }

        Vector3f axis = hingeAxis(pivotPosition, restMassPosition);
        if (axis.lengthSquared() < MIN_VECTOR_LENGTH_SQUARED) {
            return new Quaternionf();
        }
        return new Quaternionf().fromAxisAngleRad(axis, angle);
    }

    private static float forceTorque(Vector3f axis, Vector3f eccentricOffset, Vector3f localAcceleration) {
        if (!finite(axis) || !finite(eccentricOffset) || !finite(localAcceleration)
                || axis.lengthSquared() < MIN_VECTOR_LENGTH_SQUARED
                || eccentricOffset.lengthSquared() < MIN_VECTOR_LENGTH_SQUARED
                || localAcceleration.lengthSquared() < MIN_VECTOR_LENGTH_SQUARED) {
            return 0.0f;
        }
        Vector3f normalizedAxis = new Vector3f(axis).normalize();
        Vector3f radialMass = new Vector3f(eccentricOffset).fma(-eccentricOffset.dot(normalizedAxis), normalizedAxis);
        if (radialMass.lengthSquared() < MIN_VECTOR_LENGTH_SQUARED) {
            return 0.0f;
        }
        Vector3f effectiveForce = new Vector3f(localAcceleration).negate();
        Vector3f radialForce = new Vector3f(effectiveForce).fma(-effectiveForce.dot(normalizedAxis), normalizedAxis);
        if (radialForce.lengthSquared() < MIN_VECTOR_LENGTH_SQUARED) {
            return 0.0f;
        }
        float torque = radialMass.cross(radialForce, new Vector3f()).dot(normalizedAxis) * FORCE_TORQUE_GAIN;
        float restoring = -radialMass.dot(radialForce.normalize()) * AXIS_RESTORING_GAIN;
        return clamp(torque + restoring, -MAX_FORCE_TORQUE, MAX_FORCE_TORQUE);
    }

    private static Vector3f hingeAxis(Vector3f pivotPosition, Vector3f restMassPosition) {
        if (!finite(pivotPosition) || !finite(restMassPosition)) {
            return new Vector3f();
        }
        Vector3f axis = new Vector3f(restMassPosition).sub(pivotPosition);
        if (axis.lengthSquared() < MIN_VECTOR_LENGTH_SQUARED) {
            return new Vector3f();
        }
        return axis.normalize();
    }

    private static float wrapRadians(float value) {
        float wrapped = value;
        while (wrapped > Math.PI) {
            wrapped -= (float) (Math.PI * 2.0);
        }
        while (wrapped < -Math.PI) {
            wrapped += (float) (Math.PI * 2.0);
        }
        return wrapped;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean finite(Vector3f value) {
        return value != null && Float.isFinite(value.x) && Float.isFinite(value.y) && Float.isFinite(value.z);
    }
}
