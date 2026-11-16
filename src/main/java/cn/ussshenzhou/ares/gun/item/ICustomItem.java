package cn.ussshenzhou.ares.gun.item;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.TimelessHelperS;
import cn.ussshenzhou.ares.gun.custom.CustomItemIndex;
import cn.ussshenzhou.ares.gun.custom.CustomItemType;
import cn.ussshenzhou.ares.gun.util.CustomItemDataHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;

import java.util.Optional;

/// @author TACZ, USS_Shenzhou
public interface ICustomItem<T extends CustomItemIndex<D>, D> {
    Identifier EMPTY_ID = Ares.modLoc("empty");

    @Nonnull
    CustomItemType<T, D> getCustomType();

    /// > 根据物品堆获取自定义物品的配置ID
    @Nonnull
    default Identifier getCustomId(@Nonnull ItemStack stack) {
        return CustomItemDataHelper.getCustomId(stack);
    }

    /// > 根据物品堆获取自定义物品索引
    default Optional<T> getIndex(@Nonnull ItemStack stack) {
        CustomItemType<T, D> type = getCustomType();
        Identifier id = getCustomId(stack);
        if (EMPTY_ID.equals(id)) {
            return Optional.empty();
        }
        return TimelessHelperS.getIndex(id, type);
    }

    default Identifier getDisplayId(ItemStack gun) {
        return this.getIndex(gun).map(CustomItemIndex::getDisplayLocation).orElse(ICustomItem.EMPTY_ID);
    }
}