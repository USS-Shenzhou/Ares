package cn.ussshenzhou.ares.gun.resource.serialize;

import com.google.gson.*;
import cn.ussshenzhou.ares.gun.item.AttachmentSlot;
import cn.ussshenzhou.ares.gun.custom.AttachmentSlotInfo;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/// @author TACZ, USS_Shenzhou
public class AttachmentSlotInfoSerializer implements JsonDeserializer<AttachmentSlotInfo> {
    @Override
    public AttachmentSlotInfo deserialize(JsonElement ele, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        if (ele.isJsonArray()) {
            JsonArray array = ele.getAsJsonArray();
            Map<Identifier, AttachmentSlot> slots = new HashMap<>();
            for (JsonElement element : array) {
                AttachmentSlot slot = ctx.deserialize(element, AttachmentSlot.class);
                if (slot != null) {
                    slots.put(slot.id(), slot);
                }
            }
            return new AttachmentSlotInfo(slots);
        }
        throw new JsonParseException("Invalid AttachmentSlotData, need a JsonArray: " + ele);
    }
}