package cn.ussshenzhou.ares.client.resource.display;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/// @author TACZ, USS_Shenzhou
public class PoseLayerMask {
    @SerializedName("control_points")
    public List<PoseLayerMaskControlPoint> controlPoints;
}
