package cn.ussshenzhou.ares.gun.client.resource.display;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW the Pojo suffix is dropped and client.resource.pojo is merged into client.resource.display (GunDisplay,
// AmmoDisplay, AttachmentDisplay, CharmDisplay, SlotAdapterDisplay, AmmoTransform, AnimationPoseComposition, Flashlight, Laser,
// MuzzleFlash, PoseLayerBlend, PoseLayerMask, PoseLayerMaskControlPoint, Shell, ShellEjection, UnderBarrelOverrideRule).
public class GunDisplay {
    @SerializedName("model")
    public Identifier modelLocation;

    @SerializedName("texture")
    public Identifier modelTexture;

    @Nullable
    @SerializedName("animation")
    public Identifier animationLocation;

    @Nullable
    @SerializedName("default_animation")
    public Identifier defaultAnimation = null;

    @Nullable
    @SerializedName("animation_pose_compositions")
    public List<AnimationPoseComposition> animationPoseCompositions = null;

    @Nullable
    @SerializedName("controller")
    public Identifier controller = null;

    @Nullable
    @SerializedName("attachment_slot_display")
    public Map<Identifier, AttachmentSlotDisplay> attachmentSlotDisplay = null;

    @Nullable
    @SerializedName("slot")
    public Identifier slotTexture = null;

    @SerializedName("iron_zoom")
    public float ironZoom = 1.2f;

    @SerializedName("model_fov")
    public float modelFov = 70.0f;

    @SerializedName("zoom_model_fov")
    public float zoomModelFov = 70.0f;

    @Nullable
    @SerializedName("ica_control_bone")
    public String icaControlBone = "constraint";

    @Nullable
    @SerializedName("ica_strength_multiplier")
    public Float icaStrengthMultiplier = 1.0f;

    @Nullable
    @SerializedName("ica_bone_multipliers")
    public Map<String, Float> icaBoneMultipliers = null;

    @Nullable
    @SerializedName("muzzle_flash")
    public MuzzleFlash muzzleFlash = null;

    /// > 有序枪口骨骼表；数组下标即发射时使用的 muzzleIndex。
    @Nullable
    @SerializedName("muzzles")
    public List<String> muzzles = null;

    @Nullable
    @SerializedName("tracer_color")
    public String tracerColor = null;

    @Nullable
    @SerializedName("laser")
    public Laser laser = null;

    /// > 枪自带枪灯（手电）配置；声明后该枪可被枪灯快捷键开关。
    @Nullable
    @SerializedName("flashlight")
    public Flashlight flashlight = null;

    @Nullable
    @SerializedName("shell")
    public ShellEjection shell = null;

    /// > 战术拒枪姿态：枪械本体上的第二个定位组，作为一条独立视图位进入 Alt+V 循环列表。
    /// > 缺省（null）表示该枪没有此姿态，行为与仅有机瞄位时完全一致。
    @Nullable
    @SerializedName("tactical_stance")
    public TacticalStance tacticalStance = null;

    /// > HUD 枪械图标（2D 贴图，39x13 区域）。对应包内 {@code textures/gun/hud/<id>.png}；缺省则不渲染图标。
    @Nullable
    @SerializedName("hud")
    public Identifier hudTexture = null;

    /// > 空弹（或过热）时替换的 HUD 图标；缺省时退化为把 {@code hud} 染红。
    @Nullable
    @SerializedName("hud_empty")
    public Identifier hudEmptyTexture = null;

    /// > HUD 弹药计数样式：{@code normal} / {@code percent}。缺省 {@code normal}。
    @SerializedName("ammo_count_style")
    public String ammoCountStyle = "normal";

    /// > 持枪时是否强制显示准星（覆盖瞄准/换弹等隐藏规则）。
    @SerializedName("show_crosshair")
    public boolean showCrosshair = false;

    /// > 战术拒枪姿态的 display 声明。各字段缺省时回退机瞄位对应值，包作者可以先只给骨骼。
    public static class TacticalStance {
        /// > 定位组骨骼名，缺省 {@code tactical_view}。
        @Nullable
        @SerializedName("view")
        public String view = null;

        /// > 世界倍率，缺省回退 {@code iron_zoom}。
        @Nullable
        @SerializedName("zoom")
        public Float zoom = null;

        /// > 非瞄准时的模型 FOV，缺省回退 {@code model_fov}。
        @Nullable
        @SerializedName("model_fov")
        public Float modelFov = null;

        /// > 开镜时的模型 FOV，缺省回退 {@code zoom_model_fov}。
        @Nullable
        @SerializedName("zoom_model_fov")
        public Float zoomModelFov = null;
    }
}
