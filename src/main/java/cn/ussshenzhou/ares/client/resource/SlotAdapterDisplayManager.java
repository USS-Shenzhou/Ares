package cn.ussshenzhou.ares.client.resource;

import com.mojang.logging.LogUtils;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import cn.ussshenzhou.ares.gun.resource.GunGsonHelper;
import cn.ussshenzhou.ares.client.resource.display.SlotAdapterDisplayInstance;
import cn.ussshenzhou.ares.client.resource.display.SlotAdapterDisplay;
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

/// > 槽位适配器 display 的资源重载管理器，扫描 `display/slot_adapters` 目录。
/// @author TACZ, USS_Shenzhou
public class SlotAdapterDisplayManager extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    public static final Marker MARKER = MarkerFactory.getMarker("SlotAdapterDisplayManager");

    private Map<Identifier, SlotAdapterDisplayInstance> displayMap = Map.of();

    @Nonnull
    @Override
    protected Map<Identifier, JsonElement> prepare(@Nonnull ResourceManager manager, @Nonnull ProfilerFiller pProfiler) {
        return ResourceScanHelper.scanDirectory(manager, "display/slot_adapters", GunGsonHelper.GSON);
    }

    @Override
    protected void apply(@Nonnull Map<Identifier, JsonElement> resources, @Nonnull ResourceManager manager, @Nonnull ProfilerFiller pProfiler) {
        ImmutableMap.Builder<Identifier, SlotAdapterDisplayInstance> builder = ImmutableMap.builder();
        for (var entry : resources.entrySet()) {
            try {
                var obj = GsonHelper.convertToJsonObject(entry.getValue(), "slot adapter display");
                SlotAdapterDisplay pojo = GunGsonHelper.GSON.fromJson(obj, SlotAdapterDisplay.class);
                SlotAdapterDisplayInstance data = SlotAdapterDisplayInstance.of(pojo);
                builder.put(entry.getKey(), data);
            } catch (Exception e) {
                LogUtils.getLogger().error(MARKER, "Failed to load slot adapter display: {}", entry.getKey(), e);
            }
        }
        displayMap = builder.build();
    }

    public Map<Identifier, SlotAdapterDisplayInstance> getDisplayMap() {
        return displayMap;
    }

    public Optional<SlotAdapterDisplayInstance> getDisplay(Identifier id) {
        return Optional.ofNullable(displayMap.get(id));
    }
}
