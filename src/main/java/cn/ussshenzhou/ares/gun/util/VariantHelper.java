package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.gun.init.ModDataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

/// > 配件变体选择的 NBT 存取（双端共用）。
/// > <p>
/// > 选中的变体 id 直接写入配件自身的 ItemStack NBT（键 {@code Variant}），随配件实例移动；
/// > 多层树场景下由 {@link AttachmentTreeHelper} 的自底向上回写机制序列化到顶层枪械。
/// > <p>
/// > 未写入时表示「无变体 / 基础几何」。
/// @author TACZ, USS_Shenzhou
public class VariantHelper {
    /// > 写入选中的变体 id。
    public static void setVariant(ItemStack attachment, Identifier variantId) {
        if (attachment.isEmpty() || variantId == null) {
            return;
        }
        attachment.set(ModDataComponents.VARIANT, variantId);
    }

    /// > 读取选中的变体 id；未设置返回 null（表示基础几何）。
    @Nullable
    public static Identifier getVariant(ItemStack attachment) {
        if (attachment.isEmpty()) {
            return null;
        }
        return attachment.get(ModDataComponents.VARIANT);
    }

    public static boolean hasVariant(ItemStack attachment) {
        if (attachment.isEmpty()) {
            return false;
        }
        return attachment.has(ModDataComponents.VARIANT);
    }

    /// > 移除变体选择（恢复基础几何）。
    public static void removeVariant(ItemStack attachment) {
        if (attachment.isEmpty()) {
            return;
        }
        attachment.remove(ModDataComponents.VARIANT);
    }
}
