package cn.ussshenzhou.ares.gun.client.resource.display;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nullable;

/// > 镭射（激光指示器）显示配置。枪与配件 display 共用。
/// > <ul>
/// >   <li>枪 display：{@code bone} 指定激光起点骨骼（缺省回退枪口骨骼）；</li>
/// >   <li>配件 display：忽略 {@code bone}，起点取镭射配件所在槽位的挂点。</li>
/// > </ul>
/// > {@code color} 为十六进制 RGB 字符串（如 {@code "FF2020"} 或 {@code "0xFF2020"}），缺省红色；
/// > {@code length} 为激光最大长度（米），缺省由渲染端默认值决定。
/// @author TACZ, USS_Shenzhou
public class Laser {
    /// > 激光起点骨骼名（仅枪 display 有效）。缺省回退到枪口火光骨骼。
    @Nullable
    @SerializedName("bone")
    public String bone = null;

    /// > 激光颜色，十六进制 RGB（可带 # 或 0x 前缀）。缺省红色。
    @Nullable
    @SerializedName("color")
    public String color = null;

    /// > 激光最大长度（米）。缺省由渲染端默认射程决定。
    @Nullable
    @SerializedName("length")
    public Float length = null;
}
