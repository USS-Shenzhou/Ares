package cn.ussshenzhou.ares.client.resource.display;

import com.google.gson.annotations.SerializedName;

/// @author TACZ, USS_Shenzhou
public class PoseLayerMaskControlPoint {
    @SerializedName("bone")
    public String bone;

    @SerializedName("depth")
    public int depth = 0;

    @SerializedName("weight")
    public float weight = 1.0f;
}
