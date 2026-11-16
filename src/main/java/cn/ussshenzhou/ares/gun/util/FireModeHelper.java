package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.gun.custom.attribute.GunPropertiesCaches;
import cn.ussshenzhou.ares.gun.custom.gun.FireModeEntry;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import cn.ussshenzhou.ares.gun.init.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

import java.util.List;

/// @author TACZ, USS_Shenzhou
public class FireModeHelper {
    public static int getFireModeIndex(ItemStack gun) {
        if (gun.isEmpty()) {
            return 0;
        }
        return gun.getOrDefault(ModDataComponents.FIRE_MODE_INDEX, 0);
    }

    /// > 写入开火模式索引。开火模式是属性修饰来源之一，写入方负责让属性缓存失效
    /// > （契约见 [GunPropertiesCaches]）。
    public static void setFireModeIndex(ItemStack gun, int index) {
        if (gun.isEmpty()) {
            return;
        }
        gun.set(ModDataComponents.FIRE_MODE_INDEX, index);
        GunPropertiesCaches.invalidateCache(gun);
    }

    @Nullable
    public static FireModeEntry getCurrentFireMode(ItemStack gun, GunData data) {
        List<FireModeEntry> modes = data.getFireModes();
        if (modes.isEmpty()) {
            return null;
        }
        int index = getFireModeIndex(gun);
        if (index < 0 || index >= modes.size()) {
            index = 0;
        }
        return modes.get(index);
    }

    /// > 当前开火模式是否允许按住连续射击。
    /// > 找不到模式数据时默认返回 true（全自动）。
    public static boolean isCurrentModeAuto(ItemStack gun) {
        if (gun.isEmpty()) {
            return true;
        }
        if (!(gun.getItem() instanceof cn.ussshenzhou.ares.gun.item.IGun iGun)) {
            return true;
        }
        GunData data = iGun.getIndex(gun).map(cn.ussshenzhou.ares.gun.custom.gun.GunIndex::getData).orElse(null);
        if (data == null) {
            return true;
        }
        FireModeEntry mode = getCurrentFireMode(gun, data);
        return mode == null || mode.isAuto();
    }

    /// > 当前触发方式是否需要「松手发射」（非 instant，即 charge / delayed / 脚本 trigger）。
    /// > 用于客户端按键：需要松手发射时在扳机 falling-edge 调 `op.fireRelease(hand)`。
    /// > 找不到模式数据时默认返回 false（即时触发，无需松手发射）。
    public static boolean currentTriggerNeedsRelease(ItemStack gun) {
        if (gun.isEmpty()) {
            return false;
        }
        if (!(gun.getItem() instanceof cn.ussshenzhou.ares.gun.item.IGun iGun)) {
            return false;
        }
        GunData data = iGun.getIndex(gun).map(cn.ussshenzhou.ares.gun.custom.gun.GunIndex::getData).orElse(null);
        if (data == null) {
            return false;
        }
        FireModeEntry mode = getCurrentFireMode(gun, data);
        if (mode == null) {
            return false;
        }
        String trigger = mode.getTrigger();
        return trigger != null && !"instant".equals(trigger);
    }
}
