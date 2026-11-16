package cn.ussshenzhou.ares.gun.custom.tag;

import java.util.List;

/// > 单个资源包层中的标签定义。
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW renamed from IndexTagDefinition.
public record IndexTag(boolean replace, List<IndexTagEntry> entries) {
    public IndexTag {
        entries = List.copyOf(entries);
    }
}
