package cn.ussshenzhou.ares.client.animation.layer;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.Identifier;

/// @author TACZ, USS_Shenzhou
public class ControllerLayerBindingCfg {
    @SerializedName("type")
    public String type;

    @SerializedName("machine")
    public Identifier machine;

    @SerializedName("blend")
    public LayerBlendCfg blend = new LayerBlendCfg();
}
