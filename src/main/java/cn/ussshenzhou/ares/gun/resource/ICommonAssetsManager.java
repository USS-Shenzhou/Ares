package cn.ussshenzhou.ares.gun.resource;

import cn.ussshenzhou.ares.gun.attribute.GunAttribute;
import cn.ussshenzhou.ares.gun.custom.CustomItemIndex;
import cn.ussshenzhou.ares.gun.custom.CustomItemType;
import net.minecraft.resources.Identifier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Map;
import java.util.Optional;

/// @author TACZ, USS_Shenzhou
public interface ICommonAssetsManager {
    @Nonnull
    <T extends CustomItemIndex<D>, D> Optional<T> getIndex(Identifier id, CustomItemType<T, D> type);

    @UnmodifiableView
    <T extends CustomItemIndex<D>, D> Map<Identifier, T> getAllIndexes(CustomItemType<T, D> type);

    /// > 获取指定语义域的索引标签查询器。
    @Nonnull
    IndexTagLookup getIndexTagLookup(Identifier domainId);

    @Nullable
    GunAttribute getAttribute(Identifier id);
}
