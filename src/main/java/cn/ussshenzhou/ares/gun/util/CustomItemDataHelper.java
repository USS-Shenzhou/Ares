package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.init.ModDataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/// @author TACZ, USS_Shenzhou
public class CustomItemDataHelper {
    public static final Identifier EMPTY_ID = Identifier.fromNamespaceAndPath(Ares.MOD_ID, "empty");

    // FIXME REVIEW the NBT key parameter is removed from getCustomId/setCustomId and ICustomItem.CUSTOM_ID_TAG is removed; all
    // callers are updated.
    public static Identifier getCustomId(ItemStack stack) {
        if (stack.isEmpty()) {
            return EMPTY_ID;
        }
        Identifier id = stack.get(ModDataComponents.CUSTOM_ITEM_ID);
        return id != null ? id : EMPTY_ID;
    }

    public static void setCustomId(ItemStack stack, Identifier id) {
        if (stack.isEmpty()) {
            return;
        }
        stack.set(ModDataComponents.CUSTOM_ITEM_ID, id);
    }
}
