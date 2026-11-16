package cn.ussshenzhou.ares.gun.client.resource.display;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;
import org.joml.Vector3f;

import java.util.Map;

/// @author TACZ, USS_Shenzhou
public class AttachmentDisplay {
    @SerializedName("model")
    public Identifier modelLocation;

    @SerializedName("texture")
    public Identifier modelTexture;

    /// > 是否为筒状放大瞄具（长筒镜）。需要模板缓冲渲染镜片透视。
    @SerializedName("scope")
    public boolean scope = false;

    /// > 是否为红点 / 全息瞄具。需要模板缓冲渲染分划板。
    @SerializedName("sight")
    public boolean sight = false;

    /// > 是否在枪身上渲染瞄具支架（mount）。
    @SerializedName("show_mount")
    public boolean showMount = false;

    /// > 镜内可视圆形区域的半径修正系数，默认为 1。
    @SerializedName("scope_view_radius_modifier")
    public float scopeViewRadiusModifier = 1.0F;

    @SerializedName("fov")
    public float fov = 70.0F;

    @Nullable
    @SerializedName("views_fov")
    public float[] viewsFov = null;

    /// > 有级档位的倍率数组，与 [#viewsFov] 一一配套（同索引 = 同一档）。
    /// > 声明了 views_fov 但缺省本字段时，按 70/viewsFov 反推。
    @Nullable
    @SerializedName("zoom")
    public float[] zoom = null;

    /// > 无级模型 FOV 范围（[minFov, maxFov]，数值越小倍率越高）。声明后开镜按住变倍键
    /// > 拨动滚轮在范围内连续调节**模型 FOV**；缺省 = 不支持无级模型 FOV。
    @Nullable
    @SerializedName("zoom_fov_range")
    public float[] zoomFovRange = null;

    /// > 无级倍率范围（[minZoom, maxZoom]，与 [#zoomFovRange] 配套的独立字段，
    /// > 滚轮同比例映射调节）。缺省时按 70/zoomFovRange 反推。
    @Nullable
    @SerializedName("zoom_range")
    public float[] zoomRange = null;

    /// > 子配件槽位的显示配置。任意配件作为容器时，用于定位其挂载的子配件。
    /// > 叶子配件可为 null。
    @Nullable
    @SerializedName("attachment_slot_display")
    public Map<Identifier, AttachmentSlotDisplay> attachmentSlotDisplay = null;

    /// > ===== 以下字段仅在本 display 作为「配件变体」使用时有意义 =====

    /// > 变体几何模式：
    /// > - `replace`：用本 display 的模型整体替换基础配件模型（默认）；
    /// > - `overlay`：在基础配件模型之上、[#attachBone] 指定骨骼处叠加渲染本模型。
    @SerializedName("mode")
    public String mode = "replace";

    /// > overlay 模式下，叠加模型挂载到「基础配件模型」的哪根骨骼上。replace 模式忽略。
    @Nullable
    @SerializedName("attach_bone")
    public String attachBone = null;

    /// > 变体对下游（子配件挂点 / 瞄准视点）施加的额外平移（模型空间，1.0 = 16 像素）。
    /// > 例如增高座把瞄准点抬高。可空。
    @Nullable
    @SerializedName("mount_offset")
    public Vector3f mountOffset = null;

    /// > 本配件若是镭射发射器，可在此声明发射骨骼、颜色与长度。
    @Nullable
    @SerializedName("laser")
    public Laser laser = null;

    /// > 本配件若是枪灯（手电），可在此声明灯光起点骨骼、颜色与照射范围。
    @Nullable
    @SerializedName("flashlight")
    public Flashlight flashlight = null;
}
