package cn.ussshenzhou.ares.client.animation.statemachine;

import com.google.gson.annotations.SerializedName;

/// @author TACZ, USS_Shenzhou
public class StateMachineTransitionFrom {
    @SerializedName("kind")
    public String kind = "state";

    @SerializedName("stateId")
    public String stateId;
}
