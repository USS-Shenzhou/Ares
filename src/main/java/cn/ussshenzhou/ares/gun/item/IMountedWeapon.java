package cn.ussshenzhou.ares.gun.item;

import cn.ussshenzhou.ares.gun.attribute.GunAttributeModifier;
import cn.ussshenzhou.ares.gun.custom.CustomItemType;
import cn.ussshenzhou.ares.gun.custom.underbarrel.UnderBarrelGunData;
import cn.ussshenzhou.ares.gun.custom.underbarrel.UnderBarrelGunIndex;
import cn.ussshenzhou.ares.gun.init.ModAttachmentTypes;
import cn.ussshenzhou.ares.gun.init.ModCustomItemTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/// > Under-barrel / mounted weapon.
/// >
/// > It is an attachable leaf node (cannot hold other attachments), but may optionally be usable as a standalone weapon.
/// @author TACZ, USS_Shenzhou
public interface IMountedWeapon extends ICustomItem<UnderBarrelGunIndex, UnderBarrelGunData>, IAttachmentLike {

    @Nonnull
    @Override
    default CustomItemType<UnderBarrelGunIndex, UnderBarrelGunData> getCustomType() {
        return ModCustomItemTypes.UNDER_BARREL.get();
    }

    @Nonnull
    @Override
    default AttachmentType getType(ItemStack attachment) {
        return ModAttachmentTypes.UNDER_BARREL.get();
    }

    @Override
    default Map<Identifier, List<GunAttributeModifier>> getModifiers(ItemStack attachment) {
        return this.getIndex(attachment)
                .map(UnderBarrelGunIndex::getData)
                .map(UnderBarrelGunData::getModifiers)
                .orElse(Map.of());
    }

    default boolean isStandalone(ItemStack stack) {
        return this.getIndex(stack)
                .map(UnderBarrelGunIndex::getData)
                .map(UnderBarrelGunData::isStandalone)
                .orElse(false);
    }
}
