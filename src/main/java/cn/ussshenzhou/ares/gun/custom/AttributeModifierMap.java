package cn.ussshenzhou.ares.gun.custom;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import cn.ussshenzhou.ares.gun.attribute.AttributeIdHelper;
import cn.ussshenzhou.ares.gun.attribute.GunAttributeModifier;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// @author TACZ, USS_Shenzhou
public class AttributeModifierMap {
    private final Map<Identifier, List<GunAttributeModifier>> modifierMap = new HashMap<>();

    public Map<Identifier, List<GunAttributeModifier>> getModifiers() {
        return modifierMap;
    }

    public static class Deserializer implements JsonDeserializer<AttributeModifierMap> {
        @Override
        public AttributeModifierMap deserialize(JsonElement ele, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            if (!ele.isJsonObject()) {
                throw new JsonParseException("AttributeModifierMap must be a JSON object");
            }
            AttributeModifierMap map = new AttributeModifierMap();
            for (Map.Entry<String, JsonElement> entry : ele.getAsJsonObject().entrySet()) {
                String name = entry.getKey();
                Identifier key = AttributeIdHelper.tryParse(name);
                if (key == null) {
                    throw new JsonParseException("Invalid attribute key: " + entry.getKey());
                }

                JsonElement valueElement = entry.getValue();
                if (valueElement == null || valueElement.isJsonNull()) {
                    throw new JsonParseException("Invalid attribute modifier value for key: " + entry.getKey());
                }

                List<GunAttributeModifier> modifiers = new ArrayList<>();

                if (valueElement.isJsonArray()) {
                    /// > 多个修饰
                    for (JsonElement elem : valueElement.getAsJsonArray()) {
                        GunAttributeModifier gm = ctx.deserialize(elem, GunAttributeModifier.class);
                        if (gm == null) {
                            throw new JsonParseException("Invalid attribute modifier value for key: " + entry.getKey());
                        }
                        modifiers.add(gm);
                    }
                } else {
                    /// > 单个修饰
                    GunAttributeModifier gm = ctx.deserialize(valueElement, GunAttributeModifier.class);
                    if (gm == null) {
                        throw new JsonParseException("Invalid attribute modifier value for key: " + entry.getKey());
                    }
                    modifiers.add(gm);
                }

                map.modifierMap.put(key, modifiers);
            }
            return map;
        }
    }
}
