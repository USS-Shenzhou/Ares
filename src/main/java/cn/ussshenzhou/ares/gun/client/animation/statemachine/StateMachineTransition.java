package cn.ussshenzhou.ares.gun.client.animation.statemachine;

import com.google.gson.annotations.SerializedName;

/// @author TACZ, USS_Shenzhou
public class StateMachineTransition {
    @SerializedName("id")
    public String id;

    @SerializedName("from")
    public StateMachineTransitionFrom from;

    @SerializedName("to")
    public StateMachineTransitionTo to;

    @SerializedName("condition")
    public String condition;

    /// > 过渡触发瞬间执行的 Molang 动作（在进入目标状态之前），
    /// > 用于提前启动目标动画，例如 {@code "query.play_once('primary','run_start')"}。
    @SerializedName("afterTrigger")
    public String afterTrigger;

    @SerializedName("duration")
    public float duration;

    @SerializedName("easing")
    public String easing = "LINEAR";
}
