package cn.ussshenzhou.ares.gun.custom.tag;

import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/// > 按资源包优先级合并标签定义。
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW renamed from IndexTagMerger.
public class IndexTagMergeHelper {
    /// > {@code layers} 中每个列表必须已按低优先级到高优先级排列。
    public static Map<Identifier, List<IndexTagEntry>> merge(
            Map<Identifier, ? extends List<IndexTag>> layers) {
        Map<Identifier, List<IndexTagEntry>> result = new LinkedHashMap<>();
        for (var entry : layers.entrySet()) {
            LinkedHashSet<IndexTagEntry> merged = new LinkedHashSet<>();
            for (IndexTag definition : entry.getValue()) {
                if (definition.replace()) {
                    merged.clear();
                }
                merged.addAll(definition.entries());
            }
            result.put(entry.getKey(), List.copyOf(merged));
        }
        return result;
    }
}
