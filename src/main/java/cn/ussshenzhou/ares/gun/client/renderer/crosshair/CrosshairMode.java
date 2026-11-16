package cn.ussshenzhou.ares.gun.client.renderer.crosshair;

/// > 准星渲染模式。
/// > - [#STATIC]：固定贴图准星，不随散布变化。
/// > - [#DYNAMIC]：程序化准星，间距随当前散布收缩/扩散（瞄准收缩、移动/开火扩散）。
///
/// @author TACZ, USS_Shenzhou
public enum CrosshairMode {
    STATIC,
    DYNAMIC
}
