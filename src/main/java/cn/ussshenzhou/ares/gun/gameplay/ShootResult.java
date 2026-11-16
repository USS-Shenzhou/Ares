package cn.ussshenzhou.ares.gun.gameplay;

/// @author TACZ, USS_Shenzhou
public enum ShootResult {
    SUCCESS,
    /// > 请求已接受但子弹未出膛（蓄力开始/延迟倒计时开始）
    ACCEPTED,
    NO_AMMO,
    COOLDOWN,
    OVERHEATED,
    BLOCKED
}
