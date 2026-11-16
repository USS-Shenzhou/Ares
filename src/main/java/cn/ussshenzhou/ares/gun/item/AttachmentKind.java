package cn.ussshenzhou.ares.gun.item;

/// > High-level role of an attachment type.
/// >
/// > NORMAL: regular attachment (scope/muzzle/grip...). May optionally carry sub-slots
/// > (defined via attachment data) to act as a container for child attachments.
/// > MOUNTED_WEAPON: under-barrel / mounted weapon; leaf node (cannot hold anything),
/// > and may optionally be usable as a standalone weapon.
/// @author TACZ, USS_Shenzhou
public enum AttachmentKind {
    NORMAL,
    MOUNTED_WEAPON
}
