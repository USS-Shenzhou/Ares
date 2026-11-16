package cn.ussshenzhou.ares.client.animation.montage;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/// @author TACZ, USS_Shenzhou
public class MontageNotifyTrack {
    @SerializedName("name")
    public String name;

    @SerializedName("keys")
    public List<MontageNotifyKey> keys = List.of();
}
