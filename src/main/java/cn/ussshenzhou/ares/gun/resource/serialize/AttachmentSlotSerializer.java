package cn.ussshenzhou.ares.gun.resource.serialize;

import cn.ussshenzhou.ares.Ares;
import com.google.gson.*;
import cn.ussshenzhou.ares.gun.TimelessHelperS;
import cn.ussshenzhou.ares.gun.item.AttachmentSlot;
import cn.ussshenzhou.ares.gun.item.AttachmentType;
import cn.ussshenzhou.ares.gun.item.RailConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import javax.annotation.Nullable;

import java.lang.reflect.Type;
import java.util.List;

/// @author TACZ, USS_Shenzhou
public class AttachmentSlotSerializer implements JsonDeserializer<AttachmentSlot> {
    @Override
    public AttachmentSlot deserialize(JsonElement ele, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        if (ele.isJsonPrimitive() && ele.getAsJsonPrimitive().isString()) {
            /// > Shorthand: treat it as a slot generated from the attachment type id.
            String typeName = normalizeId(ele.getAsString());
            Identifier typeId = Identifier.tryParse(typeName);
            if (typeId == null) {
                throw new JsonParseException("Invalid AttachmentSlot: " + typeName);
            }
            var t = TimelessHelperS.getAttachmentType(typeId);
            if (t != null) {
                return AttachmentSlot.fromType(t);
            }
        } else if (ele.isJsonObject()) {
            JsonObject obj = ele.getAsJsonObject();
            String slotIdStr = normalizeId(GsonHelper.getAsString(obj, "id"));
            String displayName = GsonHelper.getAsString(obj, "name", null);

            Identifier id = Identifier.tryParse(slotIdStr);
            if (id == null) {
                throw new JsonParseException("Invalid AttachmentSlot id: " + slotIdStr);
            }

            List<AttachmentType> allowedTypes = GsonHelper.getAsJsonArray(obj, "allow_types")
                    .asList()
                    .stream()
                    .map(e -> {
                        String typeStr = normalizeId(e.getAsString());
                        Identifier typeId = Identifier.tryParse(typeStr);
                        if (typeId == null) {
                            throw new JsonParseException("Invalid AttachmentSlot allowed type: " + typeStr);
                        }
                        var t = TimelessHelperS.getAttachmentType(typeId);
                        if (t == null) {
                            throw new JsonParseException("Unknown AttachmentType: " + typeId);
                        }
                        return t;
                    })
                    .toList();

            Identifier allowAttachmentTag = parseAllowAttachmentTag(obj);

            /// > 内置模块：默认配件 + 必填性
            Identifier defaultAttachment = null;
            String defaultStr = GsonHelper.getAsString(obj, "default", null);
            if (defaultStr != null && !defaultStr.isEmpty()) {
                defaultAttachment = Identifier.tryParse(normalizeId(defaultStr));
                if (defaultAttachment == null) {
                    throw new JsonParseException("Invalid AttachmentSlot default attachment: " + defaultStr);
                }
            }
            boolean required = GsonHelper.getAsBoolean(obj, "required", false);

            /// > 导轨微调（可选）
            RailConfig rail = parseRail(obj, ctx);

            return AttachmentSlot.create(id, displayName, allowedTypes, allowAttachmentTag, defaultAttachment, required, rail);
        }

        throw new JsonParseException("Invalid AttachmentSlot: " + ele);
    }

    @Nullable
    private static Identifier parseAllowAttachmentTag(JsonObject parent) {
        String raw = GsonHelper.getAsString(parent, "allow_attachment_tag", null);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        if (!raw.contains(":")) {
            throw new JsonParseException("AttachmentSlot allow_attachment_tag must include namespace: " + raw);
        }
        Identifier id = Identifier.tryParse(raw);
        if (id == null) {
            throw new JsonParseException("Invalid AttachmentSlot allow_attachment_tag: " + raw);
        }
        return id;
    }

    /// > 解析可选的 {@code rail} 配置（委托给 {@link RailConfigSerializer}）。
    @Nullable
    private static RailConfig parseRail(JsonObject parent, JsonDeserializationContext ctx) {
        if (!parent.has("rail") || parent.get("rail").isJsonNull()) {
            return null;
        }
        return ctx.deserialize(parent.get("rail"), RailConfig.class);
    }

    private static String normalizeId(String raw) {
        if (raw == null) {
            return "";
        }
        if (!raw.contains(":")) {
            // FIXME REVIEW the default namespace for short ids was hard-coded as "tacz:".
            return Ares.MOD_ID + ":" + raw;
        }
        return raw;
    }
}
