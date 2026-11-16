package cn.ussshenzhou.ares.gun.item;

import com.google.common.collect.Maps;
import cn.ussshenzhou.ares.gun.util.AttachmentDataHelper;
import cn.ussshenzhou.ares.gun.util.AttachmentSlotHelper;
import cn.ussshenzhou.ares.gun.util.AttachmentTreeHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// > 可以容纳配件的对象（枪械、带槽位的配件）。
/// @author TACZ, USS_Shenzhou
public interface IAttachmentHolder {
    /// > 配件树的最大深度。语义：从任意 holder 算起，其下子配件树的最大层数。
    /// > <p>
    /// > 同时用作属性聚合 / 渲染递归遍历的硬截断，保证即便数据异常也不会无限递归。
    int MAX_ATTACHMENT_DEPTH = 3;

    /// > 计算某个物品堆作为容器时，其已安装子配件树的高度（叶子节点高度为 0）。
    /// > <p>
    /// > 遍历使用 NBT 读取（{@link #getStoredAttachment}），避免触发属性缓存递归。
    static int getSubtreeDepth(ItemStack stack) {
        return getSubtreeDepth(stack, MAX_ATTACHMENT_DEPTH);
    }

    private static int getSubtreeDepth(ItemStack stack, int remaining) {
        if (remaining <= 0 || stack == null || stack.isEmpty()) {
            return 0;
        }
        if (!(stack.getItem() instanceof IAttachmentHolder holder)) {
            return 0;
        }
        int max = 0;
        for (var entry : holder.getAttachmentSlots(stack).entrySet()) {
            ItemStack child = holder.getStoredAttachment(stack, entry.getValue());
            if (child.isEmpty()) {
                continue;
            }
            max = Math.max(max, 1 + getSubtreeDepth(child, remaining - 1));
        }
        return max;
    }

    /// > 仅表达 holder 自身的结构约束；槽位类型、根 Tag 与整树深度由统一兼容性组件判断。
    boolean canContainAttachment(ItemStack holder, AttachmentCandidate candidate);

    /// > 基础槽位：考虑 holder 自身的可见性规则，但尚未应用 NBT 中选中的适配器。
    @Nonnull
    @UnmodifiableView
    Map<Identifier, AttachmentSlot> getBaseAttachmentSlots(ItemStack holder);

    /// > 当前有效槽位。无效或已失去 target 的适配器会回退到基础槽位；其安装操作仍由兼容性组件失败关闭。
    @Nonnull
    @UnmodifiableView
    default Map<Identifier, AttachmentSlot> getAttachmentSlots(ItemStack holder) {
        return AttachmentSlotHelper.applyAdapters(holder, getBaseAttachmentSlots(holder));
    }

    /// > 获取nbt中持久化的配件，应当返回一个新的配件物品实例；如果为空且定义了虚拟配件，则回退到默认虚拟配件.
    /// > <p>
    /// > 对于内置模块：若 NBT 未写入该槽位且槽位声明了默认配件，则返回虚拟默认件（不写 NBT）。
    /// > 一旦玩家替换或往其子槽装件，会通过安装链 copy-on-write 实体化进 NBT。
    // FIXME REVIEW renamed from the misspelled getStorgeAttachment.
    default ItemStack getStoredAttachment(ItemStack holder, AttachmentSlot slot) {
        ItemStack stored = AttachmentDataHelper.getAttachment(holder, slot);
        if (stored != null && !stored.isEmpty()) {
            return stored;
        }
        return AttachmentTreeHelper.resolveDefault(slot);
    }

    /// > 获取某个槽位里的配件。可能取到缓存的物品实例，所以不应该直接写此方法返回的实例。
    default ItemStack getAttachment(ItemStack holder, AttachmentSlot slot) {
        return this.getStoredAttachment(holder, slot);
    }

    /// > Get direct attachments of the given type from this holder.
    /// >
    /// > NOTE: this does NOT recurse into nested holders. Nesting rules are enforced by the compatibility component.
    default List<ItemStack> getAttachments(ItemStack holder, AttachmentType type) {
        List<ItemStack> result = new ArrayList<>();
        var slots = getAttachmentSlots(holder);
        for (var entry : slots.entrySet()) {
            ItemStack attachment = this.getAttachment(holder, entry.getValue());
            if (attachment.isEmpty()) {
                continue;
            }

            var attachmentLike = IAttachmentLike.asAttachmentLike(attachment);
            if (attachmentLike == null || !attachmentLike.getType(attachment).equals(type)) {
                continue;
            }

            result.add(attachment);
        }
        return result;
    }

    @Nonnull
    default Map<AttachmentSlot, ItemStack> getAttachmentAndSlots(ItemStack holder) {
        var slots = getAttachmentSlots(holder);
        Map<AttachmentSlot, ItemStack> result = Maps.newLinkedHashMap();
        for (var entry : slots.entrySet()) {
            ItemStack attachment = this.getAttachment(holder, entry.getValue());
            if (attachment.isEmpty()) {
                continue;
            }
            result.put(entry.getValue(), attachment);
        }
        return result;
    }
}
