package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.gun.init.ModDataComponents;
import cn.ussshenzhou.ares.gun.item.AttachmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/// @author TACZ, USS_Shenzhou
public class AttachmentDataHelper {

    /// > 将配件写入持有者的数据中
    public static void setAttachment(ItemStack holder, AttachmentSlot slot, ItemStack attachment) {
        String key = slot.storeTag();
        Map<String, ItemStackTemplate> attachments = new HashMap<>(holder.getOrDefault(ModDataComponents.ATTACHMENTS, Map.of()));
        if (attachment.isEmpty()) {
            attachments.remove(key);
        } else {
            attachments.put(key, ItemStackTemplate.fromNonEmptyStack(attachment));
        }
        holder.set(ModDataComponents.ATTACHMENTS, Map.copyOf(attachments));
    }

    /// > 从持有者数据中移除配件
    public static void removeAttachment(ItemStack holder, AttachmentSlot slot) {
        String key = slot.storeTag();
        Map<String, ItemStackTemplate> attachments = new HashMap<>(holder.getOrDefault(ModDataComponents.ATTACHMENTS, Map.of()));
        attachments.remove(key);
        holder.set(ModDataComponents.ATTACHMENTS, Map.copyOf(attachments));
    }

    /// > 从持有者数据中读取配件数据
    @Nullable
    public static ItemStack getAttachment(ItemStack holder, AttachmentSlot slot) {
        String key = slot.storeTag();
        ItemStackTemplate attachment = holder.getOrDefault(ModDataComponents.ATTACHMENTS, Map.<String, ItemStackTemplate>of()).get(key);
        if (attachment != null) {
            return attachment.create();
        }
        return ItemStack.EMPTY;
    }
}
