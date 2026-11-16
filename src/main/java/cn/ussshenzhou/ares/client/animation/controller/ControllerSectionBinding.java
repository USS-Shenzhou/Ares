package cn.ussshenzhou.ares.client.animation.controller;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.Identifier;

/// @author TACZ, USS_Shenzhou
public class ControllerSectionBinding {
    @SerializedName("slot")
    public String slot;

    @SerializedName("montage")
    public Identifier montage;

    @SerializedName("section")
    public String section = "main";

    /// > action 槽位播放结束后回到底层姿态（idle/movement）的淡出时长（秒）。
    /// > 为 null 时使用 controller 的默认值。仅对 action 槽位生效。
    @SerializedName("blendOutSeconds")
    public Float blendOutSeconds;
}
