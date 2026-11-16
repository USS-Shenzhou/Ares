package cn.ussshenzhou.ares.gun.client.animation.mounted.serialize;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import cn.ussshenzhou.ares.gun.client.animation.mounted.OverlayWeightSpec;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

/// > [OverlayWeightSpec] 多态反序列化：
/// > ```
/// >   数字     -> CONSTANT
/// >   对象     -> KEYFRAMES（key=进度字符串，value=权重）
/// >   字符串   -> EXPRESSION（Molang，M4 求值）
/// > ```
/// @author TACZ, USS_Shenzhou
public class OverlayWeightSpecSerializer implements JsonDeserializer<OverlayWeightSpec> {
    @Override
    public OverlayWeightSpec deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json == null || json.isJsonNull()) {
            return OverlayWeightSpec.constant(1.0f);
        }
        if (json.isJsonPrimitive()) {
            JsonPrimitive primitive = json.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                return OverlayWeightSpec.constant(primitive.getAsFloat());
            }
            if (primitive.isString()) {
                return OverlayWeightSpec.expression(primitive.getAsString());
            }
            throw new JsonParseException("Invalid host_overlay weight primitive: " + primitive);
        }
        if (json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();
            TreeMap<Float, Float> points = new TreeMap<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                float time;
                try {
                    time = Float.parseFloat(entry.getKey());
                } catch (NumberFormatException e) {
                    throw new JsonParseException("Invalid host_overlay weight keyframe time: " + entry.getKey(), e);
                }
                points.put(time, entry.getValue().getAsFloat());
            }
            if (points.isEmpty()) {
                return OverlayWeightSpec.constant(1.0f);
            }
            return OverlayWeightSpec.keyframes(points);
        }
        throw new JsonParseException("Invalid host_overlay weight: " + json);
    }
}
