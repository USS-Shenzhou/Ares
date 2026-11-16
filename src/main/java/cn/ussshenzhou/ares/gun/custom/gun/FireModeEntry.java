package cn.ussshenzhou.ares.gun.custom.gun;

import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.JsonAdapter;
import cn.ussshenzhou.ares.gun.attribute.GunAttributeModifier;
import cn.ussshenzhou.ares.gun.resource.serialize.AttributeIdMapDeserializer;
import net.minecraft.resources.Identifier;

import java.util.Map;

/// @author TACZ, USS_Shenzhou
public class FireModeEntry {
    @SerializedName("type")
    private String type;

    @SerializedName("auto")
    private boolean auto = true;

    @SerializedName("trigger")
    private String trigger = "instant";

    @SerializedName("modifiers")
    @JsonAdapter(AttributeIdMapDeserializer.class)
    private Map<Identifier, GunAttributeModifier> modifiers = Map.of();

    @SerializedName("burst_count")
    private int burstCount = 1;

    @SerializedName("burst_interval")
    private int burstInterval = 2;

    @SerializedName("delay_ticks")
    private int delayTicks = 5;

    @SerializedName("charge_time")
    private int chargeTime = 20;

    @SerializedName("charge_min_ratio")
    private float chargeMinRatio = 0.2f;

    @SerializedName("charge_min_multiplier")
    private float chargeMinMultiplier = 0.3f;

    /// > 从纯字符串创建简写模式条目。
    /// > 对预定义的 "auto"、"semi"、"burst" 会设置符合语义的默认值，
    /// > 其余 type 使用字段默认值。
    public static FireModeEntry ofType(String type) {
        FireModeEntry entry = new FireModeEntry();
        entry.type = type;
        switch (type) {
            case "semi" -> entry.auto = false;
            case "burst" -> {
                entry.auto = false;
                entry.burstCount = 3;
            }
            default -> {}
        }
        return entry;
    }

    public String getType() {
        return type;
    }

    public boolean isAuto() {
        return auto;
    }

    public void setAuto(boolean auto) {
        this.auto = auto;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public Map<Identifier, GunAttributeModifier> getModifiers() {
        return modifiers;
    }

    public void setModifiers(Map<Identifier, GunAttributeModifier> modifiers) {
        this.modifiers = modifiers;
    }

    public int getBurstCount() {
        return burstCount;
    }

    public void setBurstCount(int burstCount) {
        this.burstCount = burstCount;
    }

    public int getBurstInterval() {
        return burstInterval;
    }

    public void setBurstInterval(int burstInterval) {
        this.burstInterval = burstInterval;
    }

    public int getDelayTicks() {
        return delayTicks;
    }

    public void setDelayTicks(int delayTicks) {
        this.delayTicks = delayTicks;
    }

    public int getChargeTime() {
        return chargeTime;
    }

    public void setChargeTime(int chargeTime) {
        this.chargeTime = chargeTime;
    }

    public float getChargeMinRatio() {
        return chargeMinRatio;
    }

    public void setChargeMinRatio(float chargeMinRatio) {
        this.chargeMinRatio = chargeMinRatio;
    }

    public float getChargeMinMultiplier() {
        return chargeMinMultiplier;
    }

    public void setChargeMinMultiplier(float chargeMinMultiplier) {
        this.chargeMinMultiplier = chargeMinMultiplier;
    }
}
