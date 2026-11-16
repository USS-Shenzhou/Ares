package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.gun.init.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/// > 配件导轨微调偏移的 NBT 存取（双端共用）。
/// >
/// > 偏移直接写入配件自身的 ItemStack NBT，随配件实例移动；
/// > 多层树场景下由 [AttachmentTreeHelper] 的自底向上回写机制序列化到顶层枪械。
/// >
/// > 第一阶段仅存单轴标量偏移（沿槽位 [cn.ussshenzhou.ares.gun.item.RailConfig#axis]）。
/// @author TACZ, USS_Shenzhou
public class AttachmentOffsetHelper {
    /// > 写入沿导轨方向的偏移标量。
    public static void setOffset(ItemStack attachment, float offset) {
        if (attachment.isEmpty()) {
            return;
        }
        attachment.set(ModDataComponents.RAIL_OFFSET, offset);
    }

    /// > 读取沿导轨方向的偏移标量；未设置返回 0。
    public static float getOffset(ItemStack attachment) {
        if (attachment.isEmpty()) {
            return 0f;
        }
        return attachment.getOrDefault(ModDataComponents.RAIL_OFFSET, 0f);
    }

    /// > 是否写入过偏移（区分「显式 0」与「未设置」用，一般无需关心）。
    public static boolean hasOffset(ItemStack attachment) {
        if (attachment.isEmpty()) {
            return false;
        }
        return attachment.has(ModDataComponents.RAIL_OFFSET);
    }

    /// > 移除偏移数据。
    public static void removeOffset(ItemStack attachment) {
        if (attachment.isEmpty()) {
            return;
        }
        attachment.remove(ModDataComponents.RAIL_OFFSET);
    }
}
