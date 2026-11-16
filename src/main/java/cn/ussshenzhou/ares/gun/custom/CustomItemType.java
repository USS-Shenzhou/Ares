package cn.ussshenzhou.ares.gun.custom;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Map;

/// > 代表一种数据驱动物品类型
/// >
/// > @param id 类型id
/// > @param indexPathConverter index文件路径映射
/// > @param dataPathConverter 配置数据文件路径映射
/// > @param factory index工厂
/// > @param gson gson实例
/// >
/// > @param <T> index实现类型
/// > @param <D> index的配置数据类型
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW createClientCache is removed together with the network sync.
public record CustomItemType<T extends CustomItemIndex<D>, D> (
        Identifier id,
        FileToIdConverter indexPathConverter,
        FileToIdConverter dataPathConverter,
        IndexFactory<T> factory,
        Gson gson
) {

    public CustomItemType(Identifier id, IndexFactory<T> factory, Gson gson) {
        this(id, FileToIdConverter.json("index/" + id.getPath()), FileToIdConverter.json("data/" + id.getPath()), factory, gson);
    }

    /// > index本身的配置数据解析器
    // FIXME REVIEW the parameter type changed from RegistryAccess to HolderLookup.Provider: the 26.3 server reload event only
    // provides HolderLookup.Provider, and the client RegistryAccess is a subtype. None of the 7 index factories uses it.
    @FunctionalInterface
    public interface IndexFactory<T> {
        /// > 创建一个index实例
        /// >
        /// > @param id             index的注册ID
        /// > @param json           index的json数据
        /// > @param dataMap        此index关联的所有配置数据的map
        /// > @param registryAccess
        /// > @return index实例，若创建失败则返回null
        @Nullable
        T create(Gson gson, Identifier id, JsonElement json, @UnmodifiableView Map<Identifier, JsonElement> dataMap, HolderLookup.Provider registryAccess);
    }

    public CustomIndexManager<T, D> createDataManager() {
        return new CustomIndexManager<>(
                this,
                "IndexesManager(" + id + ")"
        );
    }
}
