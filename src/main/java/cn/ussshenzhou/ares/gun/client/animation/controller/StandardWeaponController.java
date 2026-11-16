package cn.ussshenzhou.ares.gun.client.animation.controller;

import com.google.gson.annotations.SerializedName;
import cn.ussshenzhou.ares.gun.client.animation.layer.ControllerLayerBinding;
import net.minecraft.resources.Identifier;

import java.util.Map;

/// @author TACZ, USS_Shenzhou
public class StandardWeaponController {
    @SerializedName("type")
    public Identifier type;

    /// > base 主状态机 id（draw/idle/reload/inspect/holster），驱动主姿态。
    // FIXME REVIEW rifle.json pointed base to the JS state machine ares:rifle/base_script; scripts are not ported, so it now
    // points to the JSON version ares:rifle/base.
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
