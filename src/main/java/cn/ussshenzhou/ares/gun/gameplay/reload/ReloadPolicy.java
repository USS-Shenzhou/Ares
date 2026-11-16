package cn.ussshenzhou.ares.gun.gameplay.reload;

import cn.ussshenzhou.ares.gun.custom.gun.GunData;

/// > 换弹打断策略（纯数据，从 {@link GunData} 推导，双端同源）。
/// > <p>
/// > 收编原 {@code ReloadBehavior.canFireInterrupt / canSprintInterrupt}：
/// > 它们本就只读数据、无状态，因此从有状态的 behavior 实例中剥离为不可变值对象。
/// >
/// > @param canFireInterrupt  开火是否可打断当前换弹（true=打断并开火）
/// > @param canSprintInterrupt 疾跑是否可打断当前换弹（true=打断并允许疾跑）
/// @author TACZ, USS_Shenzhou
public record ReloadPolicy(boolean canFireInterrupt, boolean canSprintInterrupt) {

    /// > 弹匣换弹：开火 / 疾跑均不打断（保持原 MagazineReloadBehavior 默认）。
    public static final ReloadPolicy MAGAZINE = new ReloadPolicy(false, false);

    /// > 从枪械数据推导打断策略。
    public static ReloadPolicy of(GunData data) {
        if ("shell_by_shell".equals(data.getReloadType())) {
            GunData.ShellReloadTiming timing = data.getShellReload();
            if (timing == null) {
                return new ReloadPolicy(true, true);
            }
            return new ReloadPolicy(timing.canFireInterrupt(), timing.canSprintInterrupt());
        }
        return MAGAZINE;
    }
}
