package cn.ussshenzhou.ares.client.animation.montage;

import com.google.gson.annotations.SerializedName;
import cn.ussshenzhou.ares.client.resource.display.PoseLayerBlend;

import java.util.List;

/// @author TACZ, USS_Shenzhou
public class MontageTrack {
    @SerializedName("name")
    public String name;

    @SerializedName("blend")
    public PoseLayerBlend blend;

    @SerializedName("additive")
    public boolean additive = false;

    @SerializedName("enabled")
    public boolean enabled = true;

    @SerializedName("segments")
    public List<MontageSegment> segments = List.of();
}
