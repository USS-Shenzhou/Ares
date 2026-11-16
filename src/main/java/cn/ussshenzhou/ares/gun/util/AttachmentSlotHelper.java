package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.gun.TimelessHelperS;
import cn.ussshenzhou.ares.gun.item.AttachmentSlot;
import cn.ussshenzhou.ares.gun.item.AttachmentType;
import cn.ussshenzhou.ares.gun.item.RailConfig;
import cn.ussshenzhou.ares.gun.custom.slotadapter.SlotAdapterData;
import cn.ussshenzhou.ares.gun.custom.slotadapter.SlotAdapterIndex;
import cn.ussshenzhou.ares.gun.init.ModCustomItemTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// > 槽位适配器对槽位声明的「覆盖」纯逻辑（双端共用，服务端权威校验复用）。
/// > <p>
/// > 适配器不改变槽位 id / name / default / required，只可能改写：
/// > <ul>
/// >   <li>{@code allowTypes}：放宽或收紧可安装类型；</li>
/// >   <li>{@code rail}：附加或替换导轨微调配置。</li>
/// > </ul>
/// > 应用后返回一个新的有效 {@link AttachmentSlot}，供安装校验 / 渲染挂点 / 屏幕投影统一使用。
/// @author TACZ, USS_Shenzhou
public class AttachmentSlotHelper {
    /// > 对整组槽位应用 holder 上已选的适配器覆盖，返回新的有效槽位映射。
    /// > <p>
    /// > holder 未写入任何适配器选择时，原样返回 slots，零额外开销。
    /// >
    /// > @param holder 容纳这些槽位的宿主物品堆（枪 / 父配件）
    /// > @param slots  原始槽位映射
    /// > @return 应用适配器覆盖后的槽位映射（无变化时返回原映射）
    public static Map<Identifier, AttachmentSlot> applyAdapters(ItemStack holder,
                                                                      Map<Identifier, AttachmentSlot> slots) {
        if (holder == null || holder.isEmpty() || slots.isEmpty()) {
            return slots;
        }
        Identifier hostId = CustomItemDataHelper.getCustomId(holder);
        Map<Identifier, AttachmentSlot> result = null;
        for (var entry : slots.entrySet()) {
            Identifier adapterId = SlotAdapterHelper.getAdapter(holder, entry.getKey());
            if (adapterId == null || AdapterLookupHelper.getAdapterTarget(adapterId, hostId, entry.getKey()) == null) {
                continue;
            }
            AttachmentSlot applied = apply(entry.getValue(), adapterId);
            if (applied != entry.getValue()) {
                if (result == null) {
                    result = new LinkedHashMap<>(slots);
                }
                result.put(entry.getKey(), applied);
            }
        }
        return result == null ? slots : result;
    }

    /// > 把指定适配器对槽位的覆盖应用到 base 槽位上，返回有效槽位。
    /// > <p>
    /// > adapterId 为空、对应 index 不存在、或无任何覆盖时，原样返回 base。
    /// >
    /// > @param base      原始槽位声明
    /// > @param adapterId 选中的适配器 id（可空）
    /// > @return 应用覆盖后的有效槽位（无变化时返回 base 本身）
    public static AttachmentSlot apply(AttachmentSlot base, Identifier adapterId) {
        if (base == null || adapterId == null) {
            return base;
        }
        SlotAdapterIndex index = TimelessHelperS.getIndex(adapterId, ModCustomItemTypes.SLOT_ADAPTER.get()).orElse(null);
        if (index == null) {
            return base;
        }
        SlotAdapterData data = index.getData();
        if (data == null) {
            return base;
        }
        return apply(base, data);
    }

    /// > 把适配器数据对槽位的覆盖应用到 base 槽位上，返回有效槽位。
    public static AttachmentSlot apply(AttachmentSlot base, SlotAdapterData data) {
        if (base == null || data == null) {
            return base;
        }

        List<AttachmentType> allowTypes = resolveAllowTypes(base, data.getAllowTypesOverride());
        RailConfig rail = data.getRailOverride() != null ? data.getRailOverride() : base.rail();

        /// > 无任何变化时返回原对象，避免无谓分配
        if (allowTypes == base.allowTypes() && rail == base.rail()) {
            return base;
        }

        return new AttachmentSlot(
                base.id(),
                base.name(),
                allowTypes,
                base.storeTag(),
                base.allowAttachmentTag(),
                base.defaultAttachment(),
                base.required(),
                rail
        );
    }

    private static List<AttachmentType> resolveAllowTypes(AttachmentSlot base, List<Identifier> override) {
        if (override == null) {
            return base.allowTypes();
        }
        List<AttachmentType> result = new ArrayList<>();
        for (Identifier typeId : override) {
            AttachmentType type = TimelessHelperS.getAttachmentType(typeId);
            if (type != null) {
                result.add(type);
            }
        }
        return result;
    }
}
