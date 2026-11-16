package cn.ussshenzhou.ares.gun.item;

import cn.ussshenzhou.ares.gun.custom.CustomItemIndex;
import cn.ussshenzhou.ares.gun.custom.CustomItemType;
import cn.ussshenzhou.ares.gun.custom.charm.CharmData;
import cn.ussshenzhou.ares.gun.custom.charm.CharmIndex;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;

/// > 基础挂饰物品，用于数据包定义的挂饰（物理装饰物）。
/// >
/// > 走标准 [ICustomItem] 数据驱动管线：名称 / tooltip 由 index 驱动，配置 id 存于
/// > [ICustomItem#CUSTOM_ID_TAG] NBT。
/// @author TACZ, USS_Shenzhou
public class CharmItem extends Item implements ICustomItem<CharmIndex, CharmData> {
    public CharmItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Nonnull
    @Override
    public CustomItemType<CharmIndex, CharmData> getCustomType() {
        return CharmIndex.TYPE;
    }

    @Override
    public Component getName(ItemStack stack) {
        return getIndex(stack).map(CustomItemIndex::getName).orElseGet(() -> super.getName(stack));
    }
}
