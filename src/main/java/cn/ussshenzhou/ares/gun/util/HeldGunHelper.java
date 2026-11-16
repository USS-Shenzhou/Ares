package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.gun.item.IGun;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW renamed from DualWieldHelper. Only getPreferredHand, getPreferredHandItem, gunId and sameHeldGun are kept; the
// one-handed and dual wield checks, DualWieldSignature and the one-handed condition in getPreferredHand are removed.
// isSameItemSameTags replaced with isSameItemSameComponents.
public class HeldGunHelper {
    public static ItemStack getPreferredHandItem(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof IGun) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof IGun) {
            return off;
        }
        return main;
    }

    public static InteractionHand getPreferredHand(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof IGun) {
            return InteractionHand.MAIN_HAND;
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof IGun) {
            return InteractionHand.OFF_HAND;
        }
        return InteractionHand.MAIN_HAND;
    }

    @Nullable
    public static Identifier gunId(ItemStack stack) {
        if (!(stack.getItem() instanceof IGun gun)) {
            return null;
        }
        return gun.getCustomId(stack);
    }

    public static boolean sameHeldGun(ItemStack a, ItemStack b) {
        if (a == b) {
            return true;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return a.isEmpty() == b.isEmpty();
        }
        if (a.getItem() != b.getItem()) {
            return false;
        }
        if (a.getItem() instanceof IGun gun) {
            return gun.getCustomId(a).equals(gun.getCustomId(b));
        }
        return ItemStack.isSameItemSameComponents(a, b);
    }
}
