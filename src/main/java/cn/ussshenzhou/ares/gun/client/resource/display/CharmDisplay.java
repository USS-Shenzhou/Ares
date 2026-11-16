package cn.ussshenzhou.ares.gun.client.resource.display;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

/// > 挂饰 display 配置（display/charms/*.json），纯客户端资源。
/// >
/// > 承载视觉定义：模型 / 贴图 / 渲染变换。物理与 layout 在数据驱动的 CharmData 中。
/// @author TACZ, USS_Shenzhou
public class CharmDisplay {
    @SerializedName("model")
    public Identifier modelLocation;

    @SerializedName("texture")
    public Identifier modelTexture;

    @Nullable
    @SerializedName("render")
    public Render render;

    /// > 渲染变换参数。
    public static class Render {
        @Nullable
        @SerializedName("scale")
        public float[] scale;

        @Nullable
        @SerializedName("rotation")
        public float[] rotation;

        @Nullable
        @SerializedName("dynamic_rotation")
        public float[] dynamicRotation;

        @Nullable
        @SerializedName("tint")
        public float[] tint;

        @Nullable
        @SerializedName("forward_axis")
        public float[] forwardAxis;

        @Nullable
        @SerializedName("anchor_clearance_pixels")
        public Float anchorClearancePixels;
    }
}
