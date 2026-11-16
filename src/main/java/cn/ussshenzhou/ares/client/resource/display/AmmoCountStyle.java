package cn.ussshenzhou.ares.client.resource.display;

import com.google.gson.annotations.SerializedName;

/// > HUD 弹药计数样式。可由枪械 display 的 `ammo_count_style` 声明，缺省回退全局配置。
/// @author TACZ, USS_Shenzhou
public enum AmmoCountStyle {
    @SerializedName("normal")
    NORMAL,
    @SerializedName("percent")
    PERCENT;

    public static AmmoCountStyle parse(String value) {
        if (value == null) {
            return NORMAL;
        }
        return switch (value) {
            case "percent" -> PERCENT;
            default -> NORMAL;
        };
    }
}
