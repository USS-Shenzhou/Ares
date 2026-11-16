package cn.ussshenzhou.ares.gun.client.resource.display;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nullable;

/// > 枪灯（手电）显示配置。枪与配件 display 共用。
/// > <p>
/// > 枪 display：{@code bone} 指定灯光起点骨骼（缺省回退枪口骨骼）；
/// > 配件 display：忽略 {@code bone}，起点取枪灯配件所在槽位的挂点。
/// > <p>
/// > {@code color} 为十六进制 RGB 字符串（如 {@code "FFF0C8"}），缺省暖白；
/// > {@code range} 为照射范围（米），缺省由渲染端默认值决定。
/// @author TACZ, USS_Shenzhou
public class Flashlight {
    /// > 灯光起点骨骼名（仅枪 display 有效）。缺省回退到枪口火光骨骼。
    @Nullable
    @SerializedName("bone")
    public String bone = null;

    /// > 灯光颜色，十六进制 RGB（可带 # 或 0x 前缀）。缺省暖白。
    @Nullable
    @SerializedName("color")
    public String color = null;

    /// > 照射范围（米）。缺省由渲染端默认值决定。
    @Nullable
    @SerializedName("range")
    public Float range = null;
}