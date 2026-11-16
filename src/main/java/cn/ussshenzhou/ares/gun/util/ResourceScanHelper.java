package cn.ussshenzhou.ares.gun.util;

import com.mojang.logging.LogUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW renamed from ResourceScanner.
public class ResourceScanHelper {
    public record ScanResult<T>(Map<Identifier, T> values, Set<Identifier> failedIds) {
        public ScanResult {
            values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
            failedIds = Collections.unmodifiableSet(new LinkedHashSet<>(failedIds));
        }
    }

    private static JsonElement readLenientJson(Gson gson, Reader reader) throws IOException {
        JsonReader jsonReader = new JsonReader(reader);
        jsonReader.setStrictness(Strictness.LENIENT);
        JsonElement jsonElement = gson.getAdapter(JsonElement.class).read(jsonReader);
        if (jsonElement == null) {
            throw new JsonParseException("JSON data was null or empty");
        }
        return jsonElement;
    }

    /// > 扫描指定目录下的所有json文件<br/>
    /// > 与原版的scanDirectory方法的区别在于，查询结果是作为返回值返回的，而且允许注释<br/>
    /// > 对于相同的文件路径，只读取优先级最高的文件
    /// > @param pResourceManager 资源管理器
    /// > @param pName 目录名
    /// > @param pGson Gson实例
    /// > @return 扫描到的json文件
    public static Map<Identifier, JsonElement> scanDirectory(ResourceManager pResourceManager, String pName, Gson pGson) {
        return scanDirectory(pResourceManager, FileToIdConverter.json(pName), pGson);
    }

    public static Map<Identifier, JsonElement> scanDirectory(ResourceManager pResourceManager, FileToIdConverter filetoidconverter, Gson pGson) {
        return scanDirectoryWithFailures(pResourceManager, filetoidconverter, pGson).values();
    }

    public static ScanResult<JsonElement> scanDirectoryWithFailures(
            ResourceManager resourceManager,
            FileToIdConverter fileConverter,
            Gson gson
    ) {
        Map<Identifier, JsonElement> output = Maps.newHashMap();
        Set<Identifier> failedIds = new LinkedHashSet<>();
        for (Map.Entry<Identifier, Resource> entry : fileConverter.listMatchingResources(resourceManager).entrySet()) {
            Identifier resourceLocation = entry.getKey();
            Identifier id = fileConverter.fileToId(resourceLocation);

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement jsonElement = readLenientJson(gson, reader);
                JsonElement previous = output.put(id, jsonElement);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate data file ignored with ID " + id);
                }
            } catch (IllegalArgumentException | IOException | JsonParseException exception) {
                failedIds.add(id);
                LogUtils.getLogger().error("Couldn't parse data file {} from {}", id, resourceLocation, exception);
            }
        }
        return new ScanResult<>(output, failedIds);
    }

    /// > 扫描指定目录下的所有json文件<br/>
    /// > 与{@link #scanDirectory(ResourceManager, String, Gson)}不同的是，该方法会读取所有json文件作为列表返回
    /// > @param pResourceManager 资源管理器
    /// > @param filetoidconverter 文件路径和id的映射
    /// > @param pGson Gson实例
    /// > @return 扫描到的json文件
    public static Map<Identifier, List<JsonElement>> scanDirectoryAll(ResourceManager pResourceManager, FileToIdConverter filetoidconverter, Gson pGson) {
        return scanDirectoryAllWithFailures(pResourceManager, filetoidconverter, pGson).values();
    }

    public static ScanResult<List<JsonElement>> scanDirectoryAllWithFailures(
            ResourceManager resourceManager,
            FileToIdConverter fileConverter,
            Gson gson
    ) {
        Map<Identifier, List<JsonElement>> output = Maps.newHashMap();
        Set<Identifier> failedIds = new LinkedHashSet<>();
        for (Map.Entry<Identifier, List<Resource>> entry : fileConverter.listMatchingResourceStacks(resourceManager).entrySet()) {
            Identifier resourceLocation = entry.getKey();
            Identifier id = fileConverter.fileToId(resourceLocation);

            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonElement jsonElement = readLenientJson(gson, reader);
                    List<JsonElement> list = output.computeIfAbsent(id, ignored -> Lists.newArrayList());
                    list.add(jsonElement);
                } catch (IllegalArgumentException | IOException | JsonParseException exception) {
                    failedIds.add(id);
                    LogUtils.getLogger().error("Couldn't parse data file {} from {}", id, resourceLocation, exception);
                }
            }
        }
        return new ScanResult<>(output, failedIds);
    }
}
