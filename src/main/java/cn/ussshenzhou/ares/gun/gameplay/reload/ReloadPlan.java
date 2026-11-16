package cn.ussshenzhou.ares.gun.gameplay.reload;

import cn.ussshenzhou.ares.gun.custom.attribute.GunPropertiesCaches;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import cn.ussshenzhou.ares.gun.util.AmmoDataHelper;
import cn.ussshenzhou.ares.gun.util.AmmoInventoryHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/// > 换弹的纯数据推导（无副作用，双端同源）。
/// >
/// > 负责换弹前置条件判断与相位时长推导。计时数字单向来自 [GunData]，
/// > 两端用同一套推导得到一致的时序骨架；副作用由编排层注入的 Step 任务执行。
/// @author TACZ, USS_Shenzhou
public class ReloadPlan {

    /// > 纯数据判断：当前枪械 + 玩家背包是否满足换弹前置条件（容量未满 + 背包有匹配弹药）。
    /// > 客户端用于预测，服务端启动换弹前也做同样检查。
    public static boolean canReload(Player player, ItemStack gun, GunData data) {
        int currentAmmo = AmmoDataHelper.getAmmoCount(gun);
        if (currentAmmo >= data.getMagazineCapacity()) {
            return false;
        }
        List<Identifier> reloadIds = AmmoInventoryHelper.resolveReloadAmmoIds(
                AmmoDataHelper.getAmmoId(gun), data.getAcceptedAmmoIds());
        return AmmoInventoryHelper.hasAmmoInInventory(player, reloadIds);
    }

    /// > 推导整个换弹动作的总时长（毫秒）。
    /// >
    /// > 客户端据此做纯倒计时表现与动画时长；服务端的逐发分相位时长由
    /// > [ServerReloadSteps] 独立推导（同源于 GunData）。
    /// >
    /// > @param player    玩家（用于逐发换弹估算可装入发数）
    /// >
    /// > @param gun       枪械物品
    /// >
    /// > @param data      枪械数据
    public static long totalDurationMs(Player player, ItemStack gun, GunData data) {
        int currentAmmo = AmmoDataHelper.getAmmoCount(gun);
        if ("shell_by_shell".equals(data.getReloadType()) && data.getShellReload() != null) {
            GunData.ShellReloadTiming t = data.getShellReload();
            int needed = Math.max(0, data.getMagazineCapacity() - currentAmmo);
            List<Identifier> reloadIds = AmmoInventoryHelper.resolveReloadAmmoIds(
                    AmmoDataHelper.getAmmoId(gun), data.getAcceptedAmmoIds());
            int available = AmmoInventoryHelper.countAmmoInInventory(player, reloadIds);
            int loops = Math.min(needed, available);
            boolean empty = currentAmmo <= 0;
            int introTicks = empty ? t.getIntroEmpty() : t.getIntro();
            /// > 空仓 intro 自带装一发，故循环次数 -1；非空仓 intro 不装弹
            int loopCount = empty ? Math.max(loops - 1, 0) : loops;
            return (introTicks + t.getLoop() * loopCount + t.getEnd()) * 50L;
        }
        int magLevel = GunPropertiesCaches.of(gun).getMagLevel();
        return data.getReloadDurationForLevel(magLevel) * 50L;
    }
}
