package cn.ussshenzhou.ares.gun.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.Locale;

/// @author TACZ, USS_Shenzhou
public class AttachmentType {
    private final Identifier id;
    private final Component name;
    private final AttachmentKind kind;

    /// > Create via registry only.
    @ApiStatus.Internal
    public AttachmentType(Identifier id, AttachmentKind kind) {
        this.id = id;
        this.name = Component.translatable("attachment_type." + id.getNamespace() + "." + id.getPath());
        this.kind = kind;
    }

    public Identifier getId() {
        return id;
    }

    public Component getDisplayName() {
        return name;
    }

    public AttachmentKind getKind() {
        return kind;
    }

    public boolean isMountedWeapon() {
        return kind == AttachmentKind.MOUNTED_WEAPON;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AttachmentType that)) {
            return false;
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /// > Only for legacy/tag formatting. Do not use as a unique storage key.
    public String nbtTag() {
        return id.getPath().toUpperCase(Locale.ENGLISH);
    }
}
