package cn.ussshenzhou.ares.gun.gameplay.timeline;

import net.minecraft.world.item.ItemStack;

/// > 出膛后编排的受控操作集：[FireController] 实现它，暴露给「出膛后编排脚本」的受控原语。
/// >
/// > 这是一个**纯 Java 内部能力接口**（不直接暴露给 JS），由 `shared/script/fire` 的
/// > `FireOrchestrationApi` 包装后交给脚本。所有操作都路由回 [FireController] 的既有编排方法，
/// > 因此**双端差异由 [FireController.Hooks] 注入的回调自动承载**（客户端表现/预测，服务端权威），
/// > 脚本与内置默认走同一套端适配，无需自行 `if (isClient)`。
/// >
/// > 世界副作用（`EffectApi`）绝不经此下放（见 `docs/scripting-boundaries.md` 第三节硬约束）。
/// @author TACZ, USS_Shenzhou
public interface FireAfterShotOps {

    /// > 起连发后续发序列（按当前 fire mode 的连发数与间隔）。等价内置「burst 分支」。
    void startBurst();

    /// > 起拉栓 / 补膛（manual 即时补膛，auto 起拉栓窗口）。等价内置单发模式的「拉栓分支」。
    void boltAfterShot();

    /// > 即时补膛（无窗口）：把一发从弹匣移入膛（客户端不写 NBT，服务端权威）。
    void rechamberNow();

    /// > 当前 fire mode 的连发数（1 表示单发）。
    int burstCount();

    /// > 该枪是否使用膛（closed/manual 为 true，open 为 false）。
    boolean usesChamber();

    /// > 内置默认判定：本发出膛后是否应拉栓（据 bolt 类型与余弹推导）。脚本可据此复刻/微调默认行为。
    boolean shouldBoltByDefault();

    /// > 当前手持物（供脚本查最新弹药状态；只读语义，脚本不应写它）。
    ItemStack gun();
}
