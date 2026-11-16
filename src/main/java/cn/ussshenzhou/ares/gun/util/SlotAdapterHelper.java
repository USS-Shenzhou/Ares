package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.gun.init.ModDataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/// > 槽位适配器选择的 NBT 存取（双端共用）。
/// >
/// > 适配器选择存于「宿主」的 ItemStack NBT 复合标签 `SlotAdapters` 下，按槽位 id 索引
/// > （`slotId(string) -> adapterId(string)`）。
/// >
/// > 关键：适配器选择独立于该槽位是否安装了子配件——空槽也能记住所选适配器
/// > （例如玩家配置了「垫高的空导轨」）。多层树场景下由 [AttachmentTreeHelper] 的
/// > 自底向上回写机制序列化到顶层枪械。
/// @author TACZ, USS_Shenzhou
public class SlotAdapterHelper {
    /// > 在宿主上为指定槽位写入选中的适配器 id。
    public static void setAdapter(ItemStack holder, Identifier slotId, Identifier adapterId) {
        if (holder.isEmpty() || slotId == null || adapterId == null) {
            return;
        }
        Map<Identifier, Identifier> adapters = new HashMap<>(holder.getOrDefault(ModDataComponents.SLOT_ADAPTERS, Map.of()));
        adapters.put(slotId, adapterId);
        holder.set(ModDataComponents.SLOT_ADAPTERS, Map.copyOf(adapters));
    }

    /// > 读取宿主上指定槽位选中的适配器 id；未设置返回 null。
    @Nullable
    public static Identifier getAdapter(ItemStack holder, Identifier slotId) {
        if (holder.isEmpty() || slotId == null) {
            return null;
        }
        Map<Identifier, Identifier> adapters = holder.get(ModDataComponents.SLOT_ADAPTERS);
        if (adapters == null) {
            return null;
        }
        return adapters.get(slotId);
    }

    public static boolean hasAdapter(ItemStack holder, Identifier slotId) {
        return getAdapter(holder, slotId) != null;
    }

    /// > 移除宿主上指定槽位的适配器选择（恢复无适配器状态）。
    public static void removeAdapter(ItemStack holder, Identifier slotId) {
        if (holder.isEmpty() || slotId == null) {
            return;
        }
        Map<Identifier, Identifier> stored = holder.get(ModDataComponents.SLOT_ADAPTERS);
        if (stored == null) {
            return;
        }
        Map<Identifier, Identifier> adapters = new HashMap<>(stored);
        adapters.remove(slotId);
        if (adapters.isEmpty()) {
            holder.remove(ModDataComponents.SLOT_ADAPTERS);
        } else {
            holder.set(ModDataComponents.SLOT_ADAPTERS, Map.copyOf(adapters));
        }
    }
}
