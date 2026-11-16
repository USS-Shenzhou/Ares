package cn.ussshenzhou.ares.attachment.entity;

/// > 持枪者状态枚举，为了平衡体验，可能不是所有状态都会在服务端体现
/// @author TACZ, USS_Shenzhou
public enum GunHolderState {
    /// > 未持枪
    NO_HOLD,
    /// > 拔枪中
    DRAWING,
    /// > 收枪中
    PUTTING_AWAY,
    /// > 正常状态
    NORMAL,
    /// > 换弹中
    RELOADING,
    /// > bolt动作
    BOLT_ACTION,
    /// > 跑射延迟（仅客户端）
    STOP_SPRINTING,
    /// > 射击冷却
    /// > 这个状态对开火本身的约束（在客户端）是特殊的，特定情况下仍允许开火
    FIRE_COOLDOWN,
    /// > 检视（仅客户端）
    INSPECT

}
