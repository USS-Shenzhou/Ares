package cn.ussshenzhou.ares.gun.custom.slotadapter;

import com.google.gson.annotations.SerializedName;
import cn.ussshenzhou.ares.gun.attribute.GunAttributeModifier;
import cn.ussshenzhou.ares.gun.item.RailConfig;
import cn.ussshenzhou.ares.gun.custom.AttributeModifierMap;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// > 槽位适配器的数据（服务端权威逻辑层）。
/// > <p>
/// > 适配器是「依附于宿主槽位、不消耗独立物品」的中间层（例如垫高的导轨）。
/// > 安装后可改写槽位声明：放宽/收紧可安装类型（{@link #allowTypesOverride}）、附加或替换导轨微调
/// > （{@link #railOverride}）。几何与挂点偏移在 display 层声明。
/// > <p>
/// > v1：{@code modifiers} 字段已预留但暂不接入属性聚合（纯表现）。
/// @author TACZ, USS_Shenzhou
public class SlotAdapterData {
    /// > 安装本适配器后，对槽位 allow list 的覆盖。为空表示不改变原槽位的可安装类型。
    @SerializedName("allow_types")
    @Nullable
    public List<Identifier> allowTypesOverride = null;

    /// > 安装本适配器后，对槽位导轨微调配置的覆盖。为空表示沿用原槽位 rail。
    @SerializedName("rail")
    @Nullable
    public RailConfig railOverride = null;

    @SerializedName("modifiers")
    public AttributeModifierMap modifierMap = new AttributeModifierMap();

    public Map<Identifier, List<GunAttributeModifier>> getModifiers() {
        return modifierMap.getModifiers();
    }

    @Nullable
    public List<Identifier> getAllowTypesOverride() {
        return allowTypesOverride == null ? null : new ArrayList<>(allowTypesOverride);
    }

    @Nullable
    public RailConfig getRailOverride() {
        return railOverride;
    }
}
