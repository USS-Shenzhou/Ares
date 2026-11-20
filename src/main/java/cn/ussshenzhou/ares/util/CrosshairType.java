package cn.ussshenzhou.ares.util;

import cn.ussshenzhou.ares.Ares;
import net.minecraft.resources.Identifier;

/// > 静态准星类型（贴图映射，形状在贴图内）。名称与 `textures/crosshair/normal/<name>.png` 一一对应。
/// @author TACZ, USS_Shenzhou
public enum CrosshairType {
    EMPTY,
    DOT_1,
    CIRCLE_1, CIRCLE_2, CIRCLE_3,
    CROSS_1, CROSS_2, CROSS_3, CROSS_4, CROSS_5, CROSS_6,
    LINE_1, LINE_2, LINE_3,
    SQUARE_1, SQUARE_2, SQUARE_3, SQUARE_4, SQUARE_5, SQUARE_6,
    TRIDENT_1, TRIDENT_2;

    public Identifier getTextureLocation() {
        return Ares.modLoc("textures/crosshair/normal/" + name().toLowerCase() + ".png");
    }
}
