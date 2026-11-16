package cn.ussshenzhou.ares.gun.gameplay.timeline;

import net.minecraft.world.item.ItemStack;

/// > 出膛前否决的受控只读操作集：[FireController] 实现它，暴露给「出膛前门禁脚本」的只读查询。
/// >
/// > 这是一个**纯 Java 内部能力接口**（不直接暴露给 JS），由 `shared/script/fire` 的
/// > `FirePreShotApi` 包装后交给脚本。与 [FireAfterShotOps] 对称，但语义相反：
/// > afterShot 是出膛**后**的动作编排（有编排原语），preShot 是出膛**前**的**纯决策**——
/// > 只提供只读查询、**不含任何编排原语与副作用**。脚本据此返回 accept/block。
/// >
/// > 世界副作用（`EffectApi`）绝不经此下放（见 `docs/scripting-boundaries.md` 第三节硬约束）。
/// @author TACZ, USS_Shenzhou
public interface FirePreShotOps {

    /// > 当前 fire mode 的连发数（1 表示单发）。
    int burstCount();

    /// > 该枪是否使用膛（closed/manual 为 true，open 为 false）。
    boolean usesChamber();

    /// > 当前手持物（供脚本查最新弹药状态；只读语义，脚本不应写它）。
    ItemStack gun();
}
