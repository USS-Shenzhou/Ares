package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.gun.custom.charm.GunCharmData;
import cn.ussshenzhou.ares.gun.init.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// > 枪械「挂饰结合数据」NBT 读写的唯一集中点。
/// > <p>
/// > 按设计文档约定：本期单挂饰，但 NBT 结构为多挂饰预留——根键 {@code TaczCharms} 下用
/// > {@code charms} list 承载（本期长度恒为 1），读写按集合处理。后续放开多挂饰只需放开
/// > {@link #MAX_CHARMS} 上限，NBT 结构与遍历代码不动。跨版本 NBT 风险全部收敛于此类。
/// @author TACZ, USS_Shenzhou
public class GunCharmDataHelper {
    /// > 本期单挂饰上限。后续放开多挂饰时调大即可。
    public static final int MAX_CHARMS = 1;

    /// > 读取枪上所有挂饰结合数据（顺序与写入一致）。无则返回空列表。
    @Nonnull
    public static List<GunCharmData> getAll(ItemStack gunStack) {
        if (gunStack.isEmpty()) {
            return List.of();
        }
        return gunStack.getOrDefault(ModDataComponents.CHARMS, List.of());
    }

    /// > 读取枪上的第一个挂饰（本期主用）。无则返回空。
    public static Optional<GunCharmData> getFirst(ItemStack gunStack) {
        List<GunCharmData> all = getAll(gunStack);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }

    /// > 写入单个挂饰（覆盖既有 list，恒为 1 项）。
    public static void setSingle(ItemStack gunStack, GunCharmData data) {
        if (gunStack.isEmpty()) {
            return;
        }
        gunStack.set(ModDataComponents.CHARMS, List.of(data));
    }

    /// > 移除枪上所有挂饰，返回被移除的挂饰物品（用于归还玩家）。
    @Nonnull
    public static List<ItemStack> removeAll(ItemStack gunStack) {
        List<GunCharmData> all = getAll(gunStack);
        gunStack.remove(ModDataComponents.CHARMS);
        List<ItemStack> charms = new ArrayList<>(all.size());
        for (GunCharmData data : all) {
            charms.add(data.charmStack());
        }
        return charms;
    }
}
