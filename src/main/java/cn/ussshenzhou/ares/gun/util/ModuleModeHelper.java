package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.util.GunModuleMode;
import cn.ussshenzhou.ares.gun.init.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/// > 枪械模块模式（主枪 / 下挂）的 NBT 读写工具。
/// >
/// > 模块模式属于**宿主枪械自身**的状态，存于宿主 NBT。服务端权威写入后随物品栏同步自动下发到客户端，
/// > 客户端读取同一份 NBT，避免主枪 / 下挂模式在两端各持一份字段时因丢包或会话重建而失同步。
/// > 客户端可本地预写做预判，下一次服务端同步会以权威值覆盖自愈。
/// @author TACZ, USS_Shenzhou
public class ModuleModeHelper {
    public static GunModuleMode getModuleMode(ItemStack gun) {
        if (gun.isEmpty()) {
            return GunModuleMode.PRIMARY;
        }
        return gun.getOrDefault(ModDataComponents.MODULE_MODE, GunModuleMode.PRIMARY);
    }

    public static void setModuleMode(ItemStack gun, GunModuleMode mode) {
        if (gun.isEmpty()) {
            return;
        }
        gun.set(ModDataComponents.MODULE_MODE, mode);
    }
}
