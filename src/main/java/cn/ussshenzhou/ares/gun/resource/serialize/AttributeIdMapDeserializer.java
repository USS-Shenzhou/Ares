package cn.ussshenzhou.ares.gun.resource.serialize;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import cn.ussshenzhou.ares.gun.attribute.AttributeIdHelper;
import net.minecraft.resources.Identifier;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/// > 以属性 id 为键的 JSON 对象反序列化。
/// >
/// > 键统一走 [AttributeIdHelper#tryParse]（缺省 namespace 补 `tacz:`，非法键直接报错），
/// > 值按字段声明的值类型交回 Gson 解析——因此同一适配器可服务 `Map<Identifier, Double>`
/// > 与 `Map<Identifier, GunAttributeModifier>` 两种形状。
/// >
/// > 用 `@JsonAdapter` 挂在具体字段上。自定义解析器（如 `FireModeEntrySerializer`）
/// > 可复用 [#read]。
/// @author TACZ, USS_Shenzhou
public class AttributeIdMapDeserializer implements JsonDeserializer<Map<Identifier, Object>> {
    @Override
    public Map<Identifier, Object> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return read(json, valueTypeOf(typeOfT), context);
    }

    /// > 解析「属性 id -> 值」对象。
    public static <V> Map<Identifier, V> read(JsonElement json, Type valueType, JsonDeserializationContext context) {
        if (json == null || !json.isJsonObject()) {
            throw new JsonParseException("Expected a JSON object keyed by attribute id");
        }
        Map<Identifier, V> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
            Identifier id = AttributeIdHelper.tryParse(entry.getKey());
            if (id == null) {
                throw new JsonParseException("Invalid attribute id: " + entry.getKey());
            }
            result.put(id, context.deserialize(entry.getValue(), valueType));
        }
        return result;
    }

    private static Type valueTypeOf(Type typeOfT) {
        if (typeOfT instanceof ParameterizedType parameterized && parameterized.getActualTypeArguments().length == 2) {
            return parameterized.getActualTypeArguments()[1];
        }
        throw new JsonParseException("Attribute id map must declare its value type: " + typeOfT);
    }
}
