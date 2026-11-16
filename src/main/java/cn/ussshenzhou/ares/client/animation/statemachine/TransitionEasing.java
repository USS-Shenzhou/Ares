package cn.ussshenzhou.ares.client.animation.statemachine;

import com.maydaymemory.mae.util.Easing;

/// > 状态机过渡缓动曲线的字符串解析，供 Molang / JS 两种后端共用，避免重复。
/// @author TACZ, USS_Shenzhou
public class TransitionEasing {
    /// > 解析缓动名称；未知或 null 返回 [Easing#LINEAR]。
    public static Easing resolve(String easing) {
        if (easing == null) {
            return Easing.LINEAR;
        }
        return switch (easing) {
            case "QUAD_IN" -> Easing.QUAD_IN;
            case "QUAD_OUT" -> Easing.QUAD_OUT;
            case "QUAD_IN_OUT" -> Easing.QUAD_IN_OUT;
            case "CUBIC_IN" -> Easing.CUBIC_IN;
            case "CUBIC_OUT" -> Easing.CUBIC_OUT;
            case "CUBIC_IN_OUT" -> Easing.CUBIC_IN_OUT;
            default -> Easing.LINEAR;
        };
    }
}
