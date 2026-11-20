package cn.ussshenzhou.ares.client.animation.layer;

import com.google.gson.annotations.SerializedName;

/// @author TACZ, USS_Shenzhou
public class LayerBlendCfg {
    @SerializedName("mode")
    public String mode = "additive";

    @SerializedName("weight")
    public float weight = 1.0f;
}
