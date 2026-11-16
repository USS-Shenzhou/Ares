package cn.ussshenzhou.ares.gun.config;

import cn.ussshenzhou.ares.client.renderer.crosshair.CrosshairMode;
import cn.ussshenzhou.ares.client.renderer.crosshair.CrosshairType;
import cn.ussshenzhou.ares.client.resource.display.AmmoCountStyle;
import net.neoforged.neoforge.common.ModConfigSpec;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW ForgeConfigSpec replaced with ModConfigSpec; the script interpreted mode option is removed.
public class ClientConfig {
    public static final ModConfigSpec SPEC;


    /// > 挂饰防穿模：物理摆动时让挂饰被枪体几何挡住，不穿进枪身。
    public static final ModConfigSpec.BooleanValue CHARM_COLLISION_ENABLED;
    /// > 挂饰碰撞盒调试可视化：把参与防穿模的枪体碰撞盒画成线框、质点探针画成十字。
    public static final ModConfigSpec.BooleanValue CHARM_COLLISION_DEBUG;

    /// > 正式枪械 HUD 总开关。
    public static final ModConfigSpec.BooleanValue HUD_ENABLED;
    /// > HUD 弹药计数样式（枪械 display 未声明 ammo_count_style 时的兜底）。
    public static final ModConfigSpec.EnumValue<AmmoCountStyle> HUD_AMMO_STYLE;
    /// > 低弹药变色阈值（弹药/容量低于该比例且少于 10 发时标红）。
    public static final ModConfigSpec.DoubleValue HUD_LOW_AMMO_RATIO;

    /// > 命中标记总开关。
    public static final ModConfigSpec.BooleanValue HIT_MARKER_ENABLED;
    /// > 命中标记展示时长（ms）。
    public static final ModConfigSpec.IntValue HIT_MARKER_KEEP_TIME_MS;

    /// > 持枪时使用的静态准星类型（STATIC 模式）。
    public static final ModConfigSpec.EnumValue<CrosshairType> CROSSHAIR_TYPE;
    /// > 准星渲染模式：STATIC 固定贴图 / DYNAMIC 随散布收缩扩散。
    public static final ModConfigSpec.EnumValue<CrosshairMode> CROSSHAIR_MODE;
    /// > DYNAMIC 模式下开火后准星短暂扩散脉冲。
    public static final ModConfigSpec.BooleanValue CROSSHAIR_BLOOM_ON_FIRE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Charm (decorative pendant) settings").push("charm");

        CHARM_COLLISION_ENABLED = builder
                .comment("Enable charm collision (anti-clipping).",
                         "When enabled, swinging charms are pushed out of the gun body geometry",
                         "instead of clipping through it. Disable to skip collision solving for performance.")
                .define("collisionEnabled", true);

        CHARM_COLLISION_DEBUG = builder
                .comment("Visualize charm collision boxes.",
                         "Draws the gun-body collision boxes as wireframes and the charm mass probe",
                         "as a cross. For debugging anti-clipping coverage only.")
                .define("collisionDebug", false);

        builder.pop();

        builder.comment("Gun HUD settings").push("hud");

        HUD_ENABLED = builder
                .comment("Enable the gun HUD (ammo, fire mode, module and action state display).")
                .define("enabled", true);

        HUD_AMMO_STYLE = builder
                .comment("Fallback ammo count style when the gun display declares no ammo_count_style.",
                         "NORMAL shows a plain number, PERCENT shows a percentage of the magazine.")
                .defineEnum("ammoStyle", AmmoCountStyle.NORMAL);

        HUD_LOW_AMMO_RATIO = builder
                .comment("Low-ammo warning ratio: ammo below (capacity * ratio) and below 10 rounds is tinted red.")
                .defineInRange("lowAmmoRatio", 0.25, 0.0, 1.0);

        builder.pop();

        builder.comment("Hit marker settings").push("hit_marker");

        HIT_MARKER_ENABLED = builder
                .comment("Show hit markers (hit / headshot / kill) driven by server-side results.")
                .define("enabled", true);

        HIT_MARKER_KEEP_TIME_MS = builder
                .comment("How long a hit marker stays visible, in milliseconds.")
                .defineInRange("keepTimeMs", 300, 50, 3000);

        builder.pop();

        builder.comment("Crosshair settings").push("crosshair");

        CROSSHAIR_TYPE = builder
                .comment("Static crosshair style used while holding a gun.")
                .defineEnum("type", CrosshairType.DOT_1);

        CROSSHAIR_MODE = builder
                .comment("Crosshair render mode.",
                         "STATIC: fixed texture crosshair.",
                         "DYNAMIC: procedural crosshair whose gap follows the current spread (shrinks when aiming, expands when moving or after firing).")
                .defineEnum("mode", CrosshairMode.STATIC);

        CROSSHAIR_BLOOM_ON_FIRE = builder
                .comment("In DYNAMIC mode, briefly expand the crosshair after firing.")
                .define("bloomOnFire", true);

        builder.pop();

        SPEC = builder.build();
    }

}
