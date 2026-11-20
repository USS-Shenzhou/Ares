package cn.ussshenzhou.ares.client.resource.display;

import cn.ussshenzhou.ares.util.AmmoCountStyle;
import com.mojang.logging.LogUtils;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockAnimationFile;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockAnimationPOJO;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.google.common.base.Preconditions;
import com.maydaymemory.mae.control.montage.AnimationMontage;
import cn.ussshenzhou.ares.client.animation.montage.MontageCompiler;
import cn.ussshenzhou.ares.client.animation.montage.MontageContext;
import cn.ussshenzhou.ares.client.animation.montage.MontageCfg;
import cn.ussshenzhou.ares.client.animation.statemachine.GunMolangContext;
import cn.ussshenzhou.ares.client.model.BedrockGunModel;
import cn.ussshenzhou.ares.client.resource.ClientAssetsManager;
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
public class GunDisplay {
    private BedrockGunModel model;
    private Identifier modelTexture;
    private Map<String, BedrockAnimation> animations;

    @Nullable
    private Identifier controllerId;
    private Map<Identifier, AttachmentSlotDisplayCfg> slots;

    private Identifier slotTexture;

    @Nullable
    private Ica icaConfig;

    @Nullable
    private MuzzleFlash muzzleFlash;
    private List<String> muzzles = List.of("muzzle_flash");

    private float[] tracerColor = null;

    @Nullable
    private Laser laser;

    @Nullable
    private Flashlight flashlight;

    @Nullable
    private ShellEjection shellEjection;
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
    private GunDisplay oneHandLeft;
    @Nullable
    private GunDisplay oneHandRight;

    /// > 懒加载并缓存的 montage 编译结果。montage 编译依赖本 display 的动画表（绑定到本模型骨架），
    /// > 故按 display 维度缓存，跨该 display 派生的所有动画实例复用；随 display 随资源 reload 重建而失效。
    private final Map<Identifier, AnimationMontage<MontageContext>> montageCache = new HashMap<>();

    /// > 必须在资源包重载期间构建，否则所需的资源缓存不可用
    public static GunDisplay of(GunDisplayCfg cfg) {
        GunDisplay instance = new GunDisplay();

        buildModelAndTexture(instance, cfg);
        buildIcaConfig(instance, cfg);
        buildAnimations(instance, cfg);
        instance.controllerId = cfg.controller;
        buildMuzzleFlash(instance, cfg);
        buildMuzzles(instance, cfg);
        buildTracerColor(instance, cfg);
        buildLaser(instance, cfg);
        buildFlashlight(instance, cfg);
        buildShellEjection(instance, cfg);
        buildAimFov(instance, cfg);
        buildTacticalStance(instance, cfg);
        buildHudConfig(instance, cfg);
        buildOneHandVariants(instance, cfg);

        return instance;
    }

    /// > 构建单手双持按手变体。变体复用本体的 controller / slots / ICA / 抛壳 / 枪口火光等配置，
    /// > 仅替换模型、贴图与动画集（绑定到各自模型骨架）。任一字段缺省则回退本体对应值。
    public static void buildOneHandVariants(GunDisplay instance, GunDisplayCfg cfg) {
        if (cfg.oneHand == null) {
            return;
        }
        instance.oneHandLeft = buildVariant(instance, cfg, cfg.oneHand.left);
        instance.oneHandRight = buildVariant(instance, cfg, cfg.oneHand.right);
    }

    @Nullable
    private static GunDisplay buildVariant(GunDisplay base, GunDisplayCfg gunDisplayCfg,
                                           @Nullable GunDisplayCfg.VariantCfg variantCfg) {
        if (variantCfg == null) {
            return null;
        }
        /// > 用本体 pojo 作为模板，仅覆盖模型/贴图/动画字段后重新构建一个完整实例。
        GunDisplayCfg merged = new GunDisplayCfg();
        merged.modelLocation = variantCfg.modelLocation != null ? variantCfg.modelLocation : gunDisplayCfg.modelLocation;
        merged.modelTexture = variantCfg.modelTexture != null ? variantCfg.modelTexture : gunDisplayCfg.modelTexture;
        merged.animationLocation = variantCfg.animationLocation != null ? variantCfg.animationLocation : gunDisplayCfg.animationLocation;
        merged.defaultAnimation = variantCfg.defaultAnimation != null ? variantCfg.defaultAnimation : gunDisplayCfg.defaultAnimation;
        merged.animationPoseCompositions = resolveAnimationPoseCompositions(gunDisplayCfg, variantCfg);
        merged.controller = gunDisplayCfg.controller;
        merged.attachmentSlotDisplay = gunDisplayCfg.attachmentSlotDisplay;
        merged.slotTexture = gunDisplayCfg.slotTexture;
        merged.ironZoom = gunDisplayCfg.ironZoom;
        merged.modelFov = gunDisplayCfg.modelFov;
        merged.zoomModelFov = gunDisplayCfg.zoomModelFov;
        merged.tacticalStance = gunDisplayCfg.tacticalStance;
        merged.icaControlBone = gunDisplayCfg.icaControlBone;
        merged.icaStrengthMultiplier = gunDisplayCfg.icaStrengthMultiplier;
        merged.icaBoneMultipliers = gunDisplayCfg.icaBoneMultipliers;
        merged.muzzleFlash = gunDisplayCfg.muzzleFlash;
        merged.muzzles = gunDisplayCfg.muzzles;
        merged.tracerColor = gunDisplayCfg.tracerColor;
        merged.laser = gunDisplayCfg.laser;
        merged.shell = gunDisplayCfg.shell;
        /// > 变体不再递归 one_hand，避免无限递归。
        merged.oneHand = null;

        GunDisplay variantInstance = new GunDisplay();
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
    static List<AnimationPoseCompositionCfg> resolveAnimationPoseCompositions(GunDisplayCfg gunDisplayCfg,
                                                                              @Nullable GunDisplayCfg.VariantCfg variantCfg) {
        if (variantCfg != null && variantCfg.animationPoseCompositions != null) {
            return variantCfg.animationPoseCompositions;
        }
        return gunDisplayCfg.animationPoseCompositions;
    }

    /// > 取单手双持按手变体：`MAIN_HAND` → right，`OFF_HAND` → left。
    /// > 未声明对应变体时返回本体（回退到双手 display，验证期允许）。
    public GunDisplay forHand(InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND) {
            return oneHandLeft != null ? oneHandLeft : this;
        }
        return oneHandRight != null ? oneHandRight : this;
    }

    public boolean hasOneHandVariants() {
        return oneHandLeft != null || oneHandRight != null;
    }

    /// > 加载模型和贴图
    public static void buildModelAndTexture(GunDisplay instance, GunDisplayCfg cfg) {
        Preconditions.checkArgument(cfg.modelLocation != null, "display object missing model field");
        BedrockModelPOJO modelPojo = ClientAssetsManager.INSTANCE.getModelPojo(cfg.modelLocation);
        Preconditions.checkArgument(modelPojo != null, "there is no corresponding model file: " + cfg.modelLocation);
        instance.model = new BedrockGunModel(modelPojo, cfg.attachmentSlotDisplay);
        instance.slots = cfg.attachmentSlotDisplay;
        Preconditions.checkArgument(cfg.modelTexture != null, "display object missing texture field");
        instance.modelTexture = cfg.modelTexture.withPrefix("textures/").withSuffix(".png");

        if (cfg.slotTexture != null) {
            instance.slotTexture = cfg.slotTexture.withPrefix("textures/").withSuffix(".png");
        }
    }

    public static void buildIcaConfig(GunDisplay instance, GunDisplayCfg cfg) {
        instance.icaConfig = null;

        float strengthMultiplier = 1.0f;
        if (cfg.icaStrengthMultiplier != null) {
            strengthMultiplier = Math.max(0.0f, Math.min(1.0f, cfg.icaStrengthMultiplier));
        }

        int controlBoneIndex = -1;
        String controlBoneName = cfg.icaControlBone;
        if (controlBoneName != null) {
            int index = instance.model.getIndex(controlBoneName);
            if (index >= 0) {
                controlBoneIndex = index;
            }
        }

        Map<Integer, Float> boneMultipliersByIndex = new HashMap<>();

        if (cfg.icaBoneMultipliers != null && !cfg.icaBoneMultipliers.isEmpty()) {
            for (var e : cfg.icaBoneMultipliers.entrySet()) {
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

        instance.icaConfig = new Ica(controlBoneName, controlBoneIndex, strengthMultiplier, boneMultipliersByIndex);
    }
    /// > 加载动画
    public static void buildAnimations(GunDisplay instance, GunDisplayCfg cfg) {
        Preconditions.checkArgument(cfg.animationLocation != null, "display object missing animation field");
        BedrockAnimationFile animationPojo = ClientAssetsManager.INSTANCE.getAnimationPojo(cfg.animationLocation);
        Preconditions.checkArgument(animationPojo != null, "there is no corresponding animation file: " + cfg.animationLocation);

        BedrockAnimationFile defaultAnimationPojo = null;
        if (cfg.defaultAnimation != null) {
            defaultAnimationPojo = ClientAssetsManager.INSTANCE.getAnimationPojo(cfg.defaultAnimation);
            Preconditions.checkArgument(defaultAnimationPojo != null, "there is no corresponding animation file: " + cfg.defaultAnimation);
        }

        Map<String, BedrockAnimationPOJO> resolvedAnimations = new HashMap<>();
        if (defaultAnimationPojo != null && defaultAnimationPojo.getAnimations() != null) {
            resolvedAnimations.putAll(defaultAnimationPojo.getAnimations());
        }
        if (animationPojo.getAnimations() != null) {
            resolvedAnimations.putAll(animationPojo.getAnimations());
        }
        resolvedAnimations = AnimationPoseCompositionHelper.compose(resolvedAnimations, cfg.animationPoseCompositions);

        Map<String, BedrockAnimation> map = new HashMap<>();
        for (Map.Entry<String, BedrockAnimationPOJO> entry : resolvedAnimations.entrySet()) {
            map.put(entry.getKey(), BedrockAnimation.createAnimation(entry.getKey(), entry.getValue(), instance.model,
                    GunMolangContext.sharedCompileEngine()));
        }
        instance.animations = map;
    }
    
    public static void buildMuzzleFlash(GunDisplay instance, GunDisplayCfg gunDisplayCfg) {
        instance.muzzleFlash = null;
        MuzzleFlashCfg mf = gunDisplayCfg.muzzleFlash;
        if (mf == null) {
            return;
        }
        String boneName = mf.bone != null ? mf.bone : "muzzle_flash";
        instance.muzzleFlash = new MuzzleFlash(boneName);
    }

    public static void buildMuzzles(GunDisplay instance, GunDisplayCfg cfg) {
        String fallback = instance.muzzleFlash != null ? instance.muzzleFlash.boneName() : "muzzle_flash";
        List<String> resolved = new ArrayList<>();
        if (cfg.muzzles != null) {
            for (String boneName : cfg.muzzles) {
                if (boneName != null && !boneName.isBlank()) {
                    resolved.add(boneName);
                }
            }
        }
        instance.muzzles = resolved.isEmpty() ? List.of(fallback) : List.copyOf(resolved);
    }

    public static void buildTracerColor(GunDisplay instance, GunDisplayCfg cfg) {
        instance.tracerColor = null;
        if (cfg.tracerColor == null || cfg.tracerColor.isEmpty()) {
            return;
        }
        try {
            String hex = cfg.tracerColor.startsWith("#") ? cfg.tracerColor.substring(1) : cfg.tracerColor;
            int rgb = Integer.parseInt(hex, 16);
            instance.tracerColor = new float[]{
                    ((rgb >> 16) & 0xFF) / 255.0f,
                    ((rgb >> 8) & 0xFF) / 255.0f,
                    (rgb & 0xFF) / 255.0f
            };
        } catch (NumberFormatException e) {
            LogUtils.getLogger().warn("[TACZ] Invalid tracer_color format: {}", cfg.tracerColor);
        }
    }

    public static void buildLaser(GunDisplay instance, GunDisplayCfg gunDisplayCfg) {
        instance.laser = null;
        LaserCfg laserCfg = gunDisplayCfg.laser;
        if (laserCfg == null) {
            return;
        }
        /// > 起点骨骼缺省回退枪口火光骨骼，再无则回退默认名。
        String boneName = laserCfg.bone;
        if (boneName == null || boneName.isEmpty()) {
            boneName = instance.muzzleFlash != null ? instance.muzzleFlash.boneName() : "muzzle_flash";
        }
        float[] color = parseHexColor(laserCfg.color);
        if (color == null) {
            color = new float[]{1.0f, 0.1f, 0.1f};
        }
        float length = gunDisplayCfg.laser.length == null ? 0.0f : Math.max(0.0f, gunDisplayCfg.laser.length);
        instance.laser = new Laser(boneName, color, length);
    }

    public static void buildFlashlight(GunDisplay instance, GunDisplayCfg gunDisplayCfg) {
        instance.flashlight = null;
        FlashlightCfg lightCfg = gunDisplayCfg.flashlight;
        if (lightCfg == null) {
            return;
        }
        /// > 起点骨骼缺省回退枪口火光骨骼，再无则回退默认名。
        String boneName = lightCfg.bone;
        if (boneName == null || boneName.isEmpty()) {
            boneName = instance.muzzleFlash != null ? instance.muzzleFlash.boneName() : "muzzle_flash";
        }
        float[] color = parseHexColor(lightCfg.color);
        if (color == null) {
            color = new float[]{1.0f, 0.88f, 0.68f};
        }
        float range = lightCfg.range == null ? 0.0f : Math.max(0.0f, lightCfg.range);
        instance.flashlight = new Flashlight(boneName, color, range);
    }

    public static void buildAimFov(GunDisplay instance, GunDisplayCfg cfg) {
        Preconditions.checkArgument(cfg.ironZoom > 0.0F, "iron_zoom must > 0");
        instance.ironZoom = cfg.ironZoom;
        instance.modelFov = cfg.modelFov > 0.0F ? cfg.modelFov : 70.0F;
        instance.zoomModelFov = cfg.zoomModelFov > 0.0F ? cfg.zoomModelFov : 70.0F;
    }

    /// > 解析战术拒枪姿态：定位组骨骼缺省 `tactical_view`，倍率 / FOV 缺省回退机瞄位对应值。
    /// > 骨骼在模型里不存在时视为该枪没有此姿态（告警并整条不进视图位列表），避免包作者写错名字后
    /// > 在游戏里切到一个取不到视点的空视图位。
    public static void buildTacticalStance(GunDisplay instance, GunDisplayCfg gunDisplayCfg) {
        instance.tacticalStance = null;
        GunDisplayCfg.TacticalStanceCfg stance = gunDisplayCfg.tacticalStance;
        if (stance == null) {
            return;
        }
        String bone = stance.view == null || stance.view.isBlank() ? "tactical_view" : stance.view;
        if (instance.model.getIndex(bone) < 0) {
            LogUtils.getLogger().warn("[TACZ] tactical_stance bone '{}' not found in model {}", bone,
                    gunDisplayCfg.modelLocation);
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
    public static void buildHudConfig(GunDisplay instance, GunDisplayCfg cfg) {
        if (cfg.hudTexture != null) {
            instance.hudTexture = cfg.hudTexture.withPrefix("textures/").withSuffix(".png");
        } else {
            instance.hudTexture = null;
        }
        if (cfg.hudEmptyTexture != null) {
            instance.hudEmptyTexture = cfg.hudEmptyTexture.withPrefix("textures/").withSuffix(".png");
        } else {
            instance.hudEmptyTexture = null;
        }
        instance.ammoCountStyle = AmmoCountStyle.parse(cfg.ammoCountStyle);
        instance.showCrosshair = cfg.showCrosshair;
    }

    /// > 解析抛壳运动配置。`shell` 字段缺失则不抛壳（`shellEjection == null`）。
    public static void buildShellEjection(GunDisplay instance, GunDisplayCfg gunDisplayCfg) {
        instance.shellEjection = null;
        ShellEjectionCfg shell = gunDisplayCfg.shell;
        if (shell == null) {
            return;
        }
        instance.shellEjection = new ShellEjection(
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
    public static GunDisplay deriveWithExtraAnimations(GunDisplay base,
                                                       @Nullable Identifier extraAnimationLocation,
                                                       @Nullable Identifier extraDefaultAnimationLocation) {
        GunDisplay instance = new GunDisplay();
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
    public Ica getIcaConfig() {
        return icaConfig;
    }

    @Nullable
    public MuzzleFlash getMuzzleFlash() {
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
    public Laser getLaser() {
        return laser;
    }

    @Nullable
    public Flashlight getFlashlight() {
        return flashlight;
    }

    @Nullable
    public ShellEjection getShellEjection() {
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

    public record MuzzleFlash(String boneName) {}

    /// > 抛壳运动配置：初速度 + 随机速度幅度 + 加速度 + 三轴角速度（度/秒）+ 存活时间（秒）。
    /// > 弹壳在冻结的第一人称视图坐标系内做匀变速直线运动 + 匀角速度旋转。
    public record ShellEjection(Vector3f initialVelocity, Vector3f randomVelocity, Vector3f acceleration,
                                Vector3f angularVelocity, float livingTime) {}

    /// > 镭射显示配置：起点骨骼名 + RGB 颜色（每分量 0~1） + 最大长度（米，<=0 表示用默认射程）。
    public record Laser(String boneName, float[] color, float length) {}

    public record Flashlight(String boneName, float[] color, float range) {}

    public static class Ica {
        @Nullable
        private final String controlBoneName;
        private final int controlBoneIndex;
        private final float strengthMultiplier;
        @Nullable
        private final Map<Integer, Float> boneMultipliersByIndex;

        private Ica(@Nullable String controlBoneName, int controlBoneIndex, float strengthMultiplier,
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
        MontageCfg cfg = ClientAssetsManager.INSTANCE.getAnimationMontage(montageId);
        AnimationMontage<MontageContext> montage = cfg != null
                ? MontageCompiler.compile(montageId, cfg, animationResolver)
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
    public AttachmentSlotDisplayCfg getSlot(Identifier location) {
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
