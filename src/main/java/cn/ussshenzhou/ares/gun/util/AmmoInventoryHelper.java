package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.gun.item.AmmoItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

import java.util.List;

/// > 背包弹药查询工具（双端均可调用）。收编原 {@code ReloadBehavior} 的静态弹药统计方法。
/// @author TACZ, USS_Shenzhou
public class AmmoInventoryHelper {

    /// > 检查玩家背包中是否有匹配的弹药物品。支持多弹种 ID 列表匹配。
    public static boolean hasAmmoInInventory(Player player, List<Identifier> acceptedAmmoIds) {
        return countAmmoInInventory(player, acceptedAmmoIds) > 0;
    }

    /// > 统计玩家背包中匹配弹药的总数量。
    /// > <p>
    /// > {@code acceptedAmmoIds} 为空表示「无任何兼容弹种」，直接返回 0（不再把空列表当作「接受一切」）。
    public static int countAmmoInInventory(Player player, List<Identifier> acceptedAmmoIds) {
        if (acceptedAmmoIds.isEmpty()) {
            return 0;
        }
        int count = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof AmmoItem)) {
                continue;
            }
            Identifier stackAmmoId = CustomItemDataHelper.getCustomId(stack);
            if (!acceptedAmmoIds.contains(stackAmmoId)) {
                continue;
            }
            count += stack.getCount();
        }
        return count;
    }

    /// > 查找第一个匹配弹种的背包槽位索引，找不到返回 -1。
    /// > <p>
    /// > {@code acceptedAmmoIds} 为空表示「无任何兼容弹种」，直接返回 -1。
    public static int findAmmoSlot(Player player, List<Identifier> acceptedAmmoIds) {
        if (acceptedAmmoIds.isEmpty()) {
            return -1;
        }
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof AmmoItem)) {
                continue;
            }
            Identifier stackAmmoId = CustomItemDataHelper.getCustomId(stack);
            if (!acceptedAmmoIds.contains(stackAmmoId)) {
                continue;
            }
            return i;
        }
        return -1;
    }

    /// > 解析换弹应当查找的弹种列表：禁止混装。
    /// > <p>
    /// > 若枪已设定当前弹种（{@code currentAmmoId} 非空且在兼容列表内），换弹只认这一种弹，
    /// > 不会回退去抓背包里其它兼容弹（避免「切到无弹的弹种却被装入另一种弹」）。
    /// > 仅当枪未设定弹种时，才回退到完整兼容列表（首次装填自动适配背包现有弹种）。
    /// > <p>
    /// > 兼容列表为空（未声明任何弹种）时返回空列表，下游据此判定「无可用弹药」，不会装入任意弹药。
    /// >
    /// > @param currentAmmoId 枪当前设定的弹种（可空）
    /// > @param acceptedAmmoIds 枪的全部兼容弹种
    public static List<Identifier> resolveReloadAmmoIds(@Nullable Identifier currentAmmoId,
                                                              List<Identifier> acceptedAmmoIds) {
        if (currentAmmoId != null && acceptedAmmoIds.contains(currentAmmoId)) {
            return List.of(currentAmmoId);
        }
        return acceptedAmmoIds;
    }
}
