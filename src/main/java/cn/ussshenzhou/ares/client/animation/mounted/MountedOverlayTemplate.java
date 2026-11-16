package cn.ussshenzhou.ares.client.animation.mounted;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.Identifier;

import java.util.List;

/// > Host overlay 组合配置中复用的结构类型。
/// @author TACZ, USS_Shenzhou
public class MountedOverlayTemplate {
    public static class Transitions {
        /// > 切到下挂模式的过渡段（相对持枪姿态的差量动画）。
        @SerializedName("to_under_barrel")
        public TransitionClip toUnderBarrel;

        /// > 切回主枪模式的过渡段。
        @SerializedName("to_primary")
        public TransitionClip toPrimary;
    }

    public static class TransitionClip {
        /// > 过渡段所在的 bedrock 动画文件 id。
        @SerializedName("animation_file")
        public Identifier animationFile;

        /// > 文件内的动画名。
        @SerializedName("animation")
        public String animation;
    }

    public static class GripBlend {
        /// > 混合模式：additive / layered。
        @SerializedName("mode")
        public String mode = "layered";

        /// > per-hand 权重分组。每组以一个根骨骼 + 后代深度建控制点；未被任何组覆盖的骨骼保持 host 原姿态。
        @SerializedName("groups")
        public List<GripGroup> groups = List.of();
    }

    public static class GripGroup {
        /// > 控制点根骨骼名（持枪姿态仅作用于该骨骼及其后代）。如 lefthand / righthand / root。
        @SerializedName("root_bone")
        public String rootBone;

        /// > 后代深度。
        @SerializedName("depth")
        public int depth = 16;

        /// > 本组权重（多态：数字常数 / 关键帧曲线对象 / Molang 字符串）。
        @SerializedName("weight")
        public OverlayWeightSpec weight = OverlayWeightSpec.constant(1.0f);
    }
}
