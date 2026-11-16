package cn.ussshenzhou.ares.gun.attribute;

import com.google.gson.annotations.SerializedName;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW merged with GunAttributePojo: changed from record to class and read from JSON directly. The record-style accessors
// (id(), name(), ...) keep their names; id and display name are filled by bind(id, name) after reading.
public class GunAttribute {
    private transient Identifier id;
    private transient Component name;

    @Nullable
    @SerializedName("name")
    private String nameKey;

    @SerializedName("default")
    private double defaultValue;

    @SerializedName("min")
    private double minValue;

    @SerializedName("max")
    private double maxValue;

    @SerializedName("reference")
    private double referenceValue;

    @SerializedName("positive")
    private boolean positive;

    @SerializedName("formatter")
    private GunAttributeFormatter formatter = GunAttributeFormatter.NORMAL;

    public void bind(Identifier id, Component name) {
        this.id = id;
        this.name = name;
    }

    public Identifier id() {
        return id;
    }

    public Component name() {
        return name;
    }

    @Nullable
    public String nameKey() {
        return nameKey;
    }

    public double defaultValue() {
        return defaultValue;
    }

    public double minValue() {
        return minValue;
    }

    public double maxValue() {
        return maxValue;
    }

    public double referenceValue() {
        return referenceValue;
    }

    public boolean positive() {
        return positive;
    }

    public GunAttributeFormatter formatter() {
        return formatter;
    }

    public double clamp(double value) {
        return Double.isNaN(value) ? this.minValue : Mth.clamp(value, this.minValue, this.maxValue);
    }

    public Component getDescription() {
        return name;
    }

}
