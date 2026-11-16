package cn.ussshenzhou.ares.client.resource;

import com.mojang.logging.LogUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import cn.ussshenzhou.ares.gun.resource.GunGsonHelper;
import cn.ussshenzhou.ares.client.charm.render.CharmCollisionExtractor;
import cn.ussshenzhou.ares.client.charm.render.CharmLayoutGeometry;
import cn.ussshenzhou.ares.client.charm.render.CharmRenderStateCache;
import cn.ussshenzhou.ares.client.resource.display.CharmDisplayInstance;
import cn.ussshenzhou.ares.client.resource.display.CharmDisplay;
import cn.ussshenzhou.ares.gun.util.ResourceScanHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import javax.annotation.Nonnull;

import java.util.Map;
import java.util.Optional;

/// @author TACZ, USS_Shenzhou
public class CharmDisplayManager extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    public static final Marker MARKER = MarkerFactory.getMarker("CharmDisplayManager");

    private Map<Identifier, CharmDisplayInstance> displayMap = Map.of();

    @Nonnull
    @Override
    protected Map<Identifier, JsonElement> prepare(@Nonnull ResourceManager manager, @Nonnull ProfilerFiller pProfiler) {
        return ResourceScanHelper.scanDirectory(manager, "display/charms", GunGsonHelper.GSON);
    }

    @Override
    protected void apply(@Nonnull Map<Identifier, JsonElement> resources, @Nonnull ResourceManager manager, @Nonnull ProfilerFiller pProfiler) {
        /// > 资源重载会重建挂饰模型实例，清空依赖旧模型的分层几何与物理状态缓存
        CharmLayoutGeometry.clearCache();
        CharmRenderStateCache.clear();
        CharmCollisionExtractor.clearCache();
        Map<Identifier, CharmDisplayInstance> tempMap = Maps.newHashMap();
        for (var entry : resources.entrySet()) {
            try {
                var obj = GsonHelper.convertToJsonObject(entry.getValue(), "charm display");
                CharmDisplay pojo = GunGsonHelper.GSON.fromJson(obj, CharmDisplay.class);
                CharmDisplayInstance data = CharmDisplayInstance.of(pojo);
                tempMap.put(entry.getKey(), data);
            } catch (Exception e) {
                LogUtils.getLogger().error(MARKER, "Failed to load charm display: {}", entry.getKey(), e);
            }
        }
        displayMap = ImmutableMap.copyOf(tempMap);
        LogUtils.getLogger().info(MARKER, "Loaded {} charm displays", displayMap.size());
    }

    public void putDisplay(Identifier id, CharmDisplayInstance instance) {
        Map<Identifier, CharmDisplayInstance> newMap = Maps.newHashMap(displayMap);
        newMap.put(id, instance);
        displayMap = ImmutableMap.copyOf(newMap);
    }

    public Map<Identifier, CharmDisplayInstance> getDisplayMap() {
        return displayMap;
    }

    public Optional<CharmDisplayInstance> getDisplay(Identifier id) {
        return Optional.ofNullable(displayMap.get(id));
    }
}
