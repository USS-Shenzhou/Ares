package cn.ussshenzhou.ares.gun.client.resource;

import cn.ussshenzhou.ares.Ares;
import com.mojang.logging.LogUtils;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockAnimationFile;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import cn.ussshenzhou.ares.gun.resource.GunGsonHelper;
import cn.ussshenzhou.ares.gun.resource.JsonDataManager;
import cn.ussshenzhou.ares.gun.client.animation.controller.StandardWeaponController;
import cn.ussshenzhou.ares.gun.client.animation.montage.Montage;
import cn.ussshenzhou.ares.gun.client.animation.mounted.MountedCompatibilityProfile;
import cn.ussshenzhou.ares.gun.client.animation.mounted.MountedOverlayResolved;
import cn.ussshenzhou.ares.gun.client.animation.mounted.MountedOverlayTemplate;
import cn.ussshenzhou.ares.gun.client.animation.statemachine.GunStateMachineGraph;
import cn.ussshenzhou.ares.gun.client.resource.display.AmmoDisplayInstance;
import cn.ussshenzhou.ares.gun.client.resource.display.AttachmentDisplayInstance;
import cn.ussshenzhou.ares.gun.client.resource.display.CharmDisplayInstance;
import cn.ussshenzhou.ares.gun.client.resource.display.GunDisplayInstance;
import cn.ussshenzhou.ares.gun.client.resource.display.SlotAdapterDisplayInstance;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/// @author TACZ, USS_Shenzhou
public enum ClientAssetsManager {
    INSTANCE;
    private JsonDataManager<BedrockModelPOJO> models;
    private JsonDataManager<BedrockAnimationFile> animations;
    private JsonDataManager<Montage> animationMontages;
    private JsonDataManager<StandardWeaponController> animationControllers;
    private AnimationStateMachineManager animationStateMachines;
    private JsonDataManager<MountedCompatibilityProfile> animationMountedProfiles;
    /// > 预解析的 host overlay 运行时数据：(hostDisplayId, mountedId) -> resolved。仅含显式声明 grip 的 combo。
    private final Map<String, MountedOverlayResolved> resolvedOverlays = new HashMap<>();
    private GunDisplayManager gunDisplayManager;
    private AttachmentDisplayManager attachmentDisplayManager;
    private AttachmentDisplayManager variantDisplayManager;
    private AmmoDisplayManager ammoDisplayManager;
    private CharmDisplayManager charmDisplayManager;
    private SlotAdapterDisplayManager slotAdapterDisplayManager;
    private UnderBarrelOverrideManager underBarrelOverrideManager;

    // FIXME REVIEW listeners are registered through AddClientReloadListenersEvent.addListener with ares:client/... ids and run in
    // the order they are added.
    // FIXME REVIEW the automatic sound registration of TACZ is not ported; the files under assets/ares/tacz_sounds are copied but
    // cannot be played until sounds are handled separately.
    public void registerListeners(BiConsumer<Identifier, PreparableReloadListener> consumer) {
        models = this.create(BedrockModelPOJO.class, "geo_models", "BedrockModelPojo", consumer);
        animations = this.create(BedrockAnimationFile.class, new FileToIdConverter("animations", ".animation.json"), "BedrockAnimationPojo", consumer);
        animationMontages = this.create(Montage.class, "animations/montages", "AnimationMontage", consumer);
        animationControllers = this.create(StandardWeaponController.class, "animations/controllers", "AnimationController", consumer);
        animationStateMachines = new AnimationStateMachineManager();
        consumer.accept(Ares.modLoc("client/animation_state_machines"), animationStateMachines);
        animationMountedProfiles = this.create(MountedCompatibilityProfile.class, "animations/mounted_profiles", "AnimationMountedProfile", consumer);

        gunDisplayManager = new GunDisplayManager();
        consumer.accept(Ares.modLoc("client/gun_display"), gunDisplayManager);

        underBarrelOverrideManager = new UnderBarrelOverrideManager();
        consumer.accept(Ares.modLoc("client/under_barrel_override"), underBarrelOverrideManager);

        attachmentDisplayManager = new AttachmentDisplayManager();
        consumer.accept(Ares.modLoc("client/attachment_display"), attachmentDisplayManager);

        /// > 配件变体复用 attachment display 结构，仅目录不同
        variantDisplayManager = new AttachmentDisplayManager("display/attachment_variants");
        consumer.accept(Ares.modLoc("client/variant_display"), variantDisplayManager);

        slotAdapterDisplayManager = new SlotAdapterDisplayManager();
        consumer.accept(Ares.modLoc("client/slot_adapter_display"), slotAdapterDisplayManager);

        ammoDisplayManager = new AmmoDisplayManager();
        consumer.accept(Ares.modLoc("client/ammo_display"), ammoDisplayManager);

        charmDisplayManager = new CharmDisplayManager();
        consumer.accept(Ares.modLoc("client/charm_display"), charmDisplayManager);

        /// > 预解析 host overlay：必须在 gunDisplay 加载之后（host 模型可用）、原始动画 pojo 清理之前执行。
        /// > 按解析链（combo profile 显式 grip + mounted template）将动画绑定到各 host 模型骨架，构建 resolvedOverlays。
        consumer.accept(Ares.modLoc("client/host_overlays"), new SimplePreparableReloadListener<Void>() {
            @Override
            @ParametersAreNonnullByDefault
            protected @Nonnull Void prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
                return null;
            }

            @Override
            @ParametersAreNonnullByDefault
            protected void apply(Void pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
                resolveHostOverlays();
            }
        });

        /// > 完成加载后清理临时数据
        consumer.accept(Ares.modLoc("client/clear_raw_data"), new SimplePreparableReloadListener<Void>() {
            @Override
            @ParametersAreNonnullByDefault
            protected @Nonnull Void prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
                return null;
            }

            @Override
            @ParametersAreNonnullByDefault
            protected void apply(Void pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
                models.clearData();
                animations.clearData();
            }
        });
    }

    /// > 预构建所有显式声明 grip 的 combo 的 host overlay 运行时数据。
    /// >
    /// > combo profile 必须显式提供 [MountedCompatibilityProfile#grip]；operation / grip_blend / transitions
    /// > 直接从同一个 combo 文件读取。持枪姿态与操作差量动画的关键帧均驱动 host 骨骼，boneIndex 必须绑定到
    /// > host 模型，因此按各 combo 的 host_display 模型单独编译。必须在原始动画 pojo被清理前调用。
    private void resolveHostOverlays() {
        resolvedOverlays.clear();
        if (animationMountedProfiles == null) {
            return;
        }
        for (MountedCompatibilityProfile profile : animationMountedProfiles.getAllData().values()) {
            if (profile == null || profile.match == null || profile.match.hostDisplay == null || profile.match.mounted == null) {
                continue;
            }
            /// > 持枪姿态必须显式声明，否则不启用 overlay。
            if (profile.grip == null || profile.grip.animationFile == null || profile.grip.animation == null) {
                continue;
            }
            Optional<GunDisplayInstance> hostDisplay = getGunDisplay(profile.match.hostDisplay);
            if (hostDisplay.isEmpty() || hostDisplay.get().getGunModel() == null) {
                LogUtils.getLogger().warn("[TACZ] host overlay host display/model missing: {}", profile.match.hostDisplay);
                continue;
            }
            cn.ussshenzhou.ares.gun.client.model.BedrockGunModel hostModel = hostDisplay.get().getGunModel();

            /// > 持枪姿态动画（绑定 host 模型）。
            BedrockAnimation gripAnimation = compileAnimation(profile.grip.animationFile, profile.grip.animation, hostModel);
            if (gripAnimation == null) {
                LogUtils.getLogger().warn("[TACZ] host overlay grip animation missing: file={}, animation={}",
                        profile.grip.animationFile, profile.grip.animation);
                continue;
            }

            String gripBlendMode = "layered";
            java.util.List<MountedOverlayTemplate.GripGroup> gripGroups = java.util.List.of();
            Map<String, BedrockAnimation> operationAnimations = compileOperationAnimations(profile.operation, hostModel);
            BedrockAnimation transitionToUnderBarrel = null;
            BedrockAnimation transitionToPrimary = null;
            if (profile.gripBlend != null) {
                gripBlendMode = profile.gripBlend.mode != null ? profile.gripBlend.mode : "layered";
                gripGroups = profile.gripBlend.groups != null ? profile.gripBlend.groups : java.util.List.of();
            }
            if (profile.transitions != null) {
                transitionToUnderBarrel = compileTransitionClip(profile.transitions.toUnderBarrel, hostModel);
                transitionToPrimary = compileTransitionClip(profile.transitions.toPrimary, hostModel);
            }

            MountedOverlayResolved resolved = new MountedOverlayResolved(gripAnimation, gripBlendMode, gripGroups,
                    operationAnimations, null, transitionToUnderBarrel, transitionToPrimary);
            resolvedOverlays.put(overlayKey(profile.match.hostDisplay, profile.match.mounted), resolved);
        }
    }

    /// > 把某个动画文件中的指定动画绑定到 host 模型骨架并编译为 [BedrockAnimation]。找不到返回 null。
    @Nullable
    private BedrockAnimation compileAnimation(Identifier fileId, String animationName,
                                              cn.ussshenzhou.ares.gun.client.model.BedrockGunModel hostModel) {
        BedrockAnimationFile file = animations.getData(fileId);
        if (file == null) {
            return null;
        }
        for (BedrockAnimation animation : BedrockAnimation.createAnimation(file, hostModel)) {
            if (animationName.equals(animation.getName())) {
                return animation;
            }
        }
        return null;
    }

    private Map<String, BedrockAnimation> compileOperationAnimations(
            MountedCompatibilityProfile.Operation operation, cn.ussshenzhou.ares.gun.client.model.BedrockGunModel hostModel) {
        if (operation == null || operation.animationFile == null) {
            return Map.of();
        }
        BedrockAnimationFile file = animations.getData(operation.animationFile);
        if (file == null) {
            LogUtils.getLogger().warn("[TACZ] host overlay operation animation file missing: {}", operation.animationFile);
            return Map.of();
        }
        Map<String, BedrockAnimation> compiled = new HashMap<>();
        for (BedrockAnimation animation : BedrockAnimation.createAnimation(file, hostModel)) {
            compiled.put(animation.getName(), animation);
        }
        return compiled;
    }

    private static String overlayKey(Identifier hostDisplayId, Identifier mountedId) {
        return hostDisplayId + "|" + mountedId;
    }

    /// > 编译模式切换过渡段动画（绑定到 host 模型骨架）。缺省或失败返回 null（回退到纯权重淡入淡出）。
    @Nullable
    private BedrockAnimation compileTransitionClip(MountedOverlayTemplate.TransitionClip clip,
                                                   cn.ussshenzhou.ares.gun.client.model.BedrockGunModel hostModel) {
        if (clip == null || clip.animationFile == null || clip.animation == null) {
            return null;
        }
        BedrockAnimation animation = compileAnimation(clip.animationFile, clip.animation, hostModel);
        if (animation == null) {
            LogUtils.getLogger().warn("[TACZ] host overlay transition animation missing: file={}, animation={}",
                    clip.animationFile, clip.animation);
        }
        return animation;
    }

    public <T> JsonDataManager<T> create(Class<T> clazz, String folder, String marker, BiConsumer<Identifier, PreparableReloadListener> consumer) {
        JsonDataManager<T> manager = new JsonDataManager<>(clazz, GunGsonHelper.GSON, folder, marker);
        consumer.accept(Ares.modLoc("client/" + folder), manager);
        return manager;
    }

    public <T> JsonDataManager<T> create(Class<T> clazz, FileToIdConverter mapper, String marker, BiConsumer<Identifier, PreparableReloadListener> consumer) {
        JsonDataManager<T> manager = new JsonDataManager<>(clazz, GunGsonHelper.GSON, mapper, marker);
        consumer.accept(Ares.modLoc("client/" + marker.toLowerCase(Locale.ROOT)), manager);
        return manager;
    }

    @Nullable
    public BedrockModelPOJO getModelPojo(Identifier path) {
        return models.getData(path);
    }

    @Nullable
    public BedrockAnimationFile getAnimationPojo(Identifier path) {
        return animations.getData(path);
    }
    
    @Nullable
    public Montage getAnimationMontage(Identifier path) {
        return animationMontages.getData(path);
    }

    @Nullable
    public StandardWeaponController getAnimationController(Identifier path) {
        return animationControllers.getData(path);
    }

    @Nullable
    public GunStateMachineGraph getAnimationStateMachine(Identifier path) {
        return animationStateMachines.getData(path);
    }

    /// > 按 host 枪 display id + 下挂 id 查询预解析的 host overlay 运行时数据。
    /// > 仅当该 combo 显式声明了持枪姿态（grip）时才有结果，否则返回 null（不挂 overlay）。
    @Nullable
    public MountedOverlayResolved getResolvedOverlay(Identifier hostDisplayId, Identifier mountedId) {
        if (hostDisplayId == null || mountedId == null) {
            return null;
        }
        return resolvedOverlays.get(overlayKey(hostDisplayId, mountedId));
    }

    @Nonnull
    public Optional<GunDisplayInstance> getGunDisplay(Identifier path) {
        return gunDisplayManager.getDisplay(path);
    }

    @Nonnull
    public Optional<GunDisplayInstance> getGunDisplayWithUnderBarrelOverride(ItemStack stack, Identifier displayId) {
        Optional<GunDisplayInstance> baseOpt = gunDisplayManager.getDisplay(displayId);
        if (baseOpt.isEmpty() || underBarrelOverrideManager == null) {
            return baseOpt;
        }
        return underBarrelOverrideManager.applyIfMatched(stack, displayId, baseOpt.get());
    }

    @Nonnull
    public Optional<GunDisplayInstance> getMountedDisplayWithUnderBarrelOverride(ItemStack gunStack, Identifier gunDisplayId, Identifier mountedDisplayId) {
        if (underBarrelOverrideManager == null) {
            return getGunDisplay(mountedDisplayId);
        }
        return underBarrelOverrideManager.applyMountedIfMatched(gunStack, gunDisplayId, mountedDisplayId);
    }

    @Nonnull
    public Optional<AttachmentDisplayInstance> getAttachmentDisplay(Identifier path) {
        return attachmentDisplayManager.getDisplay(path);
    }

    /// > 按资源路径解析显示实例：先查配件 display 目录，再查变体 display 目录
    /// > （`display/attachment_variants`，作为可复用几何资产池，供模式关态几何等引用）。
    @Nonnull
    public Optional<AttachmentDisplayInstance> getDisplay(Identifier path) {
        Optional<AttachmentDisplayInstance> base = attachmentDisplayManager.getDisplay(path);
        if (base.isPresent()) {
            return base;
        }
        if (variantDisplayManager != null) {
            return variantDisplayManager.getDisplay(path);
        }
        return Optional.empty();
    }

    /// > 取配件变体的 display（结构同 attachment display，目录为 display/attachment_variants）。
    @Nonnull
    public Optional<AttachmentDisplayInstance> getVariantDisplay(Identifier path) {
        if (variantDisplayManager == null) {
            return Optional.empty();
        }
        return variantDisplayManager.getDisplay(path);
    }

    /// > 取槽位适配器的 display。
    @Nonnull
    public Optional<SlotAdapterDisplayInstance> getSlotAdapterDisplay(Identifier path) {
        if (slotAdapterDisplayManager == null) {
            return Optional.empty();
        }
        return slotAdapterDisplayManager.getDisplay(path);
    }

    /// > 取弹药的 display（含弹壳模型）。
    @Nonnull
    public Optional<AmmoDisplayInstance> getAmmoDisplay(Identifier path) {
        if (ammoDisplayManager == null) {
            return Optional.empty();
        }
        return ammoDisplayManager.getDisplay(path);
    }

    /// > 取挂饰的 display（模型 + 贴图 + 渲染变换）。
    @Nonnull
    public Optional<CharmDisplayInstance> getCharmDisplay(Identifier path) {
        if (charmDisplayManager == null) {
            return Optional.empty();
        }
        return charmDisplayManager.getDisplay(path);
    }

    public void putModel(Identifier id, BedrockModelPOJO pojo) {
        models.putData(id, pojo);
    }
    
    public void putAnimation(Identifier id, BedrockAnimationFile pojo) {
        animations.putData(id, pojo);
    }
    
    public void putGunDisplay(Identifier id, GunDisplayInstance instance) {
        gunDisplayManager.putDisplay(id, instance);
    }
}
