package cn.ussshenzhou.ares.gun.gameplay.timeline;

import net.minecraft.world.item.ItemStack;

/// > 出膛前门禁决策器：给定受控只读操作集 [FirePreShotOps] 与当前手持物，决定本次是否放行出膛。
/// >
/// > [FireController#canFire(ItemStack)] 在内置判定（弹药就绪 + trigger 非活跃 + 非 burst 闭锁 +
/// > 非拉栓中）通过后调用它做追加否决。默认实现（[#allowAll()]）恒放行，等价零脚本行为；
/// > 由 session 在声明了 `fire_plan_script` 且脚本含 `canFire` 时替换为脚本驱动实现。
/// >
/// > 这是**纯决策位**：只返回布尔，不产生任何世界副作用（见 `docs/scripting-boundaries.md` §3.3）。
/// > fire 双端本就不对称，脚本实现**允许**按 `ctx.isClient()` 分支；客户端误判由服务端权威同步纠正。
/// @author TACZ, USS_Shenzhou
@FunctionalInterface
public interface PreShotDecider {

    /// > 出膛前否决判定。
    /// >
    /// > @param ops 受控只读操作集（连发数 / 是否用膛 / 手持物查询）
    /// >
    /// > @param gun 当前手持物
    /// >
    /// > @return `true` 放行，`false` 拒绝本次出膛
    boolean allowFire(FirePreShotOps ops, ItemStack gun);

    /// > 默认门禁：恒放行。等价零脚本行为（内置判定通过即出膛）。
    static PreShotDecider allowAll() {
        return (ops, gun) -> true;
    }
}
