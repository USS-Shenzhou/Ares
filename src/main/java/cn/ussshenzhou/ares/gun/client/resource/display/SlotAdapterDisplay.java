package cn.ussshenzhou.ares.gun.client.resource.display;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;
import org.joml.Vector3f;

/// > 槽位适配器的客户端显示配置。
/// >
/// > 适配器是叠加在宿主槽位挂点处的小模型（如垫高的导轨），并可把子配件落点抬高。
/// @author TACZ, USS_Shenzhou
public class SlotAdapterDisplay {
    @SerializedName("model")
    public Identifier modelLocation;

    @SerializedName("texture")
    public Identifier modelTexture;

    /// > 适配器对子配件落点施加的额外平移（模型空间，1.0 = 16 像素）。
    /// > 例如垫高导轨把其上安装的配件整体抬高。可空。
    @Nullable
    @SerializedName("mount_offset")
    public Vector3f mountOffset = null;
}
