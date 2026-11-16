package cn.ussshenzhou.ares.gun.client.resource.display;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

/// > 弹壳模型配置（弹药 display 内嵌）。
/// > <p>
/// > {@code model} 指向弹壳的 bedrock geo 模型；{@code texture} 为其贴图（加载时补 {@code textures/} 前缀与
/// > {@code .png} 后缀）。两者缺一则该弹药不渲染抛壳。
/// @author TACZ, USS_Shenzhou
public class Shell {
    @Nullable
    @SerializedName("model")
    public Identifier model;

    @Nullable
    @SerializedName("texture")
    public Identifier texture;
}
