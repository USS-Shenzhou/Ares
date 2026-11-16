package cn.ussshenzhou.ares.gun.resource;

import com.mojang.logging.LogUtils;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import cn.ussshenzhou.ares.gun.util.ResourceScanHelper;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import javax.annotation.Nonnull;

import java.util.Map;

/// > 简单数据管理器
/// >
/// > 从资源包/数据包中读取json文件并解析为数据
/// >
/// > @param `T` 数据类型
/// @author TACZ, USS_Shenzhou
public class JsonDataManager<T> extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    protected Map<Identifier, T> dataMap = Maps.newHashMap();

    private final Gson gson;
    private final Class<T> dataClass;
    private final Marker marker;

    private final FileToIdConverter fileToIdConverter;

    public JsonDataManager(Class<T> dataClass, Gson pGson, String directory, String marker) {
        this(dataClass, pGson, FileToIdConverter.json(directory), marker);
    }

    public JsonDataManager(Class<T> dataClass, Gson pGson, FileToIdConverter fileToIdConverter, String marker) {
        this.gson = pGson;
        this.dataClass = dataClass;
        // FIXME REVIEW log4j Marker replaced with the slf4j MarkerFactory along with LogUtils.getLogger(); same in
        // CustomIndexManager and the client display managers.
        this.marker = MarkerFactory.getMarker(marker);
        this.fileToIdConverter = fileToIdConverter;
    }

    @Nonnull
    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        return ResourceScanHelper.scanDirectory(pResourceManager, fileToIdConverter, this.gson);
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        dataMap.clear();
        for (Map.Entry<Identifier, JsonElement> entry : pObject.entrySet()) {
            Identifier id = entry.getKey();
            JsonElement element = entry.getValue();
            try {
                T data = parseJson(element);
                if (data != null) {
                    dataMap.put(id, data);
                }
            } catch (JsonParseException | IllegalArgumentException e) {
                LogUtils.getLogger().error(marker, "Failed to load data file {}", id, e);
            }
        }
    }

    protected T parseJson(JsonElement element) {
        return gson.fromJson(element, getDataClass());
    }

    public Class<T> getDataClass() {
        return dataClass;
    }

    public Marker getMarker() {
        return marker;
    }

    public Gson getGson() {
        return gson;
    }

    public T getData(Identifier id) {
        return dataMap.get(id);
    }
    
    public void putData(Identifier id, T data) {
        dataMap.put(id, data);
    }

    public Map<Identifier, T> getAllData() {
        return dataMap;
    }

    public void clearData() {
        dataMap.clear();
    }
}
