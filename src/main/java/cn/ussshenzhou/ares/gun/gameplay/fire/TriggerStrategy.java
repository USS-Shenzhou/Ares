package cn.ussshenzhou.ares.gun.gameplay.fire;

import cn.ussshenzhou.ares.gun.gameplay.ShootResult;

/// > 触发策略：描述"按下扳机到子弹出膛"的时间关系。
/// > 纯时序状态机，双端共享代码，通过注入不同的 [FireCallback] 区分端行为。
/// @author TACZ, USS_Shenzhou
public interface TriggerStrategy {

    /// > 扳机按下。
    ShootResult onPress(float yaw, float pitch);

    /// > 扳机松开。
    ShootResult onRelease(float yaw, float pitch);

    /// > 每 tick 更新。
    void tick();

    /// > 重置内部状态。
    void reset();

    /// > 当前是否处于活跃状态（蓄力中/延迟等待中），活跃时不接受新的 press。
    boolean isActive();

    /// > 当前累积的蓄力原始输入量（tick 数），供客户端松手时随 C2S 上报。
    /// > 非蓄力策略默认返回 0（无原始量可上报）。
    default int chargeInput() {
        return 0;
    }

    /// > 注入客户端上报的蓄力原始量（服务端权威流用）。仅做**宽松防呆**（负数归零），
    /// > 不严格追踪时长、不 clamp 上限——时长合理性裁剪交给脚本 `onRelease`（或内置 clamp）处理。
    /// > 非蓄力策略默认忽略。
    default void injectChargeInput(int raw) {
    }

    /// > 触发策略的发射回调。由外层提供实现：
    /// > 客户端：播放动画/音效 + 发 C2S + 预消耗弹药。
    /// > 服务端：调用 ShootHelper 生成弹丸。
    @FunctionalInterface
    interface FireCallback {
        ShootResult fire(float yaw, float pitch, float damageMult, float speedMult, int muzzleIndex);
    }
}
