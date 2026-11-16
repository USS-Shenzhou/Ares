package cn.ussshenzhou.ares.client.animation.montage;

import com.google.gson.annotations.SerializedName;

/// @author TACZ, USS_Shenzhou
public class MontageSection {
    @SerializedName("name")
    public String name;

    @SerializedName("startTime")
    public float startTime = 0.0f;

    @SerializedName("endTime")
    public float endTime = 0.0f;

    @SerializedName("nextSectionName")
    public String nextSectionName;
}
