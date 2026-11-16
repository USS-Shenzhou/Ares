package cn.ussshenzhou.ares.gun.attribute;

import com.google.gson.annotations.SerializedName;

/// @author TACZ, USS_Shenzhou
public record GunAttributeModifier(
        Type type,
        double amount
) {
    public enum Type {
        @SerializedName("addend")
        ADDEND,
        @SerializedName("percent")
        PERCENT,
        @SerializedName("multiplier")
        MULTIPLIER
    }
}
