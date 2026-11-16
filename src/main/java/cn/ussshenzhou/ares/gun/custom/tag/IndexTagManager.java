package cn.ussshenzhou.ares.gun.custom.tag;

import com.mojang.logging.LogUtils;
import com.google.gson.JsonElement;
import cn.ussshenzhou.ares.gun.util.ResourceScanHelper;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/// > 一个索引标签语义域的资源重载管理器。
/// @author TACZ, USS_Shenzhou
public class IndexTagManager extends SimplePreparableReloadListener<ResourceScanHelper.ScanResult<List<JsonElement>>> {
    private final Identifier domainId;
    private final FileToIdConverter fileConverter;

    private Map<Identifier, List<IndexTagEntry>> mergedDirectEntries = Map.of();
    private Set<Identifier> parseInvalidTags = Set.of();
    private IndexTagRegistry registry = IndexTagRegistry.empty();

    public IndexTagManager(Identifier domainId, String directory) {
        this.domainId = domainId;
        this.fileConverter = FileToIdConverter.json(directory);
    }

    public Identifier getDomainId() {
        return domainId;
    }

    public IndexTagRegistry getRegistry() {
        return registry;
    }

    /// > 在所有 Index 加载完成后调用，以当前语义域的合法成员集合完成解析。
    public IndexTagRegistry resolve(Predicate<Identifier> memberValidator) {
        Map<Identifier, List<IndexTagEntry>> directEntries = new LinkedHashMap<>();
        mergedDirectEntries.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
                .forEach(entry -> directEntries.put(entry.getKey(), List.copyOf(entry.getValue())));

        registry = IndexTagResolveHelper.resolve(
                directEntries,
                parseInvalidTags,
                memberValidator,
                message -> LogUtils.getLogger().error("Index tag domain {}: {}", domainId, message)
        );
        return registry;
    }

    @Override
    protected ResourceScanHelper.ScanResult<List<JsonElement>> prepare(
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        return ResourceScanHelper.scanDirectoryAllWithFailures(
                resourceManager,
                fileConverter,
                cn.ussshenzhou.ares.gun.resource.GunGsonHelper.GSON
        );
    }

    @Override
    protected void apply(
            ResourceScanHelper.ScanResult<List<JsonElement>> scanResult,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        Map<Identifier, List<IndexTag>> layers = new LinkedHashMap<>();
        Set<Identifier> invalidTags = new LinkedHashSet<>(scanResult.failedIds());
        scanResult.values().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
                .forEach(entry -> {
                    Identifier tagId = entry.getKey();
                    List<IndexTag> definitions = new ArrayList<>();
                    for (JsonElement layer : entry.getValue()) {
                        try {
                            definitions.add(IndexTagParseHelper.parse(tagId, layer));
                        } catch (Exception exception) {
                            LogUtils.getLogger().error("Failed to parse index tag {} in domain {}", tagId, domainId, exception);
                            invalidTags.add(tagId);
                        }
                    }
                    layers.put(tagId, definitions);
                });

        mergedDirectEntries = IndexTagMergeHelper.merge(layers);
        parseInvalidTags = Set.copyOf(invalidTags);
        registry = IndexTagRegistry.empty();
    }
}
