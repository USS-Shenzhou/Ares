package cn.ussshenzhou.ares.client.animation.statemachine;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/// @author TACZ, USS_Shenzhou
public class GunStateMachine {
    @SerializedName("entryStateId")
    public String entryStateId;

    /// > 状态机创建时执行一次的 Molang 动作，用于建立常驻轨道（如并行播放所有 movement 动画）。
    /// > 这些轨道在状态机生命周期内持续存在并被同步，状态只负责选择读取哪个轨道。
    @SerializedName("onInit")
    public String onInit;

    @SerializedName("states")
    public List<StateMachineState> states = List.of();

    @SerializedName("transitions")
    public List<StateMachineTransition> transitions = List.of();
}
