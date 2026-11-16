package cn.ussshenzhou.ares.gun.client.animation.montage;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW the Definition suffix is dropped from MontageDefinition, MontageNotifyKeyDefinition, MontageNotifyTrackDefinition,
// MontageSectionDefinition, MontageSegmentDefinition, MontageTrackDefinition, GunStateMachineDefinition,
// StateMachinePoseDefinition, StateMachineStateDefinition, StateMachineTransitionDefinition, LayerBlendDefinition and
// StandardWeaponControllerDefinition.
public class Montage {
    @SerializedName("tracks")
    public List<MontageTrack> tracks = List.of();

    @SerializedName("sections")
    public List<MontageSection> sections = List.of();

    @SerializedName("notifyTracks")
    public List<MontageNotifyTrack> notifyTracks = List.of();
}
