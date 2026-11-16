package cn.ussshenzhou.ares.gun.init;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.item.AttachmentKind;
import cn.ussshenzhou.ares.gun.item.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/// @author TACZ, USS_Shenzhou
public class ModAttachmentTypes {
    public static final DeferredRegister<AttachmentType> ATTACHMENT_TYPE =
            DeferredRegister.create(ModRegistries.ATTACHMENT_TYPE, Ares.MOD_ID);

    public static DeferredHolder<AttachmentType, AttachmentType> SCOPE = register("scope", AttachmentKind.NORMAL);
    public static DeferredHolder<AttachmentType, AttachmentType> MUZZLE = register("muzzle", AttachmentKind.NORMAL);
    public static DeferredHolder<AttachmentType, AttachmentType> STOCK = register("stock", AttachmentKind.NORMAL);
    public static DeferredHolder<AttachmentType, AttachmentType> GRIP = register("grip", AttachmentKind.NORMAL);
    public static DeferredHolder<AttachmentType, AttachmentType> LASER = register("laser", AttachmentKind.NORMAL);
    public static DeferredHolder<AttachmentType, AttachmentType> EXTENDED_MAG = register("extended_mag", AttachmentKind.NORMAL);
    public static DeferredHolder<AttachmentType, AttachmentType> LIGHT = register("light", AttachmentKind.NORMAL);

    /// > 导轨/枪管套件类配件：作为容器，可挂载子配件（普通节点，深度由统一约束控制）
    public static DeferredHolder<AttachmentType, AttachmentType> KIT_BARREL = register("kit_barrel", AttachmentKind.NORMAL);

    /// > Under-barrel / mounted weapon. Leaf node (cannot hold other attachments).
    public static DeferredHolder<AttachmentType, AttachmentType> UNDER_BARREL = register("under_barrel", AttachmentKind.MOUNTED_WEAPON);

    /// > Placeholder type.
    public static DeferredHolder<AttachmentType, AttachmentType> NONE = register("none", AttachmentKind.NORMAL);

    private static DeferredHolder<AttachmentType, AttachmentType> register(String type, AttachmentKind kind) {
        return ATTACHMENT_TYPE.register(type, () -> new AttachmentType(Ares.modLoc(type), kind));
    }
}
