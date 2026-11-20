package cn.ussshenzhou.ares.client.resource.display;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

/// > 资源路径：assets/tacz/display/under_barrels/*.json
/// > 用于描述“安装某个下挂后，替换目标枪械的动画/姿态层”。
/// @author TACZ, USS_Shenzhou
public class UnderBarrelOverrideRuleCfg {
    @SerializedName("underbarrel")
    public Identifier underBarrelId;

    @SerializedName("target")
    public Identifier targetGunDisplayId;

    @Nullable
    @SerializedName("gun_display")
    public Identifier mountedWeaponDisplayId;

    @SerializedName("overrides")
    public OverridesCfg overrides;

    public static class OverridesCfg {
        @Nullable
        @SerializedName("animations")
        public Identifier animationLocation;

        @Nullable
        @SerializedName("default_animation")
        public Identifier defaultAnimationLocation;
    }
}
