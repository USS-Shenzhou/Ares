package cn.ussshenzhou.ares.gun.item;

import cn.ussshenzhou.ares.gun.TimelessHelperS;
import cn.ussshenzhou.ares.gun.resource.IndexTagLookup;
import cn.ussshenzhou.ares.gun.custom.slotadapter.SlotAdapterTarget;
import cn.ussshenzhou.ares.gun.util.CustomItemDataHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

import static cn.ussshenzhou.ares.gun.item.AttachmentCompatibilityResult.Failure;

/// > 安装、适配器切换和客户端候选筛选共用的纯兼容性判定。
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW renamed from AttachmentCompatibility.
public class AttachmentCompatibilityHelper {
    public static AttachmentCompatibilityResult check(
            IndexTagLookup lookup,
            Identifier holderId,
            AttachmentSlot baseSlot,
            AttachmentSlot effectiveSlot,
            @Nullable Identifier adapterId,
            @Nullable SlotAdapterTarget adapterTarget,
            AttachmentCandidate candidate,
            int pathDepth,
            boolean holderAllows
    ) {
        if (lookup == null || holderId == null || baseSlot == null || effectiveSlot == null || candidate == null) {
            return AttachmentCompatibilityResult.failure(Failure.INVALID_CANDIDATE, null);
        }
        if (adapterId != null && adapterTarget == null) {
            return AttachmentCompatibilityResult.failure(Failure.INVALID_ADAPTER, null);
        }
        if (!holderAllows) {
            return AttachmentCompatibilityResult.failure(Failure.STRUCTURE_NOT_ALLOWED, null);
        }
        if (!effectiveSlot.isSuitable(candidate.type())) {
            return AttachmentCompatibilityResult.failure(Failure.TYPE_NOT_ALLOWED, null);
        }

        Identifier rootTag = AttachmentTagRootHelper.resolve(holderId, baseSlot, adapterId, adapterTarget);
        if (lookup.isKnown(rootTag) && !lookup.isValid(rootTag)) {
            return AttachmentCompatibilityResult.failure(Failure.ROOT_TAG_INVALID, rootTag);
        }
        if (!lookup.contains(rootTag, candidate.id())) {
            return AttachmentCompatibilityResult.failure(Failure.ROOT_TAG_NOT_MATCHED, rootTag);
        }
        if (pathDepth < 1 || pathDepth + candidate.subtreeDepth() > IAttachmentHolder.MAX_ATTACHMENT_DEPTH) {
            return AttachmentCompatibilityResult.failure(Failure.DEPTH_EXCEEDED, rootTag);
        }
        return AttachmentCompatibilityResult.success(rootTag);
    }

    public static AttachmentCompatibilityResult checkRuntime(
            IAttachmentHolder holder,
            ItemStack holderStack,
            AttachmentSlot baseSlot,
            AttachmentSlot effectiveSlot,
            @Nullable Identifier adapterId,
            @Nullable SlotAdapterTarget adapterTarget,
            AttachmentCandidate candidate,
            int pathDepth
    ) {
        Identifier holderId = CustomItemDataHelper.getCustomId(holderStack);
        if (ICustomItem.EMPTY_ID.equals(holderId)) {
            return AttachmentCompatibilityResult.failure(Failure.INVALID_CANDIDATE, null);
        }
        return check(
                TimelessHelperS.getAttachmentTagLookup(),
                holderId,
                baseSlot,
                effectiveSlot,
                adapterId,
                adapterTarget,
                candidate,
                pathDepth,
                holder.canContainAttachment(holderStack, candidate)
        );
    }
}
