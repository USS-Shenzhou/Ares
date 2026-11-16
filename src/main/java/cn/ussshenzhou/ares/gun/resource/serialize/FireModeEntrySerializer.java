package cn.ussshenzhou.ares.gun.resource.serialize;

import com.google.gson.*;
import cn.ussshenzhou.ares.gun.attribute.GunAttributeModifier;
import cn.ussshenzhou.ares.gun.custom.gun.FireModeEntry;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Type;
import java.util.Map;

/// > 支持两种写法：
/// > - 简写字符串："auto"/"semi"/"burst" → 由 [FireModeEntry#ofType] 设置预定义默认值
/// > - 完整对象：{"type": "auto", "trigger": "instant", "burst_count": 3, ...}
/// @author TACZ, USS_Shenzhou
public class FireModeEntrySerializer implements JsonDeserializer<FireModeEntry> {
    @Override
    public FireModeEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            return FireModeEntry.ofType(json.getAsString());
        }
        if (json.isJsonObject()) {
            return parseObject(json.getAsJsonObject(), context);
        }
        throw new JsonParseException("Invalid fire_modes entry: expected string or object, got " + json);
    }

    private FireModeEntry parseObject(JsonObject obj, JsonDeserializationContext context) {
        String type = obj.has("type") ? obj.get("type").getAsString() : null;
        FireModeEntry entry = FireModeEntry.ofType(type);

        if (obj.has("auto")) {
            entry.setAuto(obj.get("auto").getAsBoolean());
        }
        if (obj.has("trigger")) {
            entry.setTrigger(obj.get("trigger").getAsString());
        }
        if (obj.has("burst_count")) {
            entry.setBurstCount(obj.get("burst_count").getAsInt());
        }
        if (obj.has("burst_interval")) {
            entry.setBurstInterval(obj.get("burst_interval").getAsInt());
        }
        if (obj.has("delay_ticks")) {
            entry.setDelayTicks(obj.get("delay_ticks").getAsInt());
        }
        if (obj.has("charge_time")) {
            entry.setChargeTime(obj.get("charge_time").getAsInt());
        }
        if (obj.has("charge_min_ratio")) {
            entry.setChargeMinRatio(obj.get("charge_min_ratio").getAsFloat());
        }
        if (obj.has("charge_min_multiplier")) {
            entry.setChargeMinMultiplier(obj.get("charge_min_multiplier").getAsFloat());
        }
        if (obj.has("modifiers")) {
            Map<Identifier, GunAttributeModifier> modifiers = AttributeIdMapDeserializer.read(
                    obj.get("modifiers"), GunAttributeModifier.class, context);
            entry.setModifiers(modifiers);
        }
        return entry;
    }
}
