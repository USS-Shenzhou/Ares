package cn.ussshenzhou.ares.gun.item;

import com.google.common.collect.Maps;
import cn.ussshenzhou.ares.gun.custom.CustomItemType;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import cn.ussshenzhou.ares.gun.custom.gun.GunIndex;
import cn.ussshenzhou.ares.gun.custom.AttachmentSlotInfo;
import cn.ussshenzhou.ares.gun.init.ModCustomItemTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;

/// @author TACZ, USS_Shenzhou
public interface IGun extends ICustomItem<GunIndex, GunData>, IAttachmentHolder {

    @Nullable
    static IGun asGun(ItemStack stack) {
        if (stack.getItem() instanceof IGun gun) {
            return gun;
        }
        return null;
    }

    @Nonnull
    static Optional<IGun> of(@Nullable ItemStack stack) {
        if (stack == null) {
            return Optional.empty();
        }
        if (stack.getItem() instanceof IGun iGun) {
            return Optional.of(iGun);
        }
        return Optional.empty();
    }

    @Override
    default boolean canContainAttachment(ItemStack holder, AttachmentCandidate candidate) {
        return candidate != null;
    }

    /// > 获取枪的基础配件槽位：处理 disable_parent_slots，但不应用槽位适配器。
    @Nonnull
    @UnmodifiableView
    default Map<Identifier, AttachmentSlot> getBaseAttachmentSlots(ItemStack gun) {
        Map<Identifier, AttachmentSlot> allSlots = this.getIndex(gun).map(GunIndex::getData)
                .map(GunData::getAttachmentSlots)
                .map(AttachmentSlotInfo::getSlots).orElse(Map.of());

        /// > 获取所有被套件禁用的槽位
        Set<Identifier> disabledSlots = getDisabledSlots(gun, allSlots);

        /// > 过滤掉被禁用的槽位
        if (disabledSlots.isEmpty()) {
            return Collections.unmodifiableMap(allSlots);
        }

        Map<Identifier, AttachmentSlot> availableSlots = Maps.newLinkedHashMap();
        for (Map.Entry<Identifier, AttachmentSlot> entry : allSlots.entrySet()) {
            if (!disabledSlots.contains(entry.getKey())) {
                availableSlots.put(entry.getKey(), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(availableSlots);
    }

    /// > 获取所有被套件禁用的父级槽位
    /// > @param gun 枪械物品
    /// > @return 被禁用的槽位ID集合
    @Nonnull
    default Set<Identifier> getDisabledSlots(ItemStack gun, Map<Identifier, AttachmentSlot> allSlots) {
        Set<Identifier> disabledSlots = new HashSet<>();

        for (Map.Entry<Identifier, AttachmentSlot> entry : allSlots.entrySet()) {
            ItemStack attachment = this.getStoredAttachment(gun, entry.getValue());
            if (attachment.isEmpty()) {
                continue;
            }

            if (attachment.getItem() instanceof IAttachment iAttachment) {
                List<Identifier> attachmentDisabledSlots = iAttachment.getDisableParentSlots(attachment);
                disabledSlots.addAll(attachmentDisabledSlots);
            }
        }

        return disabledSlots;
    }

    @Nonnull
    @Override
    default CustomItemType<GunIndex, GunData> getCustomType() {
        return ModCustomItemTypes.GUN.get();
    }
}
