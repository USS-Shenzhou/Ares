package cn.ussshenzhou.ares.gun.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/// > Attachment slot definition.
/// >
/// > Slot id represents a mount point (WHERE), and allowed types represent compatibility (WHAT).
/// >
/// > Note: storage key must be derived from slot id (not from type id) to avoid conflicts.
/// >
/// > <p>内置模块支持：
/// > <ul>
/// >   <li>{@code defaultAttachment}：该槽位的默认（内置）配件 id。未写入 NBT 时虚拟回退到该件。</li>
/// >   <li>{@code required}：必填槽。不允许卸成空，卸载只能回退到默认件。</li>
/// >   <li>{@code rail}：导轨微调配置。声明后已安装配件可沿导轨方向滑动（见 {@link RailConfig}）。</li>
/// > </ul>
/// @author TACZ, USS_Shenzhou
public record AttachmentSlot(
        Identifier id,
        Component name,
        List<AttachmentType> allowTypes,
        String storeTag,
        @Nullable Identifier allowAttachmentTag,
        @Nullable Identifier defaultAttachment,
        boolean required,
        @Nullable RailConfig rail
) {
    private static final String GUN_ATTACHMENT_BASE = "Attachment";

    public static AttachmentSlot fromType(AttachmentType type) {
        return create(type.getId(), null, List.of(type));
    }

    public static AttachmentSlot create(Identifier id, @Nullable String nameKey, List<AttachmentType> allowTypes) {
        return create(id, nameKey, allowTypes, null, null, false, null);
    }

    public static AttachmentSlot create(Identifier id, @Nullable String nameKey, List<AttachmentType> allowTypes,
                                        @Nullable Identifier defaultAttachment, boolean required) {
        return create(id, nameKey, allowTypes, null, defaultAttachment, required, null);
    }

    public static AttachmentSlot create(Identifier id, @Nullable String nameKey, List<AttachmentType> allowTypes,
                                        @Nullable Identifier defaultAttachment, boolean required,
                                        @Nullable RailConfig rail) {
        return create(id, nameKey, allowTypes, null, defaultAttachment, required, rail);
    }

    public static AttachmentSlot create(Identifier id, @Nullable String nameKey, List<AttachmentType> allowTypes,
                                        @Nullable Identifier allowAttachmentTag,
                                        @Nullable Identifier defaultAttachment, boolean required,
                                        @Nullable RailConfig rail) {
        String key = nameKey;
        if (key == null || key.isEmpty()) {
            key = "attachment_slot." + id.getNamespace() + "." + id.getPath();
        }
        return new AttachmentSlot(id, Component.translatable(key), allowTypes, toStoreTag(id), allowAttachmentTag,
                defaultAttachment, required, rail);
    }

    private static String toStoreTag(Identifier id) {
        /// > Always include namespace to avoid collisions across mods.
        return GUN_ATTACHMENT_BASE + "_" +
                id.getNamespace().toUpperCase(Locale.ENGLISH) + "_" +
                id.getPath().toUpperCase(Locale.ENGLISH);
    }

    public boolean isSuitable(AttachmentType type) {
        return allowTypes != null && allowTypes.contains(type);
    }

    /// > 该槽位是否声明了内置默认配件。
    public boolean hasDefault() {
        return defaultAttachment != null;
    }

    /// > 该槽位是否支持导轨微调。
    public boolean hasRail() {
        return rail != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AttachmentSlot that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
