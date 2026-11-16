package cn.ussshenzhou.ares.client.animation.montage;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/// @author TACZ, USS_Shenzhou
public class Montage {
    @SerializedName("tracks")
    public List<MontageTrack> tracks = List.of();

    @SerializedName("sections")
    public List<MontageSection> sections = List.of();

    @SerializedName("notifyTracks")
    public List<MontageNotifyTrack> notifyTracks = List.of();
}
