package cn.ussshenzhou.ares.gun.gameplay.timeline;

/// > 出膛后编排决策器：给定受控操作集 {@link FireAfterShotOps} 与内置默认建议，决定本发出膛后如何编排
/// > （起 burst / 拉栓 / 补膛 / 什么都不做）。
/// > <p>
/// > {@link FireController} 在出膛成功后调用它取代内置默认分支。默认实现（{@link #builtin()}）复刻原行为
/// > （连发→burst，单发→按建议拉栓），由 session 在声明了 {@code fire_plan_script} 时替换为脚本驱动实现。
/// > <p>
/// > 双端差异不由本决策器承载——它只调 {@link FireAfterShotOps} 的编排原语，端适配在 {@link FireController.Hooks}。
/// > 但 fire 双端本就不对称（冷却模型不同），脚本实现<b>允许</b>按端分支（见 {@code docs/scripting-boundaries.md}）。
/// @author TACZ, USS_Shenzhou
@FunctionalInterface
public interface AfterShotDecider {

    /// > 执行出膛后编排。
    /// >
    /// > @param ops               受控操作集（起 burst / 拉栓 / 补膛 + 只读查询）
    /// > @param shouldBoltDefault 内置默认建议：单发模式下本发是否应拉栓
    void decide(FireAfterShotOps ops, boolean shouldBoltDefault);

    /// > 内置默认编排：连发数 &gt;1 起 burst，否则按建议拉栓。等价重构前 {@code orchestrateAfterShot} 的行为。
    static AfterShotDecider builtin() {
        return (ops, shouldBoltDefault) -> {
            if (ops.burstCount() > 1) {
                ops.startBurst();
            } else if (shouldBoltDefault) {
                ops.boltAfterShot();
            }
        };
    }
}
