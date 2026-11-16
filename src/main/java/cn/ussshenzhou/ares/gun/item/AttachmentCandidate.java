package cn.ussshenzhou.ares.gun.item;

import cn.ussshenzhou.ares.gun.util.CustomItemDataHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

import java.util.Objects;

/// > 双端兼容性判定所需的最小候选身份。
/// @author TACZ, USS_Shenzhou
public record AttachmentCandidate(
        Identifier id,
        AttachmentType type,
        AttachmentKind kind,
        int subtreeDepth
) {
    public AttachmentCandidate {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(kind, "kind");
        if (subtreeDepth < 0) {
            throw new IllegalArgumentException("subtreeDepth must be non-negative");
        }
    }

    @Nullable
    public static AttachmentCandidate fromStack(ItemStack stack) {
        IAttachmentLike attachmentLike = IAttachmentLike.asAttachmentLike(stack);
        if (attachmentLike == null) {
            return null;
        }
        Identifier id = CustomItemDataHelper.getCustomId(stack);
        if (ICustomItem.EMPTY_ID.equals(id)) {
            return null;
        }
        AttachmentKind kind = stack.getItem() instanceof IMountedWeapon
                ? AttachmentKind.MOUNTED_WEAPON
                : AttachmentKind.NORMAL;
        return new AttachmentCandidate(id, attachmentLike.getType(stack), kind,
                IAttachmentHolder.getSubtreeDepth(stack));
    }
}
