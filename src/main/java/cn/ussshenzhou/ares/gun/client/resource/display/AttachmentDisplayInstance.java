package cn.ussshenzhou.ares.gun.client.resource.display;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.google.common.base.Preconditions;
import cn.ussshenzhou.ares.gun.client.model.BedrockAttachmentModel;
import cn.ussshenzhou.ares.gun.client.resource.ClientAssetsManager;
import cn.ussshenzhou.ares.gun.client.resource.display.AttachmentDisplay;
import cn.ussshenzhou.ares.gun.client.resource.display.AttachmentSlotDisplay;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

import java.util.Map;

/// @author TACZ, USS_Shenzhou
public class AttachmentDisplayInstance {
    private BedrockAttachmentModel model;
    private Identifier modelTexture;
    private boolean scope;
    private boolean sight;
    private boolean showMount;
    private float scopeViewRadiusModifier = 1.0F;
    private float[] viewsFov = new float[]{70.0F};
    @Nullable
    private float[] zoom = null;
    @Nullable
    private float[] zoomFovRange = null;
    @Nullable
    private float[] zoomRange = null;
    @Nullable
    private Map<Identifier, AttachmentSlotDisplay> slots;
    @Nullable
    private GunDisplayInstance.LaserConfig laser;
    @Nullable
    private GunDisplayInstance.FlashlightConfig flashlight;
    /// > 变体相关
    private String variantMode = "replace";
    @Nullable
    private String variantAttachBone;
    @Nullable
    private org.joml.Vector3f variantMountOffset;

    /// > 必须在资源包重载期间构建，否则所需的资源缓存不可用
    public static AttachmentDisplayInstance of(AttachmentDisplay pojo) {
        AttachmentDisplayInstance instance = new AttachmentDisplayInstance();
        instance.scope = pojo.scope;
        instance.sight = pojo.sight;
        instance.showMount = pojo.showMount;
        instance.scopeViewRadiusModifier = pojo.scopeViewRadiusModifier;
        instance.viewsFov = sanitizeViewsFov(pojo);
        instance.zoom = sanitizeZoom(pojo, instance.viewsFov);
        instance.zoomFovRange = sanitizeRange(pojo.zoomFovRange, "zoom_fov_range");
        instance.zoomRange = sanitizeRange(pojo.zoomRange, "zoom_range");
        instance.slots = pojo.attachmentSlotDisplay;
        instance.variantMode = pojo.mode == null ? "replace" : pojo.mode;
        instance.variantAttachBone = pojo.attachBone;
        instance.variantMountOffset = pojo.mountOffset;

        buildModelAndTexture(instance, pojo);
        buildLaser(instance, pojo);
        buildFlashlight(instance, pojo);

        return instance;
    }

    private static float[] sanitizeViewsFov(AttachmentDisplay pojo) {
        if (pojo.viewsFov == null || pojo.viewsFov.length == 0) {
            Preconditions.checkArgument(pojo.fov > 0.0F, "fov must > 0");
            return new float[]{Math.min(pojo.fov, 70.0F)};
        }
        float[] result = new float[pojo.viewsFov.length];
        for (int i = 0; i < pojo.viewsFov.length; i++) {
            float fov = pojo.viewsFov[i];
            Preconditions.checkArgument(fov > 0.0F, "fov must > 0");
            result[i] = Math.min(fov, 70.0F);
        }
        return result;
    }

    /// > 有级档位倍率数组：必须与 views_fov 同长配套（同索引 = 同一档，超长截断并告警）；
    /// > 缺省由 70/fov 反推。
    private static float[] sanitizeZoom(AttachmentDisplay pojo, float[] viewsFov) {
        float[] raw = pojo.zoom;
        if (raw == null || raw.length == 0) {
            return null;
        }
        if (raw.length != viewsFov.length) {
            return null;
        }
        float[] result = new float[raw.length];
        for (int i = 0; i < raw.length; i++) {
            result[i] = Math.max(0.1F, raw[i]);
        }
        return result;
    }

    /// > 通用范围 sanitize：长度 2、0 < min < max、max 上限 70（fov 语义）。非法返回 null（视为未配置）。
    private static float[] sanitizeRange(float[] range, String name) {
        if (range == null || range.length != 2) {
            return null;
        }
        float min = Math.min(range[0], range[1]);
        float max = Math.max(range[0], range[1]);
        if (min <= 0.0F || min >= max || max > 70.0F) {
            return null;
        }
        return new float[]{min, max};
    }

    public static void buildLaser(AttachmentDisplayInstance instance, AttachmentDisplay pojo) {
        instance.laser = null;
        if (pojo.laser == null) {
            return;
        }
        String boneName = pojo.laser.bone == null || pojo.laser.bone.isEmpty() ? "laser_beam" : pojo.laser.bone;
        float[] color = GunDisplayInstance.parseHexColor(pojo.laser.color);
        if (color == null) {
            color = new float[]{1.0f, 0.1f, 0.1f};
        }
        float length = pojo.laser.length == null ? 0.0f : Math.max(0.0f, pojo.laser.length);
        instance.laser = new GunDisplayInstance.LaserConfig(boneName, color, length);
    }

    public static void buildFlashlight(AttachmentDisplayInstance instance, AttachmentDisplay pojo) {
        instance.flashlight = null;
        if (pojo.flashlight == null) {
            return;
        }
        String boneName = pojo.flashlight.bone == null || pojo.flashlight.bone.isEmpty()
                ? "flashlight" : pojo.flashlight.bone;
        float[] color = GunDisplayInstance.parseHexColor(pojo.flashlight.color);
        if (color == null) {
            color = new float[]{1.0f, 0.88f, 0.68f};
        }
        float range = pojo.flashlight.range == null ? 0.0f : Math.max(0.0f, pojo.flashlight.range);
        instance.flashlight = new GunDisplayInstance.FlashlightConfig(boneName, color, range);
    }

    /// > 加载模型和贴图
    public static void buildModelAndTexture(AttachmentDisplayInstance instance, AttachmentDisplay pojo) {
        Preconditions.checkArgument(pojo.modelLocation != null, "display object missing model field");
        BedrockModelPOJO modelPojo = ClientAssetsManager.INSTANCE.getModelPojo(pojo.modelLocation);
        Preconditions.checkArgument(modelPojo != null, "there is no corresponding model file: " + pojo.modelLocation);
        instance.model = new BedrockAttachmentModel(modelPojo);
        instance.model.setIsScope(instance.scope);
        instance.model.setIsSight(instance.sight);
        instance.model.setScopeViewRadiusModifier(instance.scopeViewRadiusModifier);
        Preconditions.checkArgument(pojo.modelTexture != null, "display object missing texture field");
        instance.modelTexture = pojo.modelTexture.withPrefix("textures/").withSuffix(".png");
    }

    public BedrockAttachmentModel getModel() {
        return model;
    }

    public Identifier getModelTexture() {
        return modelTexture;
    }

    @Nullable
    public GunDisplayInstance.LaserConfig getLaser() {
        return laser;
    }

    @Nullable
    public GunDisplayInstance.FlashlightConfig getFlashlight() {
        return flashlight;
    }

    public boolean isScope() {
        return scope;
    }

    public boolean isSight() {
        return sight;
    }

    public boolean isShowMount() {
        return showMount;
    }

    public float getScopeViewRadiusModifier() {
        return scopeViewRadiusModifier;
    }

    public float[] getViewsFov() {
        return viewsFov;
    }

    /// > 有级档位倍率数组（与 views_fov 同索引配套）；未声明返回 null（按 70/viewsFov 反推）。
    @Nullable
    public float[] getZoom() {
        return zoom;
    }

    /// > 无级模型 FOV 范围 [minFov, maxFov]（数值越小倍率越高）；未配置返回 null。
    @Nullable
    public float[] getZoomFovRange() {
        return zoomFovRange;
    }

    /// > 无级倍率范围 [minZoom, maxZoom]（独立于模型 FOV 的配套字段）；未配置返回 null。
    @Nullable
    public float[] getZoomRange() {
        return zoomRange;
    }

    /// > 是否支持有级变倍（V 键循环档位）：声明了至少两个 views_fov 档位。
    public boolean hasSteppedZooms() {
        return viewsFov != null && viewsFov.length > 1;
    }

    /// > 是否支持无级变倍（按住变倍键 + 滚轮）：声明了 zoom_fov_range 或 zoom_range 任一。
    public boolean hasSmoothZoom() {
        return zoomFovRange != null || zoomRange != null;
    }

    /// > 获取指定子配件槽位的显示配置，叶子配件返回 null。
    @Nullable
    public AttachmentSlotDisplay getSlot(Identifier slotId) {
        if (slots == null) {
            return null;
        }
        return slots.get(slotId);
    }

    /// > 变体几何模式：`replace`（替换基础模型）或 `overlay`（在 attach_bone 处叠加）。
    public String getVariantMode() {
        return variantMode;
    }

    /// > 变体是否为 overlay 模式（在基础配件模型上叠加渲染）。
    public boolean isOverlayVariant() {
        return "overlay".equalsIgnoreCase(variantMode);
    }

    /// > overlay 模式下叠加模型挂载到基础配件模型的骨骼名；非 overlay 返回 null。
    @Nullable
    public String getVariantAttachBone() {
        return variantAttachBone;
    }

    /// > 变体对下游（子配件挂点 / 瞄准视点）施加的额外平移；无则返回 null。
    @Nullable
    public org.joml.Vector3f getVariantMountOffset() {
        return variantMountOffset;
    }
}
