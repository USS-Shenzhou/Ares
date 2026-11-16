package cn.ussshenzhou.ares.gun.custom.tag;

import net.minecraft.resources.Identifier;

import java.util.Objects;

/// > 标签定义中的一条直接成员；可指向索引 ID 或另一标签。
/// @author TACZ, USS_Shenzhou
public record IndexTagEntry(Identifier id, boolean tagReference) {
    public IndexTagEntry {
        Objects.requireNonNull(id, "id");
    }

    public static IndexTagEntry member(Identifier id) {
        return new IndexTagEntry(id, false);
    }

    public static IndexTagEntry tag(Identifier id) {
        return new IndexTagEntry(id, true);
    }
}
