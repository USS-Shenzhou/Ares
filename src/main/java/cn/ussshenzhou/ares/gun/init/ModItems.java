package cn.ussshenzhou.ares.gun.init;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.TimelessHelperS;
import cn.ussshenzhou.ares.gun.custom.CustomItemIndex;
import cn.ussshenzhou.ares.gun.item.*;
import cn.ussshenzhou.ares.gun.util.CustomItemDataHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW @EventBusSubscriber without listener methods is removed here and in ModAttachmentTypes, because NeoForge throws
// for such classes.
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Ares.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Ares.MOD_ID);

    // FIXME REVIEW 26.3 Item.Properties needs the item id, so item constructors take Properties and items are registered with
    // registerItem.
    public static DeferredItem<GunItem> GUN = ITEMS.registerItem("gun", GunItem::new);
    public static DeferredItem<AttachmentItem> ATTACHMENT = ITEMS.registerItem("attachment", AttachmentItem::new);
    public static DeferredItem<UnderBarrelWeaponItem> UNDER_BARREL = ITEMS.registerItem("under_barrel", UnderBarrelWeaponItem::new);
    public static DeferredItem<AmmoItem> AMMO = ITEMS.registerItem("ammo", AmmoItem::new);
    public static DeferredItem<CharmItem> CHARM = ITEMS.registerItem("charm", CharmItem::new);

    public static DeferredHolder<CreativeModeTab, CreativeModeTab> OTHER_TAB = TABS.register("other", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tab.ares.test"))
            .icon(() -> ModItems.GUN.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                for (var type : TimelessHelperS.getCustomItemTypes()) {
                    Map<Identifier, ? extends CustomItemIndex<?>> indexes = TimelessHelperS.getAllIndexes(type);
                    indexes.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(CustomItemIndex.ID_NAME_SORT))
                            .forEach(entry -> {
                                ItemStack stack = entry.getValue().createStack(1);
                                /// > 变体 / 适配器等非实体物品（baseItem 为 AIR）不进创造栏
                                if (stack.isEmpty()) {
                                    return;
                                }
                                CustomItemDataHelper.setCustomId(stack, entry.getKey());
                                output.accept(stack);
                            });
                }
            }).build());

}
