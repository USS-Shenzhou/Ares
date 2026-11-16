package cn.ussshenzhou.ares.gun.client.resource;

import com.mojang.logging.LogUtils;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import cn.ussshenzhou.ares.gun.resource.GunGsonHelper;
import cn.ussshenzhou.ares.gun.client.resource.display.AttachmentDisplayInstance;
import cn.ussshenzhou.ares.gun.client.resource.display.AttachmentDisplay;
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
public class AttachmentDisplayManager extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    public static final Marker MARKER = MarkerFactory.getMarker("AttachmentDisplayManager");

    private final String directory;
    private Map<Identifier, AttachmentDisplayInstance> displayMap = Map.of();

    public AttachmentDisplayManager() {
        this("display/attachments");
    }

    /// > @param directory 扫描的资源目录。变体复用同一套 display 结构，只是目录不同
    /// >                  （如 `display/attachment_variants`）。
    public AttachmentDisplayManager(String directory) {
        this.directory = directory;
    }

    @Nonnull
    @Override
    protected Map<Identifier, JsonElement> prepare(@Nonnull ResourceManager manager, @Nonnull ProfilerFiller pProfiler) {
        return ResourceScanHelper.scanDirectory(manager, directory, GunGsonHelper.GSON);
    }

    @Override
    protected void apply(@Nonnull Map<Identifier, JsonElement> resources, @Nonnull ResourceManager manager, @Nonnull ProfilerFiller pProfiler) {
        ImmutableMap.Builder<Identifier, AttachmentDisplayInstance> builder = ImmutableMap.builder();
        for (var entry : resources.entrySet()) {
            try {
                var obj = GsonHelper.convertToJsonObject(entry.getValue(), "attachment display");
                AttachmentDisplay pojo = GunGsonHelper.GSON.fromJson(obj, AttachmentDisplay.class);
                AttachmentDisplayInstance data = AttachmentDisplayInstance.of(pojo);

                builder.put(entry.getKey(), data);
            } catch (Exception e) {
                LogUtils.getLogger().error(MARKER, "Failed to load attachment display: {}", entry.getKey(), e);
            }
        }
        displayMap = builder.build();
    }

    public Map<Identifier, AttachmentDisplayInstance> getDisplayMap() {
        return displayMap;
    }

    public Optional<AttachmentDisplayInstance> getDisplay(Identifier id) {
        return Optional.ofNullable(displayMap.get(id));
    }
}
