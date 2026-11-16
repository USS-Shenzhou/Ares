package cn.ussshenzhou.ares.gun.custom.charm;

/// > 挂饰物理参数（端无关的纯数据）。
/// >
/// > 物理虽然只在客户端表现，但参数属于数据驱动内容，随 [CharmData] 一起加载/同步。
/// > 构造时做范围钳制，保证数值落在合理区间。
/// @author TACZ, USS_Shenzhou
public record CharmPhysicsSettings(
        float mass,
        float ropeLength,
        float damping,
        float stiffness,
        int solverIterations,
        float gravityScale
) {
    public static final CharmPhysicsSettings DEFAULT = new CharmPhysicsSettings(0.08f, 0.18f, 0.16f, 0.92f, 6, 1.0f);

    public CharmPhysicsSettings {
        mass = Math.max(0.001f, mass);
        ropeLength = Math.max(0.001f, ropeLength);
        damping = clamp(damping, 0.0f, 0.98f);
        stiffness = clamp(stiffness, 0.0f, 1.0f);
        solverIterations = Math.max(1, solverIterations);
        gravityScale = Math.max(0.0f, gravityScale);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
