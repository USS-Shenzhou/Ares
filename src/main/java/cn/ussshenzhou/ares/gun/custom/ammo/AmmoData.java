package cn.ussshenzhou.ares.gun.custom.ammo;

import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.JsonAdapter;
import cn.ussshenzhou.ares.gun.attribute.GunAttributeModifier;
import cn.ussshenzhou.ares.gun.resource.serialize.AttributeIdMapDeserializer;
import net.minecraft.resources.Identifier;

import java.util.Map;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW removed fields: projectile_type, explosion, laser, hooks.
public class AmmoData {
    @SerializedName("modifiers")
    @JsonAdapter(AttributeIdMapDeserializer.class)
    private Map<Identifier, GunAttributeModifier> modifiers = Map.of();

    @SerializedName("stack_size")
    private int stackSize = 64;

    @SerializedName("bullet_amount")
    private int bulletAmount = -1;

    public Map<Identifier, GunAttributeModifier> getModifiers() {
        return modifiers;
    }

    public int getStackSize() {
        return stackSize;
    }

    public int getBulletAmount() {
        return bulletAmount;
    }
}
