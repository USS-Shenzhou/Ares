package cn.ussshenzhou.ares.gun.custom.charm;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/// > 挂饰的数据驱动配置（data/charms/*_data.json），服务端权威并同步到客户端。
/// > <p>
/// > 仅承载「行为定义」：物理参数 + 模型 layout（部件骨骼名约定）。视觉相关（模型 / 贴图 / 渲染变换）
/// > 由客户端 display 资源承载，不在此处。
/// @author TACZ, USS_Shenzhou
public class CharmData {
    @SerializedName("physics")
    private Physics physics;

    @SerializedName("parts")
    private Parts parts;

    /// > 取物理参数，缺省返回默认值。
    public CharmPhysicsSettings getPhysics() {
        return physics == null ? CharmPhysicsSettings.DEFAULT : physics.toSettings();
    }

    /// > 取模型 layout，缺省返回 {@link CharmModelLayout#ROD_DOUBLE_RING}。
    public CharmModelLayout getLayout() {
        return parts == null ? CharmModelLayout.ROD_DOUBLE_RING : parts.toLayout();
    }

    public static class Physics {
        private float mass = CharmPhysicsSettings.DEFAULT.mass();
        @SerializedName("rope_length")
        private float ropeLength = CharmPhysicsSettings.DEFAULT.ropeLength();
        private float damping = CharmPhysicsSettings.DEFAULT.damping();
        private float stiffness = CharmPhysicsSettings.DEFAULT.stiffness();
        @SerializedName("solver_iterations")
        private int solverIterations = CharmPhysicsSettings.DEFAULT.solverIterations();
        @SerializedName("gravity_scale")
        private float gravityScale = CharmPhysicsSettings.DEFAULT.gravityScale();

        private CharmPhysicsSettings toSettings() {
            return new CharmPhysicsSettings(mass, ropeLength, damping, stiffness, solverIterations, gravityScale);
        }
    }

    public static class Parts {
        private String anchor = "gun_anchor";
        private String attachment = "attachment";
        @SerializedName("fixed_ring")
        private String fixedRing = "gun_ring";
        @SerializedName("dynamic_ring")
        private String dynamicRing = "charm_ring";
        private String body = "body";
        private List<String> collision = List.of();

        private CharmModelLayout toLayout() {
            return new CharmModelLayout(
                    anchor,
                    fixedRing,
                    dynamicRing,
                    List.of(attachment, fixedRing),
                    List.of(dynamicRing, body),
                    collision == null ? List.of() : collision
            );
        }
    }
}
