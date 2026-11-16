package cn.ussshenzhou.ares.gun.attribute;

import cn.ussshenzhou.ares.Ares;
import net.minecraft.resources.Identifier;

/// > 内置默认属性id
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW removed the two explosion attributes.
public class GunAttributes {
    public static final Identifier ADS                  = Ares.modLoc("ads");
    public static final Identifier AMMO_SPEED           = Ares.modLoc("ammo_speed");
    public static final Identifier ARMOR_IGNORE         = Ares.modLoc("armor_ignore");
    public static final Identifier DAMAGE               = Ares.modLoc("damage");
    public static final Identifier HEADSHOT_MULTIPLIER  = Ares.modLoc("headshot_multiplier");

    public static final Identifier STAND_INACCURACY     = Ares.modLoc("inaccuracy_stand");
    public static final Identifier AIM_INACCURACY       = Ares.modLoc("inaccuracy_aim");
    /// > 战术拒枪姿态的瞄准散布（本体第二瞄位）。
    public static final Identifier TACTICAL_INACCURACY  = Ares.modLoc("inaccuracy_tactical");
    public static final Identifier MOVE_INACCURACY      = Ares.modLoc("inaccuracy_move");
    public static final Identifier SNEAK_INACCURACY     = Ares.modLoc("inaccuracy_sneak");
    public static final Identifier LIE_INACCURACY       = Ares.modLoc("inaccuracy_lie");

    public static final Identifier KNOCKBACK            = Ares.modLoc("knockback");

    public static final Identifier PENETRATION          = Ares.modLoc("penetration");
    public static final Identifier LIMB_MULTIPLIER      = Ares.modLoc("limb_multiplier");
    public static final Identifier PIERCE_DECAY         = Ares.modLoc("pierce_decay");

    public static final Identifier PITCH_RECOIL         = Ares.modLoc("recoil_pitch");
    public static final Identifier YAW_RECOIL           = Ares.modLoc("recoil_yaw");

    public static final Identifier RPM                  = Ares.modLoc("rpm");
    public static final Identifier PIERCE               = Ares.modLoc("pierce");
    public static final Identifier WEIGHT               = Ares.modLoc("weight");
}
