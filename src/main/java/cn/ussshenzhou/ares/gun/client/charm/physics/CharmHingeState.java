package cn.ussshenzhou.ares.gun.client.charm.physics;

import cn.ussshenzhou.ares.gun.client.charm.CharmLayerPlacement;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/// > 吊环铰链扭转状态（自源挂饰模组移植，仅依赖 JOML）。
/// > <p>
/// > 在主摆动之上叠加一层「松动吊环」的扭转：用弱弹簧把扭转角拉向可见目标角，同时保留角动量，
/// > 让吊环随挂饰摆动而轻微旋转。
/// @author TACZ, USS_Shenzhou
public class CharmHingeState {
    private static final float MIN_VECTOR_LENGTH_SQUARED = 1.0E-8f;
    private static final float MAX_DELTA_SECONDS = 0.05f;
    private static final float CENTER_STIFFNESS = 1.35f;
    private static final float TARGET_STIFFNESS = 1.65f;
    private static final float ANGULAR_DAMPING = 0.58f;
    private static final float MAX_ANGULAR_SPEED = 13.0f;
    private static final float MAX_HINGE_ANGLE = 2.35f;
    private static final float TARGET_DEADBAND = 0.018f;

    private float angle;
    private float angularVelocity;
    private boolean initialized;

    public Quaternionf rotation(Vector3f pivotPosition, Vector3f restMassPosition, Vector3f currentMassPosition, Vector3f surfaceNormal, float deltaSeconds) {
        return rotation(pivotPosition, restMassPosition, currentMassPosition, surfaceNormal, deltaSeconds, 0.0f);
    }

    public Quaternionf rotation(Vector3f pivotPosition, Vector3f restMassPosition, Vector3f currentMassPosition, Vector3f surfaceNormal, float deltaSeconds, float contactBias) {
        float target = CharmLayerPlacement.hingeTwistAngle(pivotPosition, restMassPosition, currentMassPosition, surfaceNormal);
        if (!Float.isFinite(target)) {
            return new Quaternionf();
        }
        if (Float.isFinite(contactBias)) {
            target = wrapRadians(target + contactBias);
        }
        if (!initialized) {
            angle = 0.0f;
            angularVelocity = 0.0f;
            initialized = true;
        } else {
            float dt = Math.max(0.001f, Math.min(deltaSeconds, MAX_DELTA_SECONDS));
            float targetDelta = wrapRadians(target - angle);
            if (Math.abs(targetDelta) < TARGET_DEADBAND) {
                targetDelta = 0.0f;
            }

            /// > 松动吊环应携带角动量；只用弱弹簧把它轻推向可见目标角。
            float angularAcceleration = targetDelta * TARGET_STIFFNESS
                    - angle * CENTER_STIFFNESS
                    - angularVelocity * ANGULAR_DAMPING;
            angularVelocity = clamp(angularVelocity + angularAcceleration * dt, -MAX_ANGULAR_SPEED, MAX_ANGULAR_SPEED);
            angle = clamp(wrapRadians(angle + angularVelocity * dt), -MAX_HINGE_ANGLE, MAX_HINGE_ANGLE);
        }

        Vector3f axis = hingeAxis(pivotPosition, restMassPosition);
        if (axis.lengthSquared() < MIN_VECTOR_LENGTH_SQUARED) {
            return new Quaternionf();
        }
        return new Quaternionf().fromAxisAngleRad(axis, angle);
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
