package cn.ussshenzhou.ares.gun.custom;

import com.mojang.logging.LogUtils;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import cn.ussshenzhou.ares.gun.util.ResourceScanHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.UnmodifiableView;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/// @author TACZ, USS_Shenzhou
public class CustomIndexManager<T extends CustomItemIndex<D>, D> extends SimplePreparableReloadListener<CustomIndexManager.RawIndexData> {

    // FIXME REVIEW renamed from IndexPojo.
    public record RawIndexData(
            Map<Identifier, JsonElement> indexes,
            Map<Identifier, JsonElement> data,
            Set<Identifier> indexReadFailures,
            Set<Identifier> dataReadFailures
    ) {
        public RawIndexData {
            indexes = Map.copyOf(indexes);
            data = Map.copyOf(data);
            indexReadFailures = Set.copyOf(indexReadFailures);
            dataReadFailures = Set.copyOf(dataReadFailures);
        }
    }

    protected HolderLookup.Provider registryAccess;

    protected final Map<Identifier, T> indexes = Maps.newHashMap();
    protected CustomItemType<T, D> customItemType;

    private final Gson gson;
    private final Marker marker;

    private final FileToIdConverter indexPathConverter;
    private final FileToIdConverter dataPathConverter;

    private Set<Identifier> indexReadFailureIds = Set.of();
    private Set<Identifier> dataReadFailureIds = Set.of();
    private Set<Identifier> constructionFailureIds = Set.of();

    public CustomIndexManager(
            CustomItemType<T, D> customItemType,
            String marker
    ) {
        this.customItemType = customItemType;
        this.gson = customItemType.gson();
        this.marker = MarkerFactory.getMarker(marker);
        this.indexPathConverter = customItemType.indexPathConverter();
        this.dataPathConverter = customItemType.dataPathConverter();
    }

    public void setRegistryAccess(HolderLookup.Provider registryAccess) {
        this.registryAccess = registryAccess;
    }

    @ParametersAreNonnullByDefault
    @Nonnull
    @Override
    protected RawIndexData prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        var indexResult = ResourceScanHelper.scanDirectoryWithFailures(pResourceManager, indexPathConverter, this.gson);
        var dataResult = ResourceScanHelper.scanDirectoryWithFailures(pResourceManager, dataPathConverter, this.gson);
        return new RawIndexData(
                indexResult.values(),
                dataResult.values(),
                indexResult.failedIds(),
                dataResult.failedIds()
        );
    }

    @ParametersAreNonnullByDefault
    @Override
    protected void apply(RawIndexData raw, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<Identifier, JsonElement> map = Collections.unmodifiableMap(raw.data());
        Set<Identifier> constructionFailures = new LinkedHashSet<>();

        for (Map.Entry<Identifier, JsonElement> entry : raw.indexes().entrySet()) {
            Identifier id = entry.getKey();
            JsonElement element = entry.getValue();
            try {
                T index = customItemType.factory().create(this.gson, id, element, map, registryAccess);
                if (index != null) {
                    indexes.put(id, index);
                } else {
                    constructionFailures.add(id);
                }
            } catch (Exception e) {
                constructionFailures.add(id);
                LogUtils.getLogger().error(marker, "Failed to load index file {}", id, e);
            }
        }

        this.indexReadFailureIds = raw.indexReadFailures();
        this.dataReadFailureIds = raw.dataReadFailures();
        this.constructionFailureIds = Set.copyOf(constructionFailures);
    }

    public Optional<T> getIndex(Identifier id) {
        return Optional.ofNullable(indexes.get(id));
    }

    @UnmodifiableView
    public Map<Identifier, T> getAll() {
        return Collections.unmodifiableMap(indexes);
    }

    public Set<Identifier> getIndexReadFailureIds() {
        return indexReadFailureIds;
    }

    public Set<Identifier> getDataReadFailureIds() {
        return dataReadFailureIds;
    }

    public Set<Identifier> getConstructionFailureIds() {
        return constructionFailureIds;
    }

    /// > 返回本轮无法形成有效 Index 的 ID，包括 Index JSON 读取失败与构造失败。
    public Set<Identifier> getFailedIndexIds() {
        Set<Identifier> failedIds = new LinkedHashSet<>(indexReadFailureIds);
        failedIds.addAll(constructionFailureIds);
        return Collections.unmodifiableSet(failedIds);
    }
}
