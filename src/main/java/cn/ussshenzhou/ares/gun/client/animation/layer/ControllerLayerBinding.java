package cn.ussshenzhou.ares.gun.client.animation.layer;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.Identifier;

/// @author TACZ, USS_Shenzhou
public class ControllerLayerBinding {
    @SerializedName("type")
    public String type;

    @SerializedName("machine")
    public Identifier machine;

    @SerializedName("blend")
    public LayerBlend blend = new LayerBlend();
}
