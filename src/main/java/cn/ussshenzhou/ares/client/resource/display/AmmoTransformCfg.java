package cn.ussshenzhou.ares.client.resource.display;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nullable;

/// > 弹药各渲染上下文的缩放变换。
/// >
/// > 字段名沿用 legacy（`fixed` / `ground` / `thirdperson`）以兼容旧 display JSON。
/// > 各值为 `[x, y, z]` 三元组，缺省时由 [AmmoDisplay]
/// > 回退到默认缩放。
/// @author TACZ, USS_Shenzhou
public class AmmoTransformCfg {
    @Nullable
    @SerializedName("scale")
    public ScaleCfg scale = null;

    public static class ScaleCfg {
        @Nullable
        @SerializedName("fixed")
        public float[] fixed = null;

        @Nullable
        @SerializedName("ground")
        public float[] ground = null;

        @Nullable
        @SerializedName("thirdperson")
        public float[] thirdperson = null;
    }
}
