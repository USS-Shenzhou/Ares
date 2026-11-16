package cn.ussshenzhou.ares.gun.item;

import cn.ussshenzhou.ares.gun.attribute.GunAttributeModifier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/// > 配件类物品的标记接口，统一普通配件和套件配件的共同行为
/// @author TACZ, USS_Shenzhou
public interface IAttachmentLike {
    /// > 尝试将物品堆转换为配件类物品
    /// > @param stack 物品堆
    /// > @return 如果是配件类物品（IAttachment）返回对应接口，否则返回 null
    @Nullable
    static IAttachmentLike asAttachmentLike(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof IAttachmentLike attachmentLike) {
            return attachmentLike;
        }
        return null;
    }

    /// > 尝试将物品堆转换为配件类物品（Optional 形式）
    /// > @param stack 物品堆
    /// > @return Optional 包装的配件类物品
    @Nonnull
    static Optional<IAttachmentLike> of(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        if (stack.getItem() instanceof IAttachmentLike attachmentLike) {
            return Optional.of(attachmentLike);
        }
        return Optional.empty();
    }

    /// > 获取配件类型
    /// > @param attachment 配件物品堆
    /// > @return 配件类型，如果不是有效配件则返回 NONE 类型
    @Nonnull
    AttachmentType getType(ItemStack attachment);

    /// > 获取配件提供的枪械属性修改器
    /// > @param attachment 配件物品堆
    /// > @return 属性修改器映射
    Map<Identifier, List<GunAttributeModifier>> getModifiers(ItemStack attachment);
}
