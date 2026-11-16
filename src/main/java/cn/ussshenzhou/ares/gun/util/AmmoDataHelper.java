package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.gun.custom.attribute.GunPropertiesCaches;
import cn.ussshenzhou.ares.gun.init.ModDataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW unloadAmmo is removed (unloading is not ported).
public class AmmoDataHelper {
    public static int getAmmoCount(ItemStack gun) {
        if (gun.isEmpty()) {
            return 0;
        }
        return gun.getOrDefault(ModDataComponents.AMMO_COUNT, 0);
    }

    /// > 写入弹匣存量。余弹不是属性来源，故不触发属性缓存失效（{@link #setAmmoId} 才需要）。
    public static void setAmmoCount(ItemStack gun, int count) {
        if (gun.isEmpty()) {
            return;
        }
        gun.set(ModDataComponents.AMMO_COUNT, Math.max(0, count));
    }

    @Nullable
    public static Identifier getAmmoId(ItemStack gun) {
        if (gun.isEmpty()) {
            return null;
        }
        return gun.get(ModDataComponents.AMMO_ID);
    }

    /// > 写入当前弹种。弹种是属性修饰来源之一，写入方负责让属性缓存失效
    /// > （契约见 {@link GunPropertiesCaches}）。
    public static void setAmmoId(ItemStack gun, Identifier ammoId) {
        if (gun.isEmpty()) {
            return;
        }
        gun.set(ModDataComponents.AMMO_ID, ammoId);
        GunPropertiesCaches.invalidateCache(gun);
    }

    /// > 清除当前弹种。同样影响属性计算，写入方负责让属性缓存失效。
    public static void clearAmmoId(ItemStack gun) {
        if (gun.isEmpty()) {
            return;
        }
        gun.remove(ModDataComponents.AMMO_ID);
        GunPropertiesCaches.invalidateCache(gun);
    }

    /// > 是否处于待击发就绪状态（膛内有弹）。
    /// > 仅 closed/manual bolt 类型有意义；open 类型不使用该状态。
    /// > 该状态仅由服务端写入，客户端只读用于预判与表现。
    public static boolean isChambered(ItemStack gun) {
        if (gun.isEmpty()) {
            return false;
        }
        return gun.getOrDefault(ModDataComponents.CHAMBERED, false);
    }

    public static void setChambered(ItemStack gun, boolean chambered) {
        if (gun.isEmpty()) {
            return;
        }
        gun.set(ModDataComponents.CHAMBERED, chambered);
    }

    public static void clearChambered(ItemStack gun) {
        if (gun.isEmpty()) {
            return;
        }
        gun.remove(ModDataComponents.CHAMBERED);
    }

    public static boolean hasAmmo(ItemStack gun) {
        return getAmmoCount(gun) > 0;
    }

    /// > 总持有弹数 = 弹匣存量 + 膛内（若 usesChamber 且已上膛）。
    /// > open 类型传 usesChamber=false，AmmoCount 即总量。
    public static int getTotalRounds(ItemStack gun, boolean usesChamber) {
        int count = getAmmoCount(gun);
        if (usesChamber && isChambered(gun)) {
            count++;
        }
        return count;
    }

    public static int consumeAmmo(ItemStack gun, int amount) {
        int current = getAmmoCount(gun);
        int consumed = Math.min(current, amount);
        setAmmoCount(gun, current - consumed);
        return consumed;
    }
}
