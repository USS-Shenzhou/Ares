package cn.ussshenzhou.ares.gun;

import cn.ussshenzhou.ares.gun.item.AttachmentType;
import cn.ussshenzhou.ares.gun.resource.ICommonAssetsManager;
import cn.ussshenzhou.ares.gun.resource.IndexTagLookup;
import cn.ussshenzhou.ares.gun.custom.CommonAssetsManager;
import cn.ussshenzhou.ares.gun.custom.CustomItemIndex;
import cn.ussshenzhou.ares.gun.custom.CustomItemType;
import cn.ussshenzhou.ares.gun.custom.attachment.AttachmentIndex;
import cn.ussshenzhou.ares.gun.custom.charm.CharmData;
import cn.ussshenzhou.ares.gun.custom.charm.CharmIndex;
import cn.ussshenzhou.ares.gun.custom.gun.GunIndex;
import cn.ussshenzhou.ares.gun.custom.tag.IndexTagDomains;
import cn.ussshenzhou.ares.gun.init.ModCustomItemTypes;
import cn.ussshenzhou.ares.gun.init.ModRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Map;
import java.util.Optional;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW renamed from TimelessAPI; the client-only display getters are moved to TimelessHelperC.
public class TimelessHelperS {
    @Nullable
    public static AttachmentType getAttachmentType(Identifier id) {
        return ModRegistries.ATTACHMENT_TYPE_REGISTRY.getValue(id);
    }

    @Nullable
    public static CustomItemType<?,?> getCustomItemType(Identifier id) {
        return ModRegistries.CUSTOM_ITEM_TYPE_REGISTRY.getValue(id);
    }

    public static Registry<CustomItemType<?,?>> getCustomItemTypes() {
        return ModRegistries.CUSTOM_ITEM_TYPE_REGISTRY;
    }

    public static ICommonAssetsManager getCommonAssetsManager() {
        return CommonAssetsManager.get();
    }

    @Nonnull
    public static <T extends CustomItemIndex<D>, D> Optional<T> getIndex(Identifier id, CustomItemType<T, D> type) {
        return CommonAssetsManager.get().getIndex(id, type);
    }

    @UnmodifiableView
    public static <T extends CustomItemIndex<D>, D> Map<Identifier, T> getAllIndexes(CustomItemType<T, D> type) {
        return CommonAssetsManager.get().getAllIndexes(type);
    }

    /// > 获取指定语义域的索引标签查询器。
    @Nonnull
    public static IndexTagLookup getIndexTagLookup(Identifier domainId) {
        return CommonAssetsManager.get().getIndexTagLookup(domainId);
    }

    /// > 获取可安装配件（普通配件与下挂武器）的标签查询器。
    @Nonnull
    public static IndexTagLookup getAttachmentTagLookup() {
        return getIndexTagLookup(IndexTagDomains.ATTACHMENT);
    }

    @Nonnull
    public static Optional<GunIndex> getGunIndex(Identifier id) {
        return CommonAssetsManager.get().getIndex(id, ModCustomItemTypes.GUN.get());
    }

    @UnmodifiableView
    public static Map<Identifier, GunIndex> getAllGunIndexes() {
        return CommonAssetsManager.get().getAllIndexes(ModCustomItemTypes.GUN.get());
    }

    @Nonnull
    public static Optional<AttachmentIndex> getAttachmentIndex(Identifier id) {
        return CommonAssetsManager.get().getIndex(id, ModCustomItemTypes.ATTACHMENT.get());
    }

    @UnmodifiableView
    public static Map<Identifier, AttachmentIndex> getAllAttachmentIndexes() {
        return CommonAssetsManager.get().getAllIndexes(ModCustomItemTypes.ATTACHMENT.get());
    }

    /// > 取某挂饰 id 对应的数据驱动配置（物理参数 + 模型 layout）。双端可用（数据已同步到客户端）。
    @Nonnull
    public static Optional<CharmData> getCharmData(Identifier charmId) {
        if (charmId == null) {
            return Optional.empty();
        }
        return getIndex(charmId, CharmIndex.TYPE).map(CharmIndex::getData);
    }
}
