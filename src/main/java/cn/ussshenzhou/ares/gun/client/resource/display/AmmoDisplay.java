package cn.ussshenzhou.ares.gun.client.resource.display;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

/// > 弹药 display 配置。
/// >
/// > 承载弹药本体的 3D 模型 / 贴图 / GUI 图标 / 各上下文缩放，以及抛壳弹壳模型（[#shell]）。
/// > 模型或贴图缺失时退化为 GUI 2D 图标渲染。后续可扩展曳光弹颜色、命中粒子等字段。
/// @author TACZ, USS_Shenzhou
public class AmmoDisplay {
    /// > 弹药本体 bedrock geo 模型 id（如 `tacz:ammo/9mm`）。缺失则退化 GUI 渲染。
    @Nullable
    @SerializedName("model")
    public Identifier model = null;

    /// > 弹药模型贴图 id（加载时补 `textures/` 前缀与 `.png` 后缀）。
    @Nullable
    @SerializedName("texture")
    public Identifier texture = null;

    /// > GUI 格子 2D 图标贴图 id（加载时补 `textures/` 前缀与 `.png` 后缀）。
    @Nullable
    @SerializedName("slot")
    public Identifier slot = null;

    /// > 各上下文缩放变换。
    @Nullable
    @SerializedName("transform")
    public AmmoTransform transform = null;

    @Nullable
    @SerializedName("shell")
    public Shell shell = null;
}
