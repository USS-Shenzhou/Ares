package cn.ussshenzhou.ares.client.resource.display;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nullable;

/// @author TACZ, USS_Shenzhou
public class PoseLayerBlend {
    @SerializedName("mode")
    public String mode;

    @Nullable
    @SerializedName("weight")
    public Float weight;

    @Nullable
    @SerializedName("mask")
    public PoseLayerMask mask;
}
