package cn.ussshenzhou.ares.gun.custom.gun;

import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.JsonAdapter;
import cn.ussshenzhou.ares.gun.attribute.GunAttributeModifier;
import cn.ussshenzhou.ares.gun.resource.serialize.AttributeIdMapDeserializer;
import net.minecraft.resources.Identifier;

import java.util.Map;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW removed field: hooks.
public class AmmoEntry {
    @SerializedName("type")
    private Identifier type;

    @SerializedName("modifiers")
    @JsonAdapter(AttributeIdMapDeserializer.class)
    private Map<Identifier, GunAttributeModifier> modifiers = Map.of();

    public Identifier getType() {
        return type;
    }

    public Map<Identifier, GunAttributeModifier> getModifiers() {
        return modifiers;
    }
}
