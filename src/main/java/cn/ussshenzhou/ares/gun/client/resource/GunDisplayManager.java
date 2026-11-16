package cn.ussshenzhou.ares.gun.client.resource;

import com.mojang.logging.LogUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import cn.ussshenzhou.ares.gun.resource.GunGsonHelper;
import cn.ussshenzhou.ares.gun.client.resource.display.GunDisplayInstance;
import cn.ussshenzhou.ares.gun.client.resource.display.GunDisplay;
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
public class GunDisplayManager extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    public static final Marker MARKER = MarkerFactory.getMarker("GunDisplayManager");

    private Map<Identifier, GunDisplayInstance> displayMap = Map.of();

    @Nonnull
    @Override
    protected Map<Identifier, JsonElement> prepare(@Nonnull ResourceManager manager, @Nonnull ProfilerFiller pProfiler) {
        return ResourceScanHelper.scanDirectory(manager, "display/guns", GunGsonHelper.GSON);
    }

    @Override
    protected void apply(@Nonnull Map<Identifier, JsonElement> resources, @Nonnull ResourceManager manager, @Nonnull ProfilerFiller pProfiler) {
        Map<Identifier, GunDisplayInstance> tempMap = Maps.newHashMap();
        for (var entry : resources.entrySet()) {
            try {
                var obj = GsonHelper.convertToJsonObject(entry.getValue(), "gun display");
                GunDisplay pojo = GunGsonHelper.GSON.fromJson(obj, GunDisplay.class);
                GunDisplayInstance data = GunDisplayInstance.of(pojo);

                tempMap.put(entry.getKey(), data);
            } catch (Exception e) {
                LogUtils.getLogger().error(MARKER, "Failed to load gun display: {}", entry.getKey(), e);
            }
        }
        displayMap = ImmutableMap.copyOf(tempMap);
    }
    
    public void putDisplay(Identifier id, GunDisplayInstance instance) {
        Map<Identifier, GunDisplayInstance> newMap = Maps.newHashMap(displayMap);
        newMap.put(id, instance);
        displayMap = ImmutableMap.copyOf(newMap);
    }

    public Map<Identifier, GunDisplayInstance> getDisplayMap() {
        return displayMap;
    }

    public Optional<GunDisplayInstance> getDisplay(Identifier id) {
        return Optional.ofNullable(displayMap.get(id));
    }
}
