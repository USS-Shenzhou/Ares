package cn.ussshenzhou.ares.gun.resource.serialize;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import cn.ussshenzhou.ares.gun.item.RailConfig;
import net.minecraft.util.GsonHelper;
import javax.annotation.Nullable;
import org.joml.Vector3f;

import java.lang.reflect.Type;
import java.util.Locale;

/// > 导轨微调配置的反序列化。
/// >
/// > 形如 `{"axis": "z", "min": -0.05, "max": 0.05, "step": 0.005}`。
/// > `axis` 支持 "x"/"y"/"z" 简写或三元数组 `[x, y, z]`。
/// @author TACZ, USS_Shenzhou
public class RailConfigSerializer implements JsonDeserializer<RailConfig> {
    @Override
    public RailConfig deserialize(JsonElement ele, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        if (!ele.isJsonObject()) {
            throw new JsonParseException("Invalid rail config: " + ele);
        }
        JsonObject obj = ele.getAsJsonObject();
        Vector3f axis = parseAxis(obj.get("axis"));
        float min = GsonHelper.getAsFloat(obj, "min", 0f);
        float max = GsonHelper.getAsFloat(obj, "max", 0f);
        float step = GsonHelper.getAsFloat(obj, "step", 0f);
        return new RailConfig(axis, min, max, step);
    }

    private static Vector3f parseAxis(@Nullable JsonElement ele) {
        if (ele == null || ele.isJsonNull()) {
            return new Vector3f(0, 0, 1);
        }
        if (ele.isJsonArray()) {
            var arr = ele.getAsJsonArray();
            if (arr.size() != 3) {
                throw new JsonParseException("Rail axis array must have 3 elements: " + ele);
            }
            return new Vector3f(arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat());
        }
        String s = ele.getAsString().trim().toLowerCase(Locale.ENGLISH);
        return switch (s) {
            case "x" -> new Vector3f(1, 0, 0);
            case "y" -> new Vector3f(0, 1, 0);
            case "z" -> new Vector3f(0, 0, 1);
            default -> throw new JsonParseException("Invalid rail axis: " + s);
        };
    }
}
