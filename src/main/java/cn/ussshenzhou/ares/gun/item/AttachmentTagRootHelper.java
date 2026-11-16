package cn.ussshenzhou.ares.gun.item;

import cn.ussshenzhou.ares.gun.custom.slotadapter.SlotAdapterTarget;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

/// > 可安装配件根 Tag 的派生与优先级规则。
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW renamed from AttachmentTagRoots.
public class AttachmentTagRootHelper {
    private static final String ROOT_PREFIX = "allow_attachments/";
    private static final String ADAPTER_PREFIX = ROOT_PREFIX + "adapters/";

    public static Identifier holder(Identifier holderId) {
        return Identifier.fromNamespaceAndPath(holderId.getNamespace(), ROOT_PREFIX + holderId.getPath());
    }

    public static Identifier adapter(Identifier adapterId) {
        return Identifier.fromNamespaceAndPath(adapterId.getNamespace(), ADAPTER_PREFIX + adapterId.getPath());
    }

    /// > 适配器规则完整替换基础规则；显式覆盖优先于自动派生根。
    public static Identifier resolve(Identifier holderId, AttachmentSlot baseSlot,
                                           @Nullable Identifier adapterId,
                                           @Nullable SlotAdapterTarget adapterTarget) {
        if (adapterId != null) {
            if (adapterTarget != null && adapterTarget.allowAttachmentTag() != null) {
                return adapterTarget.allowAttachmentTag();
            }
            return adapter(adapterId);
        }
        if (baseSlot.allowAttachmentTag() != null) {
            return baseSlot.allowAttachmentTag();
        }
        return holder(holderId);
    }
}
