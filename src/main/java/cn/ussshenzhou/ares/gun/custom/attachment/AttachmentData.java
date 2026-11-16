package cn.ussshenzhou.ares.gun.custom.attachment;

import com.google.gson.annotations.SerializedName;
import cn.ussshenzhou.ares.gun.attribute.GunAttributeModifier;
import cn.ussshenzhou.ares.gun.custom.AttachmentSlotInfo;
import cn.ussshenzhou.ares.gun.custom.AttributeModifierMap;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// @author TACZ, USS_Shenzhou
public class AttachmentData {
    @SerializedName("modifiers")
    public AttributeModifierMap modifierMap = new AttributeModifierMap();

    @SerializedName("extended_mag_level")
    private int extendedMagLevel = 0;

    /// > 原厂配件标记。为 true 时该配件没有独立 display/模型，
    /// > 其几何寄生在宿主枪模型的 default_model_group 骨骼中，子槽位也复用宿主枪。
    /// > 逻辑上与普通配件无异（计入属性、可替换、可挂子配件）。
    @SerializedName("builtin")
    private boolean builtin = false;

    /// > 配件自身提供的子配件槽位。任意配件只要定义了槽位即可作为容器挂载子配件。
    @SerializedName("attachment_slots")
    private AttachmentSlotInfo attachmentSlots = new AttachmentSlotInfo();

    /// > 该配件安装后需要禁用的父级（枪械）槽位列表。
    @SerializedName("disable_parent_slots")
    private List<Identifier> disableParentSlots = new ArrayList<>();

    public Map<Identifier, List<GunAttributeModifier>> getModifiers() {
        return modifierMap.getModifiers();
    }

    public int getExtendedMagLevel() {
        return extendedMagLevel;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public AttachmentSlotInfo getAttachmentSlots() {
        return attachmentSlots;
    }

    public List<Identifier> getDisableParentSlots() {
        return disableParentSlots;
    }
}
