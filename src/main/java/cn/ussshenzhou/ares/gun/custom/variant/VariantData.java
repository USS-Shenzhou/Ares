package cn.ussshenzhou.ares.gun.custom.variant;

import com.google.gson.annotations.SerializedName;
import cn.ussshenzhou.ares.gun.attribute.GunAttributeModifier;
import cn.ussshenzhou.ares.gun.custom.AttributeModifierMap;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

/// > 配件变体的数据（服务端权威逻辑层）。
/// > <p>
/// > 变体是「依附于配件、不消耗独立物品」的几何中间层（例如带增高座的瞄具）。
/// > 本数据描述变体携带的逻辑修饰；几何与挂点重定位在 display 层声明。
/// > <p>
/// > v1：{@code modifiers} 字段已预留但暂不接入属性聚合（纯表现）。
/// @author TACZ, USS_Shenzhou
public class VariantData {
    @SerializedName("modifiers")
    public AttributeModifierMap modifierMap = new AttributeModifierMap();

    public Map<Identifier, List<GunAttributeModifier>> getModifiers() {
        return modifierMap.getModifiers();
    }
}
