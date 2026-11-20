package cn.ussshenzhou.ares.client.animation.montage;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/// @author TACZ, USS_Shenzhou
public class MontageCfg {
    @SerializedName("tracks")
    public List<MontageTrackCfg> tracks = List.of();

    @SerializedName("sections")
    public List<MontageSectionCfg> sections = List.of();

    @SerializedName("notifyTracks")
    public List<MontageNotifyTrackCfg> notifyTracks = List.of();
}
