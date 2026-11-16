package cn.ussshenzhou.ares.gun.item;

import cn.ussshenzhou.ares.gun.TimelessHelperS;
import cn.ussshenzhou.ares.gun.attribute.GunAttributeModifier;
import net.minecraft.resources.Identifier;
import cn.ussshenzhou.ares.gun.custom.CustomItemType;
import cn.ussshenzhou.ares.gun.custom.attachment.AttachmentData;
import cn.ussshenzhou.ares.gun.custom.attachment.AttachmentIndex;
import cn.ussshenzhou.ares.gun.custom.variant.VariantIndex;
import cn.ussshenzhou.ares.gun.init.ModAttachmentTypes;
import cn.ussshenzhou.ares.gun.init.ModCustomItemTypes;
import cn.ussshenzhou.ares.gun.util.ToggleHelper;
import cn.ussshenzhou.ares.gun.util.VariantHelper;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// @author TACZ, USS_Shenzhou
public interface IAttachment extends ICustomItem<AttachmentIndex, AttachmentData>, IAttachmentLike, IAttachmentHolder {
    @Nullable
    static IAttachment asAttachment(ItemStack stack) {
        if (stack.getItem() instanceof IAttachment iAttachment) {
            return iAttachment;
        }
        return null;
    }

    @Nonnull
    static Optional<IAttachment> of(@Nullable ItemStack stack) {
        if (stack == null) {
            return Optional.empty();
        }
        if (stack.getItem() instanceof IAttachment iAttachment) {
            return Optional.of(iAttachment);
        }
        return Optional.empty();
    }

    @Nonnull
    @Override
    default CustomItemType<AttachmentIndex, AttachmentData> getCustomType() {
        return ModCustomItemTypes.ATTACHMENT.get();
    }

    @Nonnull
    default AttachmentType getType(ItemStack attachment) {
        return this.getIndex(attachment)
                .map(AttachmentIndex::getAttachmentType)
                .orElse(ModAttachmentTypes.NONE.get());
    }

    /// > 读取配件「当前生效形态」的完整附件数据。
    /// >
    /// > 开启态 / 不可开关形态返回基础 data；关闭态优先取当前生效形态（变体优先）声明的
    /// > off_data，缺失时回退基础 data。index 不可得返回 null。
    @Nullable
    default AttachmentData getActiveData(ItemStack attachment) {
        AttachmentIndex index = this.getIndex(attachment).orElse(null);
        if (index == null) {
            return null;
        }
        if (ToggleHelper.isEnabled(attachment)) {
            return index.getData();
        }
        /// > 关闭态：当前生效形态（变体优先）的 off_data 优先，缺失回退基础 data
        Identifier variantId = VariantHelper.getVariant(attachment);
        if (variantId != null) {
            VariantIndex variant = TimelessHelperS.getIndex(variantId, ModCustomItemTypes.ATTACHMENT_VARIANT.get()).orElse(null);
            if (variant != null && variant.getOffData() != null) {
                return variant.getOffData();
            }
        }
        AttachmentData offData = index.getOffData();
        return offData != null ? offData : index.getData();
    }

    default Map<Identifier, List<GunAttributeModifier>> getModifiers(ItemStack attachment) {
        AttachmentData data = getActiveData(attachment);
        return data == null ? Map.of() : data.getModifiers();
    }

    /// > 该配件当前生效形态是否为原厂配件（几何寄生在宿主枪模型中，无独立 display/模型）。
    default boolean isBuiltin(ItemStack attachment) {
        AttachmentData data = getActiveData(attachment);
        return data != null && data.isBuiltin();
    }

    /// > 判断任意物品堆当前生效形态是否为原厂配件（非配件返回 false）。
    static boolean isBuiltinAttachment(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof IAttachment iAttachment) {
            return iAttachment.isBuiltin(stack);
        }
        return false;
    }

    /// > 该配件当前生效形态提供的子配件槽位（可为空 map，表示叶子配件）。
    @Nonnull
    @UnmodifiableView
    @Override
    default Map<Identifier, AttachmentSlot> getBaseAttachmentSlots(ItemStack holder) {
        AttachmentData data = getActiveData(holder);
        if (data == null) {
            return Map.of();
        }
        return Collections.unmodifiableMap(data.getAttachmentSlots().getSlots());
    }

    /// > 普通配件容器只接受普通配件；下挂武器不能嵌套在配件下。
    @Override
    default boolean canContainAttachment(ItemStack holder, AttachmentCandidate candidate) {
        return candidate != null && candidate.kind() != AttachmentKind.MOUNTED_WEAPON;
    }

    /// > 该配件当前生效形态禁用的父级（枪械）槽位列表。
    default List<Identifier> getDisableParentSlots(ItemStack attachment) {
        AttachmentData data = getActiveData(attachment);
        return data == null ? List.of() : data.getDisableParentSlots();
    }

    /// > 该配件当前生效形态的扩展弹匣等级。
    default int getExtendedMagLevel(ItemStack attachment) {
        AttachmentData data = getActiveData(attachment);
        return data == null ? 0 : data.getExtendedMagLevel();
    }
}
