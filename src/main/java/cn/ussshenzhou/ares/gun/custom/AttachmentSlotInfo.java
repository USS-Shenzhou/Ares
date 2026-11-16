package cn.ussshenzhou.ares.gun.custom;

import com.google.common.collect.Maps;
import cn.ussshenzhou.ares.gun.item.AttachmentSlot;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Set;

/// @author TACZ, USS_Shenzhou
public class AttachmentSlotInfo {
    private Map<Identifier, AttachmentSlot> slots = Maps.newHashMap();

    public AttachmentSlotInfo() {
    }

    public AttachmentSlotInfo(Map<Identifier, AttachmentSlot> slots) {
        this.slots = slots;
    }

    public Map<Identifier, AttachmentSlot> getSlots() {
        return slots;
    }

    public Set<Identifier> getSlotsId() {
        return slots.keySet();
    }
}
