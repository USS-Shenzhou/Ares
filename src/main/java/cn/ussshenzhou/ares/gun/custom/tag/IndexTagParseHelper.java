package cn.ussshenzhou.ares.gun.custom.tag;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/// > 标签 JSON 文件解析器。
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW renamed from IndexTagFileParser.
public class IndexTagParseHelper {
    public static IndexTag parse(Identifier tagId, JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            throw new JsonParseException("标签文件必须是对象: " + tagId);
        }
        JsonObject object = element.getAsJsonObject();
        boolean replace = readReplace(tagId, object);
        List<IndexTagEntry> entries = readEntries(tagId, object);
        return new IndexTag(replace, entries);
    }

    private static boolean readReplace(Identifier tagId, JsonObject object) {
        if (!object.has("replace")) {
            return false;
        }
        JsonElement value = object.get("replace");
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new JsonParseException("标签 replace 必须是布尔值: " + tagId);
        }
        return value.getAsBoolean();
    }

    private static List<IndexTagEntry> readEntries(Identifier tagId, JsonObject object) {
        if (!object.has("values")) {
            return List.of();
        }
        JsonElement value = object.get("values");
        if (value == null || !value.isJsonArray()) {
            throw new JsonParseException("标签 values 必须是数组: " + tagId);
        }

        JsonArray values = value.getAsJsonArray();
        List<IndexTagEntry> entries = new ArrayList<>(values.size());
        for (JsonElement entry : values) {
            if (entry == null || !entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                throw new JsonParseException("标签成员必须是字符串: " + tagId);
            }
            entries.add(parseEntry(tagId, entry.getAsString()));
        }
        return entries;
    }

    private static IndexTagEntry parseEntry(Identifier tagId, String raw) {
        boolean tagReference = raw.startsWith("#");
        String idText = tagReference ? raw.substring(1) : raw;
        if (idText.isEmpty() || !idText.contains(":")) {
            throw new JsonParseException("标签成员必须使用完整命名空间: " + raw + " in " + tagId);
        }
        Identifier id = Identifier.tryParse(idText);
        if (id == null) {
            throw new JsonParseException("无效标签成员 ID: " + raw + " in " + tagId);
        }
        return new IndexTagEntry(id, tagReference);
    }
}
