package cn.ussshenzhou.ares.gun.client.resource.display;

import com.mojang.logging.LogUtils;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockAnimationFile;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockAnimationPOJO;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.google.common.base.Preconditions;
import com.maydaymemory.mae.control.montage.AnimationMontage;
import cn.ussshenzhou.ares.gun.client.animation.montage.MontageCompiler;
import cn.ussshenzhou.ares.gun.client.animation.montage.MontageContext;
import cn.ussshenzhou.ares.gun.client.animation.montage.Montage;
import cn.ussshenzhou.ares.gun.client.animation.statemachine.GunMolangContext;
import cn.ussshenzhou.ares.gun.client.model.BedrockGunModel;
import cn.ussshenzhou.ares.gun.client.resource.ClientAssetsManager;
import cn.ussshenzhou.ares.gun.client.resource.display.AmmoCountStyle;
import cn.ussshenzhou.ares.gun.client.resource.display.AnimationPoseComposition;
import cn.ussshenzhou.ares.gun.client.resource.display.AttachmentSlotDisplay;
import cn.ussshenzhou.ares.gun.client.resource.display.Flashlight;
import cn.ussshenzhou.ares.gun.client.resource.display.GunDisplay;
import cn.ussshenzhou.ares.gun.client.resource.display.Laser;
import cn.ussshenzhou.ares.gun.client.resource.display.MuzzleFlash;
import cn.ussshenzhou.ares.gun.client.resource.display.ShellEjection;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import javax.annotation.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/// @author TACZ, USS_Shenzhou
public class GunDisplayInstance {
    private BedrockGunModel model;
    private Identifier modelTexture;
    private Map<String, BedrockAnimation> animations;

    @Nullable
    private Identifier controllerId;
    private Map<Identifier, AttachmentSlotDisplay> slots;

    private Identifier slotTexture;

    @Nullable
    private IcaConfig icaConfig;

    @Nullable
    private MuzzleFlashConfig muzzleFlash;
    private List<String> muzzles = List.of("muzzle_flash");

    private float[] tracerColor = null;

    @Nullable
    private LaserConfig laser;

    @Nullable
    private FlashlightConfig flashlight;

    @Nullable
    private ShellEjectionConfig shellEjection;
    private float ironZoom = 1.2F;
    private float modelFov = 70.0F;
    private float zoomModelFov = 70.0F;

    /// > 战术拒枪姿态（本体第二定位组）；未声明为 null。
    @Nullable
    private TacticalStance tacticalStance;

    /// > HUD 相关：枪械图标 / 空弹图标 / 弹药计数样式 / 强制显示准星
    @Nullable
    private Identifier hudTexture;
    @Nullable
    private Identifier hudEmptyTexture;
    private AmmoCountStyle ammoCountStyle = AmmoCountStyle.NORMAL;
    private boolean showCrosshair = false;

    /// > 单手双持按手变体：主手用 right，副手用 left。无声明时为 null（双持也回退本体）。
    @Nullable
    private GunDisplayInstance oneHandLeft;
    @Nullable
    private GunDisplayInstance oneHandRight;

    /// > 懒加载并缓存的 montage 编译结果。montage 编译依赖本 display 的动画表（绑定到本模型骨架），
    /// > 故按 display 维度缓存，跨该 display 派生的所有动画实例复用；随 display 随资源 reload 重建而失效。
    private final Map<Identifier, AnimationMontage<MontageContext>> montageCache = new HashMap<>();

    /// > 必须在资源包重载期间构建，否则所需的资源缓存不可用
    public static GunDisplayInstance of(GunDisplay pojo) {
        GunDisplayInstance instance = new GunDisplayInstance();

        buildModelAndTexture(instance, pojo);
        buildIcaConfig(instance, pojo);
        buildAnimations(instance, pojo);
        instance.controllerId = pojo.controller;
        buildMuzzleFlash(instance, pojo);
        buildMuzzles(instance, pojo);
        buildTracerColor(instance, pojo);
        buildLaser(instance, pojo);
        buildFlashlight(instance, pojo);
        buildShellEjection(instance, pojo);
        buildAimFov(instance, pojo);
        buildTacticalStance(instance, pojo);
        buildHudConfig(instance, pojo);
        buildOneHandVariants(instance, pojo);

        return instance;
    }

    /// > 构建单手双持按手变体。变体复用本体的 controller / slots / ICA / 抛壳 / 枪口火光等配置，
    /// > 仅替换模型、贴图与动画集（绑定到各自模型骨架）。任一字段缺省则回退本体对应值。
    public static void buildOneHandVariants(GunDisplayInstance instance, GunDisplay pojo) {
        if (pojo.oneHand == null) {
            return;
        }
        instance.oneHandLeft = buildVariant(instance, pojo, pojo.oneHand.left);
        instance.oneHandRight = buildVariant(instance, pojo, pojo.oneHand.right);
    }

    @Nullable
    private static GunDisplayInstance buildVariant(GunDisplayInstance base, GunDisplay basePojo,
                                                   @Nullable GunDisplay.Variant variant) {
        if (variant == null) {
            return null;
        }
        /// > 用本体 pojo 作为模板，仅覆盖模型/贴图/动画字段后重新构建一个完整实例。
        GunDisplay merged = new GunDisplay();
        merged.modelLocation = variant.modelLocation != null ? variant.modelLocation : basePojo.modelLocation;
        merged.modelTexture = variant.modelTexture != null ? variant.modelTexture : basePojo.modelTexture;
        merged.animationLocation = variant.animationLocation != null ? variant.animationLocation : basePojo.animationLocation;
        merged.defaultAnimation = variant.defaultAnimation != null ? variant.defaultAnimation : basePojo.defaultAnimation;
        merged.animationPoseCompositions = resolveAnimationPoseCompositions(basePojo, variant);
        merged.controller = basePojo.controller;
        merged.attachmentSlotDisplay = basePojo.attachmentSlotDisplay;
        merged.slotTexture = basePojo.slotTexture;
        merged.ironZoom = basePojo.ironZoom;
        merged.modelFov = basePojo.modelFov;
        merged.zoomModelFov = basePojo.zoomModelFov;
        merged.tacticalStance = basePojo.tacticalStance;
        merged.icaControlBone = basePojo.icaControlBone;
        merged.icaStrengthMultiplier = basePojo.icaStrengthMultiplier;
        merged.icaBoneMultipliers = basePojo.icaBoneMultipliers;
        merged.muzzleFlash = basePojo.muzzleFlash;
        merged.muzzles = basePojo.muzzles;
        merged.tracerColor = basePojo.tracerColor;
        merged.laser = basePojo.laser;
        merged.shell = basePojo.shell;
        /// > 变体不再递归 one_hand，避免无限递归。
        merged.oneHand = null;

        GunDisplayInstance variantInstance = new GunDisplayInstance();
        buildModelAndTexture(variantInstance, merged);
        buildIcaConfig(variantInstance, merged);
        buildAnimations(variantInstance, merged);
        variantInstance.controllerId = merged.controller;
        buildMuzzleFlash(variantInstance, merged);
        buildMuzzles(variantInstance, merged);
        buildTracerColor(variantInstance, merged);
        buildLaser(variantInstance, merged);
        buildFlashlight(variantInstance, merged);
        buildShellEjection(variantInstance, merged);
        buildAimFov(variantInstance, merged);
        buildTacticalStance(variantInstance, merged);
        buildHudConfig(variantInstance, merged);
        return variantInstance;
    }

    @Nullable
    static List<AnimationPoseComposition> resolveAnimationPoseCompositions(GunDisplay basePojo,
                                                                           @Nullable GunDisplay.Variant variant) {
        if (variant != null && variant.animationPoseCompositions != null) {
            return variant.animationPoseCompositions;
        }
        return basePojo.animationPoseCompositions;
    }

    /// > 取单手双持按手变体：`MAIN_HAND` → right，`OFF_HAND` → left。
    /// > 未声明对应变体时返回本体（回退到双手 display，验证期允许）。
    public GunDisplayInstance forHand(InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND) {
            return oneHandLeft != null ? oneHandLeft : this;
        }
        return oneHandRight != null ? oneHandRight : this;
    }

    public boolean hasOneHandVariants() {
        return oneHandLeft != null || oneHandRight != null;
    }

    /// > 加载模型和贴图
    public static void buildModelAndTexture(GunDisplayInstance instance, GunDisplay pojo) {
        Preconditions.checkArgument(pojo.modelLocation != null, "display object missing model field");
        BedrockModelPOJO modelPojo = ClientAssetsManager.INSTANCE.getModelPojo(pojo.modelLocation);
        Preconditions.checkArgument(modelPojo != null, "there is no corresponding model file: " + pojo.modelLocation);
        instance.model = new BedrockGunModel(modelPojo, pojo.attachmentSlotDisplay);
        instance.slots = pojo.attachmentSlotDisplay;
        Preconditions.checkArgument(pojo.modelTexture != null, "display object missing texture field");
        instance.modelTexture = pojo.modelTexture.withPrefix("textures/").withSuffix(".png");

        if (pojo.slotTexture != null) {
            instance.slotTexture = pojo.slotTexture.withPrefix("textures/").withSuffix(".png");
        }
    }

    public static void buildIcaConfig(GunDisplayInstance instance, GunDisplay pojo) {
        instance.icaConfig = null;

        float strengthMultiplier = 1.0f;
        if (pojo.icaStrengthMultiplier != null) {
            strengthMultiplier = Math.max(0.0f, Math.min(1.0f, pojo.icaStrengthMultiplier));
        }

        int controlBoneIndex = -1;
        String controlBoneName = pojo.icaControlBone;
        if (controlBoneName != null) {
            int index = instance.model.getIndex(controlBoneName);
            if (index >= 0) {
                controlBoneIndex = index;
            }
        }

        Map<Integer, Float> boneMultipliersByIndex = new HashMap<>();

        if (pojo.icaBoneMultipliers != null && !pojo.icaBoneMultipliers.isEmpty()) {
            for (var e : pojo.icaBoneMultipliers.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) {
                    continue;
                }
                int index = instance.model.getIndex(e.getKey());
                if (index < 0) {
                    continue;
                }
                float mul = Math.max(0.0f, Math.min(1.0f, e.getValue()));
                boneMultipliersByIndex.put(index, mul);
            }
            if (boneMultipliersByIndex.isEmpty()) {
                boneMultipliersByIndex = null;
            }
        }

        if (controlBoneIndex < 0 && boneMultipliersByIndex == null) {
            return;
        }

        instance.icaConfig = new IcaConfig(controlBoneName, controlBoneIndex, strengthMultiplier, boneMultipliersByIndex);
    }
    /// > 加载动画
    public static void buildAnimations(GunDisplayInstance instance, GunDisplay pojo) {
        Preconditions.checkArgument(pojo.animationLocation != null, "display object missing animation field");
        BedrockAnimationFile animationPojo = ClientAssetsManager.INSTANCE.getAnimationPojo(pojo.animationLocation);
        Preconditions.checkArgument(animationPojo != null, "there is no corresponding animation file: " + pojo.animationLocation);

        BedrockAnimationFile defaultAnimationPojo = null;
        if (pojo.defaultAnimation != null) {
            defaultAnimationPojo = ClientAssetsManager.INSTANCE.getAnimationPojo(pojo.defaultAnimation);
            Preconditions.checkArgument(defaultAnimationPojo != null, "there is no corresponding animation file: " + pojo.defaultAnimation);
        }

        Map<String, BedrockAnimationPOJO> resolvedAnimations = new HashMap<>();
        if (defaultAnimationPojo != null && defaultAnimationPojo.getAnimations() != null) {
            resolvedAnimations.putAll(defaultAnimationPojo.getAnimations());
        }
        if (animationPojo.getAnimations() != null) {
            resolvedAnimations.putAll(animationPojo.getAnimations());
        }
        resolvedAnimations = AnimationPoseCompositionHelper.compose(resolvedAnimations, pojo.animationPoseCompositions);

        Map<String, BedrockAnimation> map = new HashMap<>();
        for (Map.Entry<String, BedrockAnimationPOJO> entry : resolvedAnimations.entrySet()) {
            map.put(entry.getKey(), BedrockAnimation.createAnimation(entry.getKey(), entry.getValue(), instance.model,
                    GunMolangContext.sharedCompileEngine()));
        }
        instance.animations = map;
    }
    
    public static void buildMuzzleFlash(GunDisplayInstance instance, GunDisplay pojo) {
        instance.muzzleFlash = null;
        MuzzleFlash mf = pojo.muzzleFlash;
        if (mf == null) {
            return;
        }
        String boneName = mf.bone != null ? mf.bone : "muzzle_flash";
        instance.muzzleFlash = new MuzzleFlashConfig(boneName);
    }

    public static void buildMuzzles(GunDisplayInstance instance, GunDisplay pojo) {
        String fallback = instance.muzzleFlash != null ? instance.muzzleFlash.boneName() : "muzzle_flash";
        List<String> resolved = new ArrayList<>();
        if (pojo.muzzles != null) {
            for (String boneName : pojo.muzzles) {
                if (boneName != null && !boneName.isBlank()) {
                    resolved.add(boneName);
                }
            }
        }
        instance.muzzles = resolved.isEmpty() ? List.of(fallback) : List.copyOf(resolved);
    }

    public static void buildTracerColor(GunDisplayInstance instance, GunDisplay pojo) {
        instance.tracerColor = null;
        if (pojo.tracerColor == null || pojo.tracerColor.isEmpty()) {
            return;
        }
        try {
            String hex = pojo.tracerColor.startsWith("#") ? pojo.tracerColor.substring(1) : pojo.tracerColor;
            int rgb = Integer.parseInt(hex, 16);
            instance.tracerColor = new float[]{
                    ((rgb >> 16) & 0xFF) / 255.0f,
                    ((rgb >> 8) & 0xFF) / 255.0f,
                    (rgb & 0xFF) / 255.0f
            };
        } catch (NumberFormatException e) {
            LogUtils.getLogger().warn("[TACZ] Invalid tracer_color format: {}", pojo.tracerColor);
        }
    }

    public static void buildLaser(GunDisplayInstance instance, GunDisplay pojo) {
        instance.laser = null;
        Laser laserPojo = pojo.laser;
        if (laserPojo == null) {
            return;
        }
        /// > 起点骨骼缺省回退枪口火光骨骼，再无则回退默认名。
        String boneName = laserPojo.bone;
        if (boneName == null || boneName.isEmpty()) {
            boneName = instance.muzzleFlash != null ? instance.muzzleFlash.boneName() : "muzzle_flash";
        }
        float[] color = parseHexColor(laserPojo.color);
        if (color == null) {
            color = new float[]{1.0f, 0.1f, 0.1f};
        }
        float length = pojo.laser.length == null ? 0.0f : Math.max(0.0f, pojo.laser.length);
        instance.laser = new LaserConfig(boneName, color, length);
    }

    public static void buildFlashlight(GunDisplayInstance instance, GunDisplay pojo) {
        instance.flashlight = null;
        Flashlight lightPojo = pojo.flashlight;
        if (lightPojo == null) {
            return;
        }
        /// > 起点骨骼缺省回退枪口火光骨骼，再无则回退默认名。
        String boneName = lightPojo.bone;
        if (boneName == null || boneName.isEmpty()) {
            boneName = instance.muzzleFlash != null ? instance.muzzleFlash.boneName() : "muzzle_flash";
        }
        float[] color = parseHexColor(lightPojo.color);
        if (color == null) {
            color = new float[]{1.0f, 0.88f, 0.68f};
        }
        float range = lightPojo.range == null ? 0.0f : Math.max(0.0f, lightPojo.range);
        instance.flashlight = new FlashlightConfig(boneName, color, range);
    }

    public static void buildAimFov(GunDisplayInstance instance, GunDisplay pojo) {
        Preconditions.checkArgument(pojo.ironZoom > 0.0F, "iron_zoom must > 0");
        instance.ironZoom = pojo.ironZoom;
        instance.modelFov = pojo.modelFov > 0.0F ? pojo.modelFov : 70.0F;
        instance.zoomModelFov = pojo.zoomModelFov > 0.0F ? pojo.zoomModelFov : 70.0F;
    }

    /// > 解析战术拒枪姿态：定位组骨骼缺省 `tactical_view`，倍率 / FOV 缺省回退机瞄位对应值。
    /// > 骨骼在模型里不存在时视为该枪没有此姿态（告警并整条不进视图位列表），避免包作者写错名字后
    /// > 在游戏里切到一个取不到视点的空视图位。
    public static void buildTacticalStance(GunDisplayInstance instance, GunDisplay pojo) {
        instance.tacticalStance = null;
        GunDisplay.TacticalStance stance = pojo.tacticalStance;
        if (stance == null) {
            return;
        }
        String bone = stance.view == null || stance.view.isBlank() ? "tactical_view" : stance.view;
        if (instance.model.getIndex(bone) < 0) {
            LogUtils.getLogger().warn("[TACZ] tactical_stance bone '{}' not found in model {}", bone,
                    pojo.modelLocation);
            return;
        }
        float zoom = stance.zoom != null && stance.zoom > 0.0F ? stance.zoom : instance.ironZoom;
        float modelFov = stance.modelFov != null && stance.modelFov > 0.0F
                ? stance.modelFov : instance.modelFov;
        float zoomModelFov = stance.zoomModelFov != null && stance.zoomModelFov > 0.0F
                ? stance.zoomModelFov : instance.zoomModelFov;
        instance.tacticalStance = new TacticalStance(bone, zoom, modelFov, zoomModelFov);
    }

    /// > 解析 HUD 相关配置（枪图标 / 空弹图标 / 弹药计数样式 / 强制准星）。HUD 贴图缺省为 null（不渲染）。
    public static void buildHudConfig(GunDisplayInstance instance, GunDisplay pojo) {
        if (pojo.hudTexture != null) {
            instance.hudTexture = pojo.hudTexture.withPrefix("textures/").withSuffix(".png");
        } else {
            instance.hudTexture = null;
        }
        if (pojo.hudEmptyTexture != null) {
            instance.hudEmptyTexture = pojo.hudEmptyTexture.withPrefix("textures/").withSuffix(".png");
        } else {
            instance.hudEmptyTexture = null;
        }
        instance.ammoCountStyle = AmmoCountStyle.parse(pojo.ammoCountStyle);
        instance.showCrosshair = pojo.showCrosshair;
    }

    /// > 解析抛壳运动配置。`shell` 字段缺失则不抛壳（`shellEjection == null`）。
    public static void buildShellEjection(GunDisplayInstance instance, GunDisplay pojo) {
        instance.shellEjection = null;
        ShellEjection shell = pojo.shell;
        if (shell == null) {
            return;
        }
        instance.shellEjection = new ShellEjectionConfig(
                new Vector3f(shell.initialVelocity),
                new Vector3f(shell.randomVelocity),
                new Vector3f(shell.acceleration),
                new Vector3f(shell.angularVelocity),
                Math.max(0.0f, shell.livingTime)
        );
    }

    /// > 解析十六进制 RGB 颜色字符串（可带 # 或 0x），失败返回 null。
    @Nullable
    public static float[] parseHexColor(@Nullable String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) {
            return null;
        }
        try {
            String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
            if (hex.startsWith("0x") || hex.startsWith("0X")) {
                hex = hex.substring(2);
            }
            int rgb = Integer.parseInt(hex, 16);
            return new float[]{
                    ((rgb >> 16) & 0xFF) / 255.0f,
                    ((rgb >> 8) & 0xFF) / 255.0f,
                    (rgb & 0xFF) / 255.0f
            };
        } catch (NumberFormatException e) {
            LogUtils.getLogger().warn("[TACZ] Invalid laser color format: {}", hexColor);
            return null;
        }
    }

    /// > 基于 base 派生一个实例，但动画采用“base + extra”的合并策略。
    /// > 适用于：下挂只新增 underbarrel_* 动画，主枪仍使用 base 动画，避免重名覆盖。
    public static GunDisplayInstance deriveWithExtraAnimations(GunDisplayInstance base,
                                                              @Nullable Identifier extraAnimationLocation,
                                                              @Nullable Identifier extraDefaultAnimationLocation) {
        GunDisplayInstance instance = new GunDisplayInstance();
        instance.model = base.model;
        instance.modelTexture = base.modelTexture;
        instance.slots = base.slots;
        instance.slotTexture = base.slotTexture;

        instance.icaConfig = base.icaConfig;
        instance.controllerId = base.controllerId;
        instance.muzzleFlash = base.muzzleFlash;
        instance.muzzles = base.muzzles;
        instance.tracerColor = base.tracerColor;
        instance.laser = base.laser;
        instance.flashlight = base.flashlight;
        instance.shellEjection = base.shellEjection;
        instance.ironZoom = base.ironZoom;
        instance.modelFov = base.modelFov;
        instance.zoomModelFov = base.zoomModelFov;
        instance.tacticalStance = base.tacticalStance;
        instance.hudTexture = base.hudTexture;
        instance.hudEmptyTexture = base.hudEmptyTexture;
        instance.ammoCountStyle = base.ammoCountStyle;
        instance.showCrosshair = base.showCrosshair;

        /// > animations: merge base + extra
        if (extraAnimationLocation == null && extraDefaultAnimationLocation == null) {
            instance.animations = base.animations;
        } else {
            Map<String, BedrockAnimation> map = new HashMap<>(base.animations);

            if (extraDefaultAnimationLocation != null) {
                BedrockAnimationFile defaultPojo = ClientAssetsManager.INSTANCE.getAnimationPojo(extraDefaultAnimationLocation);
                Preconditions.checkArgument(defaultPojo != null, "there is no corresponding animation file: " + extraDefaultAnimationLocation);
                var defaultAnimations = BedrockAnimation.createAnimation(defaultPojo, instance.model,
                        GunMolangContext.sharedCompileEngine());
                for (var animation : defaultAnimations) {
                    map.put(animation.getName(), animation);
                }
            }

            if (extraAnimationLocation != null) {
                BedrockAnimationFile extraPojo = ClientAssetsManager.INSTANCE.getAnimationPojo(extraAnimationLocation);
                Preconditions.checkArgument(extraPojo != null, "there is no corresponding animation file: " + extraAnimationLocation);
                var extraAnimations = BedrockAnimation.createAnimation(extraPojo, instance.model,
                        GunMolangContext.sharedCompileEngine());
                for (var animation : extraAnimations) {
                    map.put(animation.getName(), animation);
                }
            }

            instance.animations = map;
        }

        return instance;
    }

    @Nullable
    public IcaConfig getIcaConfig() {
        return icaConfig;
    }

    @Nullable
    public MuzzleFlashConfig getMuzzleFlash() {
        return muzzleFlash;
    }

    /// > 解析指定枪口骨骼；缺省或越界时回退 0 号枪口。
    public String getMuzzleBone(int muzzleIndex) {
        return muzzles.get(muzzleIndex >= 0 && muzzleIndex < muzzles.size() ? muzzleIndex : 0);
    }

    /// > 当前 display 已声明的全部枪口骨骼，数组下标即 muzzleIndex。
    public List<String> getMuzzleBones() {
        return muzzles;
    }

    @Nullable
    public float[] getTracerColor() {
        return tracerColor;
    }

    @Nullable
    public LaserConfig getLaser() {
        return laser;
    }

    @Nullable
    public FlashlightConfig getFlashlight() {
        return flashlight;
    }

    @Nullable
    public ShellEjectionConfig getShellEjection() {
        return shellEjection;
    }

    public float getIronZoom() {
        return ironZoom;
    }

    public float getModelFov() {
        return modelFov;
    }

    public float getZoomModelFov() {
        return zoomModelFov;
    }

    @Nullable
    public TacticalStance getTacticalStance() {
        return tacticalStance;
    }

    /// > 战术拒枪姿态配置：定位组骨骼 + 单档倍率 + 模型 FOV 组。
    public record TacticalStance(String viewBone, float zoom, float modelFov, float zoomModelFov) {
    }

    public record MuzzleFlashConfig(String boneName) {}

    /// > 抛壳运动配置：初速度 + 随机速度幅度 + 加速度 + 三轴角速度（度/秒）+ 存活时间（秒）。
    /// > 弹壳在冻结的第一人称视图坐标系内做匀变速直线运动 + 匀角速度旋转。
    public record ShellEjectionConfig(Vector3f initialVelocity, Vector3f randomVelocity, Vector3f acceleration,
                                      Vector3f angularVelocity, float livingTime) {}

    /// > 镭射显示配置：起点骨骼名 + RGB 颜色（每分量 0~1） + 最大长度（米，<=0 表示用默认射程）。
    public record LaserConfig(String boneName, float[] color, float length) {}

    public record FlashlightConfig(String boneName, float[] color, float range) {}

    public static class IcaConfig {
        @Nullable
        private final String controlBoneName;
        private final int controlBoneIndex;
        private final float strengthMultiplier;
        @Nullable
        private final Map<Integer, Float> boneMultipliersByIndex;

        private IcaConfig(@Nullable String controlBoneName, int controlBoneIndex, float strengthMultiplier,
                          @Nullable Map<Integer, Float> boneMultipliersByIndex) {
            this.controlBoneName = controlBoneName;
            this.controlBoneIndex = controlBoneIndex;
            this.strengthMultiplier = strengthMultiplier;
            this.boneMultipliersByIndex = boneMultipliersByIndex != null ? Map.copyOf(boneMultipliersByIndex) : null;
        }

        @Nullable
        public String getControlBoneName() {
            return controlBoneName;
        }

        public int getControlBoneIndex() {
            return controlBoneIndex;
        }

        public float getStrengthMultiplier() {
            return strengthMultiplier;
        }

        @Nullable
        public Map<Integer, Float> getBoneMultipliersByIndex() {
            return boneMultipliersByIndex;
        }
    }

    public BedrockGunModel getGunModel() {
        return model;
    }

    public Map<String, BedrockAnimation> getAnimations() {
        return animations;
    }

    /// > 懒加载并缓存指定 montage 的编译结果。缓存 null 结果避免重复编译失败。
    /// >
    /// > `animationResolver` 用于把 montage 段引用的动画名解析为本 display 的 [BedrockAnimation]
    /// > （绑定本模型骨架），因此编译产物只在本 display 内复用。
    @Nullable
    public AnimationMontage<MontageContext> getOrCompileMontage(Identifier montageId,
                                                                Function<String, BedrockAnimation> animationResolver) {
        if (montageId == null) {
            return null;
        }
        if (montageCache.containsKey(montageId)) {
            return montageCache.get(montageId);
        }
        Montage definition = ClientAssetsManager.INSTANCE.getAnimationMontage(montageId);
        AnimationMontage<MontageContext> montage = definition != null
                ? MontageCompiler.compile(montageId, definition, animationResolver)
                : null;
        if (montage == null) {
            LogUtils.getLogger().warn("[TACZ] Animation montage not loaded: {}", montageId);
        }
        montageCache.put(montageId, montage);
        return montage;
    }

    @Nullable
    public Identifier getControllerId() {
        return controllerId;
    }

    public boolean hasAnimationController() {
        return controllerId != null;
    }

    public Identifier getModelTexture() {
        return modelTexture;
    }

    @Nullable
    public AttachmentSlotDisplay getSlot(Identifier location) {
        /// > todo 考虑返回一个默认的列表？
        if (slots == null) {
            return null;
        }
        return slots.get(location);
    }

    @Nullable
    public Identifier getSlotTexture() {
        return slotTexture;
    }

    @Nullable
    public Identifier getHudTexture() {
        return hudTexture;
    }

    @Nullable
    public Identifier getHudEmptyTexture() {
        return hudEmptyTexture;
    }

    public AmmoCountStyle getAmmoCountStyle() {
        return ammoCountStyle;
    }

    public boolean isShowCrosshair() {
        return showCrosshair;
    }
}
