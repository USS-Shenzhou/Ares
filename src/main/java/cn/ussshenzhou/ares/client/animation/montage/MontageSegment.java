package cn.ussshenzhou.ares.client.animation.montage;

import com.google.gson.annotations.SerializedName;

/// @author TACZ, USS_Shenzhou
public class MontageSegment {
    @SerializedName("startTime")
    public float startTime = 0.0f;

    @SerializedName("animation")
    public String animation;

    @SerializedName("sourceStartTime")
    public float sourceStartTime = 0.0f;

    @SerializedName("sourceEndTime")
    public Float sourceEndTime;
}
