package cn.ussshenzhou.ares.gun.client.animation.mounted;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.Identifier;

/// > 下挂组合配置（per-combo）：描述某把主枪 display 安装某个下挂时的 host overlay。
/// >
/// > 持枪姿态必须由作者显式声明（[#grip]）才会启用 host overlay；operation / transitions / grip_blend
/// > 也在同一个 combo 文件中声明，避免 profile 与 overlay 模板之间来回跳转。
/// @author TACZ, USS_Shenzhou
public class MountedCompatibilityProfile {
    @SerializedName("match")
    public Match match;

    /// > 持枪姿态（绝对姿态，per-combo 必填）。指向一个 bedrock 动画文件中的某个动画，预编译时绑定到 host 模型骨架。
    /// > 退化为单帧 idle 即可；其 lefthand/righthand/root 关键帧描述这把枪装该下挂时怎么握。
    @SerializedName("grip")
    public GripPose grip;

    @SerializedName("operation")
    public Operation operation;

    @SerializedName("grip_blend")
    public MountedOverlayTemplate.GripBlend gripBlend = new MountedOverlayTemplate.GripBlend();

    @SerializedName("transitions")
    public MountedOverlayTemplate.Transitions transitions;

    /// > 深度定制 override（整套替换 / 换状态机）。本轮仅预留字段，不实现具体生效逻辑。
    @SerializedName("override")
    public Identifier override;

    public static class Match {
        @SerializedName("host_display")
        public Identifier hostDisplay;

        @SerializedName("mounted")
        public Identifier mounted;
    }

    public static class GripPose {
        /// > 持枪姿态所在的 bedrock 动画文件 id。
        @SerializedName("animation_file")
        public Identifier animationFile;

        /// > 文件内的动画名（持枪姿态）。
        @SerializedName("animation")
        public String animation;
    }

    public static class Operation {
        @SerializedName("animation_file")
        public Identifier animationFile;
    }
}
