package cn.ussshenzhou.ares.gun.client.animation.statemachine;

import com.google.gson.annotations.SerializedName;

/// @author TACZ, USS_Shenzhou
public class StateMachineState {
    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    /// > 进入状态时执行的 Molang 动作（如 {@code "query.play_loop('primary','idle')"}）。
    @SerializedName("onEnter")
    public String onEnter;

    /// > 状态激活期间每 tick 执行的 Molang 动作（如 {@code "query.set_locomotion_phase('primary',2.0,1.0)"}）。
    @SerializedName("onUpdate")
    public String onUpdate;

    /// > 离开状态时执行的 Molang 动作。
    @SerializedName("onExit")
    public String onExit;

    /// > 该状态输出姿态的来源轨道名。
    @SerializedName("pose")
    public StateMachinePose pose;
}
