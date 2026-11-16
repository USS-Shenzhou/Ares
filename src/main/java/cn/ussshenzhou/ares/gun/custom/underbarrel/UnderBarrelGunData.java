package cn.ussshenzhou.ares.gun.custom.underbarrel;

import com.google.gson.annotations.SerializedName;
import cn.ussshenzhou.ares.gun.attribute.GunAttributeModifier;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import cn.ussshenzhou.ares.gun.custom.AttachmentSlotInfo;
import cn.ussshenzhou.ares.gun.custom.AttributeModifierMap;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

/// > Data for under-barrel / mounted weapons.
/// >
/// > Design constraints:
/// > - Cannot install any attachments (slots are always empty).
/// > - May be usable as a standalone weapon when standalone=true.
/// @author TACZ, USS_Shenzhou
public class UnderBarrelGunData extends GunData {
    @SerializedName("standalone")
    private boolean standalone = false;

    @SerializedName("modifiers")
    public AttributeModifierMap modifierMap = new AttributeModifierMap();

    @Override
    public AttachmentSlotInfo getAttachmentSlots() {
        /// > Mounted weapons are leaf nodes.
        return new AttachmentSlotInfo();
    }

    public boolean isStandalone() {
        return standalone;
    }

    public Map<Identifier, List<GunAttributeModifier>> getModifiers() {
        return modifierMap.getModifiers();
    }
}
