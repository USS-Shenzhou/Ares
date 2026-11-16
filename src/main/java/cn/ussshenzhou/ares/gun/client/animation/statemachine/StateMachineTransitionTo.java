package cn.ussshenzhou.ares.gun.client.animation.statemachine;

import com.google.gson.annotations.SerializedName;

/// @author TACZ, USS_Shenzhou
public class StateMachineTransitionTo {
    @SerializedName("kind")
    public String kind = "state";

    @SerializedName("stateId")
    public String stateId;
}
