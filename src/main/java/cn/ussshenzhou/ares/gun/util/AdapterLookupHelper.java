package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.gun.TimelessHelperS;
import cn.ussshenzhou.ares.gun.custom.slotadapter.SlotAdapterIndex;
import cn.ussshenzhou.ares.gun.custom.slotadapter.SlotAdapterTarget;
import cn.ussshenzhou.ares.gun.custom.variant.VariantIndex;
import cn.ussshenzhou.ares.gun.init.ModCustomItemTypes;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// > 变体 / 适配器的反查工具（双端共用）。
/// >
/// > 变体与适配器都是独立的数据驱动 index，通过 `targets` 定向到宿主。
/// > 本工具基于 [TimelessHelperS#getAllIndexes] 反查「某配件有哪些变体」「某宿主槽位有哪些适配器」，
/// > 天然支持跨包扩展（第三方包新增 index 即被纳入）。
/// >
/// > 这些 index 集合在每次资源重载时整体替换，规模很小（仅改装界面 / 校验时查询），故不额外建缓存。
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW renamed from AdapterRegistry.
public class AdapterLookupHelper {
    /// > 反查适用于指定配件的所有变体 index id。
    public static List<Identifier> getVariantsFor(Identifier attachmentId) {
        if (attachmentId == null) {
            return List.of();
        }
        List<Identifier> result = new ArrayList<>();
        Map<Identifier, VariantIndex> all = TimelessHelperS.getAllIndexes(ModCustomItemTypes.ATTACHMENT_VARIANT.get());
        for (var entry : all.entrySet()) {
            if (entry.getValue().appliesTo(attachmentId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /// > 指定变体 id 是否适用于指定配件（服务端权威校验用）。
    public static boolean isVariantValidFor(Identifier variantId, Identifier attachmentId) {
        if (variantId == null || attachmentId == null) {
            return false;
        }
        VariantIndex index = TimelessHelperS.getIndex(variantId, ModCustomItemTypes.ATTACHMENT_VARIANT.get()).orElse(null);
        return index != null && index.appliesTo(attachmentId);
    }

    /// > 反查适用于指定「宿主 + 槽位」的所有适配器 index id。
    public static List<Identifier> getAdaptersFor(Identifier hostId, Identifier slotId) {
        if (hostId == null || slotId == null) {
            return List.of();
        }
        List<Identifier> result = new ArrayList<>();
        Map<Identifier, SlotAdapterIndex> all = TimelessHelperS.getAllIndexes(ModCustomItemTypes.SLOT_ADAPTER.get());
        for (var entry : all.entrySet()) {
            if (entry.getValue().appliesTo(hostId, slotId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /// > 解析指定适配器在「宿主 + 槽位」上的唯一 target。不存在、重复或不适用时返回 null。
    @Nullable
    public static SlotAdapterTarget getAdapterTarget(Identifier adapterId, Identifier hostId,
                                                      Identifier slotId) {
        if (adapterId == null || hostId == null || slotId == null) {
            return null;
        }
        SlotAdapterIndex index = TimelessHelperS.getIndex(adapterId, ModCustomItemTypes.SLOT_ADAPTER.get()).orElse(null);
        return index == null ? null : index.getTarget(hostId, slotId);
    }

    /// > 指定适配器 id 是否适用于指定「宿主 + 槽位」（服务端权威校验用）。
    public static boolean isAdapterValidFor(Identifier adapterId, Identifier hostId, Identifier slotId) {
        return getAdapterTarget(adapterId, hostId, slotId) != null;
    }
}
