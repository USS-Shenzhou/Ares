package cn.ussshenzhou.ares.gun.gameplay.timeline;

import cn.ussshenzhou.ares.gun.custom.gun.FireModeEntry;
import cn.ussshenzhou.ares.gun.gameplay.fire.ChargeTrigger;
import cn.ussshenzhou.ares.gun.gameplay.fire.DelayedTrigger;
import cn.ussshenzhou.ares.gun.gameplay.fire.InstantTrigger;
import cn.ussshenzhou.ares.gun.gameplay.fire.TriggerStrategy;

/// > 触发策略供给器：由给定 fire mode 与开火回调构建 [TriggerStrategy]。
/// >
/// > 由 session 在装配开火行为时构建：默认实现（[#builtin()]）按 [FireModeEntry#getTrigger()]
/// > 走内置三策略（instant / delayed / charge），等价零脚本行为；声明了 `fire.plan` 的 `trigger`
/// > 时替换为脚本驱动实现（`ScriptTriggerStrategy`）。
/// >
/// > 之所以用供给器注入（而非 `FireController` 内 switch 直造），是因为 trigger 依赖
/// > `FireController::firePrimary` 作为出膛回调，而 controller 构造时 trigger 就要建好——用供给器把
/// > 「构建时机」延后到 controller 拿到自己的回调之后，解决构造顺序依赖（参照 [CooldownProvider] 模式）。
/// @author TACZ, USS_Shenzhou
@FunctionalInterface
public interface TriggerStrategyProvider {

    /// > 据 fire mode 与出膛回调构建触发策略。
    /// >
    /// > @param mode     当前 fire mode 数据（读 trigger 类型 / 延迟 tick / 蓄力参数）
    /// >
    /// > @param callback 出膛回调（通常是 `FireController::firePrimary`）
    TriggerStrategy create(FireModeEntry mode, TriggerStrategy.FireCallback callback);

    /// > 内置默认供给器：按 [FireModeEntry#getTrigger()] 走内置三策略。等价零脚本行为。
    static TriggerStrategyProvider builtin() {
        return (mode, callback) -> switch (mode.getTrigger()) {
            case "delayed" -> new DelayedTrigger(callback, mode.getDelayTicks());
            case "charge" -> new ChargeTrigger(callback,
                    mode.getChargeTime(), mode.getChargeMinRatio(), mode.getChargeMinMultiplier());
            default -> new InstantTrigger(callback);
        };
    }
}
