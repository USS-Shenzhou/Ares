package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import cn.ussshenzhou.ares.gun.custom.gun.GunIndex;
import cn.ussshenzhou.ares.gun.item.IGun;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW isSameItemSameTags replaced with isSameItemSameComponents.
public class DualWieldHelper {
    /// > 「副手可用特殊物品」物品标签：主手持单手枪时，副手持此标签内的物品（如盾牌）将启用
    /// > 主手单手形态——主手左键开火，副手右键走原版使用（盾牌格挡等）。
    public static final TagKey<Item> OFFHAND_USABLE = TagKey.create(Registries.ITEM, Ares.modLoc("offhand_usable"));

    public static boolean isOneHandedGun(ItemStack stack) {
        if (!(stack.getItem() instanceof IGun gun)) {
            return false;
        }
        GunData data = gun.getIndex(stack).map(GunIndex::getData).orElse(null);
        return data != null && data.isOneHanded();
    }

    /// > 该物品是否为「副手可用特殊物品」（带 [#OFFHAND_USABLE] 标签，如盾牌）。
    public static boolean isOffhandUsable(ItemStack stack) {
        return !stack.isEmpty() && stack.is(OFFHAND_USABLE);
    }

    /// > 是否为「主手单手枪 + 副手可用特殊物品」组合：主手以单手（右手）形态持枪，
    /// > 副手保留原版使用（右键格挡等），主手左键开火。
    public static boolean isMainOneHandWithOffhandItem(Player player) {
        return isMainOneHandWithOffhandItem(player.getMainHandItem(), player.getOffhandItem());
    }

    public static boolean isMainOneHandWithOffhandItem(ItemStack mainStack, ItemStack offStack) {
        return isOneHandedGun(mainStack) && isOffhandUsable(offStack);
    }

    public static boolean isDualWielding(Player player) {
        return isDualWielding(player.getMainHandItem(), player.getOffhandItem());
    }

    public static boolean isDualWielding(ItemStack mainStack, ItemStack offStack) {
        return !mainStack.isEmpty() && !offStack.isEmpty() && isOneHandedGun(mainStack) && isOneHandedGun(offStack);
    }

    /// > 该手当前是否以「单手形态」持枪。
    /// >
    /// > 副手只在「自身是单手枪」且「主手未持双手枪霸占视野」时成立——主手持双手长枪时副手等于未启用。
    /// > 主手在双持、或「自身单手枪 + 副手持可用特殊物品（盾牌等）」时单手，否则双手持握。
    /// > 渲染（选用单手按手变体 display）与 operator 会话形态判定共用此口径，避免二者发散。
    public static boolean isOneHandedForm(Player player, InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND) {
            return isOneHandedGun(player.getOffhandItem()) && !mainHandOccupiesView(player);
        }
        return isDualWielding(player) || isMainOneHandWithOffhandItem(player);
    }

    /// > 主手是否持有会霸占视野的双手枪（持双手长枪时副手枪不渲染、不启用）。
    public static boolean mainHandOccupiesView(Player player) {
        ItemStack main = player.getMainHandItem();
        return main.getItem() instanceof IGun && !isOneHandedGun(main);
    }

    public static ItemStack getPreferredHandItem(Player player) {
        ItemStack main = player.getMainHandItem();
        if (isOneHandedGun(main) || main.getItem() instanceof IGun) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (isOneHandedGun(off) || off.getItem() instanceof IGun) {
            return off;
        }
        return main;
    }

    public static InteractionHand getPreferredHand(Player player) {
        ItemStack main = player.getMainHandItem();
        if (isOneHandedGun(main) || main.getItem() instanceof IGun) {
            return InteractionHand.MAIN_HAND;
        }
        ItemStack off = player.getOffhandItem();
        if (isOneHandedGun(off) || off.getItem() instanceof IGun) {
            return InteractionHand.OFF_HAND;
        }
        return InteractionHand.MAIN_HAND;
    }

    public static DualWieldSignature signature(Player player) {
        return signature(player.getMainHandItem(), player.getOffhandItem());
    }

    public static DualWieldSignature signature(ItemStack mainStack, ItemStack offStack) {
        boolean active = isDualWielding(mainStack, offStack);
        return new DualWieldSignature(
                active,
                gunId(mainStack),
                gunId(offStack),
                isOneHandedGun(mainStack),
                isOneHandedGun(offStack),
                isOffhandUsable(offStack)
        );
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
