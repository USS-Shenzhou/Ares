package cn.ussshenzhou.ares.client.animation.controller;

import com.google.gson.annotations.SerializedName;
import cn.ussshenzhou.ares.client.animation.layer.ControllerLayerBinding;
import net.minecraft.resources.Identifier;

import java.util.Map;

/// @author TACZ, USS_Shenzhou
public class StandardWeaponControlData {
    @SerializedName("type")
    public Identifier type;

    /// > base 主状态机 id（draw/idle/reload/inspect/holster），驱动主姿态。
    @SerializedName("base")
    public Identifier base;

    @SerializedName("montages")
    public Map<String, Identifier> montages = Map.of();

    @SerializedName("sections")
    public Map<String, ControllerSectionBinding> sections = Map.of();

    @SerializedName("actions")
    public Map<String, ControllerSectionBinding> actions = Map.of();

    @SerializedName("layers")
    public Map<String, ControllerLayerBinding> layers = Map.of();
}
