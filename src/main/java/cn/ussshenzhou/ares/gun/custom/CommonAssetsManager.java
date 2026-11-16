package cn.ussshenzhou.ares.gun.custom;

import cn.ussshenzhou.ares.Ares;
import com.mojang.logging.LogUtils;
import com.google.common.collect.ImmutableMap;
import cn.ussshenzhou.ares.gun.TimelessHelperS;
import cn.ussshenzhou.ares.gun.attribute.GunAttribute;
import cn.ussshenzhou.ares.gun.resource.ICommonAssetsManager;
import cn.ussshenzhou.ares.gun.resource.IndexTagLookup;
import cn.ussshenzhou.ares.gun.custom.attachment.AttachmentIndex;
import cn.ussshenzhou.ares.gun.custom.attachment.AttachmentData;
import cn.ussshenzhou.ares.gun.custom.gun.AmmoEntry;
import cn.ussshenzhou.ares.gun.custom.gun.FireModeEntry;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import cn.ussshenzhou.ares.gun.custom.gun.GunIndex;
import cn.ussshenzhou.ares.gun.custom.slotadapter.SlotAdapterIndex;
import cn.ussshenzhou.ares.gun.custom.underbarrel.UnderBarrelGunIndex;
import cn.ussshenzhou.ares.gun.custom.variant.VariantIndex;
import cn.ussshenzhou.ares.gun.init.ModCustomItemTypes;
import cn.ussshenzhou.ares.gun.custom.tag.IndexTagDomains;
import cn.ussshenzhou.ares.gun.custom.tag.IndexTagManager;
import cn.ussshenzhou.ares.gun.custom.tag.IndexTagRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.resource.ResourcePackLoader;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW network sync removed: snapshots, compression, slicing, IndexTagNetworkCodec, NetworkCache, ClientCache,
// fromNetwork and the network caches of attributes and indexes.
// FIXME REVIEW built-in content only contains m4a1, m9a4, ai_awp and m870 copied from the TACZ default pack, with the tacz
// namespace and translation keys renamed to ares (assets/ares, data/ares, lang).
@EventBusSubscriber(modid = Ares.MOD_ID)
public class CommonAssetsManager implements ICommonAssetsManager {
    private static final CommonAssetsManager EMPTY = new CommonAssetsManager();

    private static volatile CommonAssetsManager PUBLISHED;
    private static volatile CommonAssetsManager INITIAL_LOADING_MANAGER;
    private static volatile CommonAssetsManager CLIENT_BUILTIN;

    public static ICommonAssetsManager get() {
        CommonAssetsManager published = PUBLISHED;
        if (published != null) {
            return published;
        }
        CommonAssetsManager loadingManager = INITIAL_LOADING_MANAGER;
        if (loadingManager != null) {
            return loadingManager;
        }
        CommonAssetsManager clientBuiltin = CLIENT_BUILTIN;
        return clientBuiltin == null ? EMPTY : clientBuiltin;
    }

    private Map<Identifier, CustomIndexManager<?, ?>> indexManagers = ImmutableMap.of();
    private Map<Identifier, IndexTagManager> indexTagManagers = ImmutableMap.of();
    private Map<Identifier, IndexTagRegistry> indexTagRegistries = ImmutableMap.of();
    private GunAttributeManager attributeManager;

    @SuppressWarnings("unchecked")
    public <T extends CustomItemIndex<D>, D> CustomIndexManager<T, D> getManager(CustomItemType<T, D> id) {
        return (CustomIndexManager<T, D>) indexManagers.get(id.id());
    }

    @Nonnull
    public <T extends CustomItemIndex<D>, D> Optional<T> getIndex(Identifier id, CustomItemType<T, D> type) {
        var manager = getManager(type);
        if (manager == null) {
            return Optional.empty();
        }
        return manager.getIndex(id);
    }

    @UnmodifiableView
    public <T extends CustomItemIndex<D>, D> Map<Identifier, T> getAllIndexes(CustomItemType<T, D> type) {
        var manager = getManager(type);
        if (manager == null) {
            return Map.of();
        }
        return manager.getAll();
    }

    @Nonnull
    @Override
    public IndexTagLookup getIndexTagLookup(Identifier domainId) {
        IndexTagRegistry registry = indexTagRegistries.get(domainId);
        return registry == null ? IndexTagLookup.EMPTY : registry;
    }

    @Nullable
    public GunAttribute getAttribute(Identifier id) {
        if (attributeManager == null) {
            return null;
        }
        return attributeManager.getAttribute(id);
    }

    // FIXME REVIEW Forge AddReloadListenerEvent replaced with AddServerReloadListenersEvent, which needs an Identifier key per
    // listener: ares:gun_attributes, ares:index/<namespace>/<path>, ares:index_tag/attachment and ares:common_assets_publish.
    // FIXME REVIEW the ServerScriptManager listener is removed.
    @SubscribeEvent
    public static void onReload(AddServerReloadListenersEvent event) {
        CommonAssetsManager manager = createWithListeners(event.getServerResources().fullRegistries().lookup(), event::addListener);

        event.addListener(Ares.modLoc("common_assets_publish"), (sharedState, taskExecutor, barrier, reloadExecutor) ->
                barrier.wait(Void.TYPE).thenRunAsync(() -> publishReloadedAssets(manager), reloadExecutor));

        /// > 首次加载期间，索引解析需要访问本轮已应用的属性管理器。
        if (PUBLISHED == null) {
            INITIAL_LOADING_MANAGER = manager;
        }
    }

    private static CommonAssetsManager createWithListeners(HolderLookup.Provider registries,
                                                           BiConsumer<Identifier, PreparableReloadListener> addListener) {
        CommonAssetsManager manager = new CommonAssetsManager();

        var attributeManager = new GunAttributeManager();
        addListener.accept(Ares.modLoc("gun_attributes"), attributeManager);
        manager.attributeManager = attributeManager;

        ImmutableMap.Builder<Identifier, CustomIndexManager<?, ?>> builder = ImmutableMap.builder();
        var customIndexes = TimelessHelperS.getCustomItemTypes();
        for (var type : customIndexes) {
            var managerInstance = type.createDataManager();
            managerInstance.setRegistryAccess(registries);
            builder.put(type.id(), managerInstance);
            addListener.accept(Ares.modLoc("index/" + type.id().getNamespace() + "/" + type.id().getPath()), managerInstance);
        }
        manager.indexManagers = builder.build();

        var attachmentTagManager = new IndexTagManager(
                IndexTagDomains.ATTACHMENT,
                IndexTagDomains.ATTACHMENT_DIRECTORY
        );
        manager.indexTagManagers = ImmutableMap.of(attachmentTagManager.getDomainId(), attachmentTagManager);
        addListener.accept(Ares.modLoc("index_tag/" + IndexTagDomains.ATTACHMENT.getPath()), attachmentTagManager);
        return manager;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PUBLISHED = null;
        INITIAL_LOADING_MANAGER = null;
    }

    private static void publishReloadedAssets(CommonAssetsManager manager) {
        CommonAssetsManager previous = PUBLISHED;
        long startedAt = System.nanoTime();
        try {
            manager.resolveIndexTagRegistries();
            manager.validateAttributeReferences();
            PUBLISHED = manager;
            INITIAL_LOADING_MANAGER = null;

            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;
            LogUtils.getLogger().info("Published common assets buildMs={}", elapsedMillis);
        } catch (RuntimeException exception) {
            if (previous == null && INITIAL_LOADING_MANAGER == manager) {
                INITIAL_LOADING_MANAGER = null;
            }
            LogUtils.getLogger().error("Failed to build common assets snapshot; keeping the previous published assets", exception);
            throw exception;
        }
    }

    /// Replaces the common assets network sync of TACZ: the data only comes from the Ares jar,
    /// so a client connected to a remote server reads the same files from its own copy of the jar.
    public static void loadBuiltinForClient(HolderLookup.Provider registries) {
        List<PreparableReloadListener> listeners = new ArrayList<>();
        CommonAssetsManager manager = createWithListeners(registries, (key, listener) -> listeners.add(listener));
        CLIENT_BUILTIN = manager;

        Pack.ResourcesSupplier supplier = ResourcePackLoader.getPackFor(Ares.MOD_ID).orElseThrow();
        PackLocationInfo location = new PackLocationInfo("mod/" + Ares.MOD_ID, Component.literal(Ares.MOD_ID), PackSource.BUILT_IN, Optional.empty());
        // FIXME REVIEW the jar is opened with NeoForge ResourcePackLoader.getPackFor; Pack.Metadata is built by hand without
        // overlays and with COMPATIBLE.
        Pack.Metadata metadata = new Pack.Metadata(Component.empty(), PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of(), true);
        List<PackResources> packs = supplier.openResources(location, metadata).toList();
        PreparableReloadListener.PreparationBarrier barrier = new PreparableReloadListener.PreparationBarrier() {
            @Override
            public <T> CompletableFuture<T> wait(T value) {
                return CompletableFuture.completedFuture(value);
            }
        };
        try (MultiPackResourceManager resourceManager = new MultiPackResourceManager(PackType.SERVER_DATA, packs)) {
            PreparableReloadListener.SharedState state = new PreparableReloadListener.SharedState(resourceManager);
            for (PreparableReloadListener listener : listeners) {
                listener.reload(state, Runnable::run, barrier, Runnable::run).join();
            }
        }
        manager.resolveIndexTagRegistries();
        manager.validateAttributeReferences();
    }

    public static void clearBuiltinForClient() {
        CLIENT_BUILTIN = null;
    }

    private void resolveIndexTagRegistries() {
        if (indexTagManagers.isEmpty()) {
            indexTagRegistries = Map.of();
            return;
        }

        Map<Identifier, IndexTagRegistry> resolved = new LinkedHashMap<>();
        IndexTagManager attachmentTagManager = indexTagManagers.get(IndexTagDomains.ATTACHMENT);
        if (attachmentTagManager != null) {
            var guns = getManager(ModCustomItemTypes.GUN.get());
            var attachments = getManager(ModCustomItemTypes.ATTACHMENT.get());
            var underBarrels = getManager(ModCustomItemTypes.UNDER_BARREL.get());
            var slotAdapters = getManager(ModCustomItemTypes.SLOT_ADAPTER.get());

            Set<Identifier> attachmentIds = new LinkedHashSet<>();
            if (attachments != null) {
                attachmentIds.addAll(attachments.getAll().keySet());
            }
            if (underBarrels != null) {
                attachmentIds.addAll(underBarrels.getAll().keySet());
            }

            IndexTagRegistry attachmentRegistry = attachmentTagManager.resolve(attachmentIds::contains);
            resolved.put(IndexTagDomains.ATTACHMENT, attachmentRegistry);
            validateAttachmentData(attachmentRegistry);
        }
        indexTagRegistries = Map.copyOf(resolved);
    }

    private void validateAttachmentData(IndexTagRegistry attachmentRegistry) {
        var gunManager = getManager(ModCustomItemTypes.GUN.get());
        var attachmentManager = getManager(ModCustomItemTypes.ATTACHMENT.get());
        var underBarrelManager = getManager(ModCustomItemTypes.UNDER_BARREL.get());
        var slotAdapterManager = getManager(ModCustomItemTypes.SLOT_ADAPTER.get());
        var variantManager = getManager(ModCustomItemTypes.ATTACHMENT_VARIANT.get());

        List<String> errors = new ArrayList<>();
        collectIndexFailures("guns", gunManager, errors);
        collectIndexFailures("attachments", attachmentManager, errors);
        collectIndexFailures("under_barrels", underBarrelManager, errors);
        collectIndexFailures("slot_adapters", slotAdapterManager, errors);
        collectIndexFailures("attachment_variants", variantManager, errors);

        Map<Identifier, GunIndex> guns = gunManager == null ? Map.of() : gunManager.getAll();
        Map<Identifier, AttachmentIndex> attachments = attachmentManager == null
                ? Map.of()
                : attachmentManager.getAll();
        Map<Identifier, UnderBarrelGunIndex> underBarrels = underBarrelManager == null
                ? Map.of()
                : underBarrelManager.getAll();
        Map<Identifier, SlotAdapterIndex> slotAdapters = slotAdapterManager == null
                ? Map.of()
                : slotAdapterManager.getAll();
        Map<Identifier, VariantIndex> variants = variantManager == null
                ? Map.of()
                : variantManager.getAll();
        errors.addAll(AttachmentDataValidationHelper.validate(
                guns,
                attachments,
                underBarrels,
                slotAdapters,
                variants,
                attachmentRegistry,
                TimelessHelperS::getAttachmentType
        ));

        if (!errors.isEmpty()) {
            LogUtils.getLogger().error("Attachment common assets validation found {} error(s); continuing with valid loaded data:\n - {}",
                    errors.size(), String.join("\n - ", errors));
        }
    }

    /// > 属性引用校验：所有内容数据里出现的属性 id 必须在注册表中存在。
    /// >
    /// > 属性解析已改为不依赖注册表（修饰可创建属性），拼错的 id 不再于解析期报错，因此由本校验
    /// > 补齐 fail-fast，并把错误定位到引用它的内容文件。
    private void validateAttributeReferences() {
        List<String> errors = new ArrayList<>();

        var gunManager = getManager(ModCustomItemTypes.GUN.get());
        if (gunManager != null) {
            gunManager.getAll().forEach((id, index) -> collectGunAttributeReferences("guns", id, index.getData(), errors));
        }

        var underBarrelManager = getManager(ModCustomItemTypes.UNDER_BARREL.get());
        if (underBarrelManager != null) {
            underBarrelManager.getAll().forEach((id, index) ->
                    collectGunAttributeReferences("under_barrels", id, index.getData(), errors));
        }

        var attachmentManager = getManager(ModCustomItemTypes.ATTACHMENT.get());
        if (attachmentManager != null) {
            attachmentManager.getAll().forEach((id, index) ->
                    checkAttributeReferences("attachments", id, index.getData().getModifiers().keySet(), errors));
        }

        var slotAdapterManager = getManager(ModCustomItemTypes.SLOT_ADAPTER.get());
        if (slotAdapterManager != null) {
            slotAdapterManager.getAll().forEach((id, index) ->
                    checkAttributeReferences("slot_adapters", id, index.getData().getModifiers().keySet(), errors));
        }

        var variantManager = getManager(ModCustomItemTypes.ATTACHMENT_VARIANT.get());
        if (variantManager != null) {
            variantManager.getAll().forEach((id, index) -> {
                checkAttributeReferences("attachment_variants", id, index.getData().getModifiers().keySet(), errors);
                AttachmentData offData = index.getOffData();
                if (offData != null) {
                    checkAttributeReferences("attachment_variants", id, offData.getModifiers().keySet(), errors);
                }
            });
        }

        var ammoManager = getManager(ModCustomItemTypes.AMMO.get());
        if (ammoManager != null) {
            ammoManager.getAll().forEach((id, index) ->
                    checkAttributeReferences("ammo", id, index.getData().getModifiers().keySet(), errors));
        }

        if (!errors.isEmpty()) {
            LogUtils.getLogger().error("Attribute reference validation found {} error(s):\n - {}",
                    errors.size(), String.join("\n - ", errors));
        }
    }

    /// > 收集枪械数据（含下挂武器）里的属性引用：基础属性 + 弹种层 + 开火模式层。
    private void collectGunAttributeReferences(String domain, Identifier contentId, GunData data,
                                               List<String> errors) {
        checkAttributeReferences(domain, contentId, data.getBaseAttributes().keySet(), errors);
        for (AmmoEntry entry : data.getAmmos()) {
            checkAttributeReferences(domain, contentId, entry.getModifiers().keySet(), errors);
        }
        for (FireModeEntry mode : data.getFireModes()) {
            checkAttributeReferences(domain, contentId, mode.getModifiers().keySet(), errors);
        }
    }

    private void checkAttributeReferences(String domain, Identifier contentId,
                                          Collection<Identifier> attributes, List<String> errors) {
        for (Identifier attribute : attributes) {
            if (getAttribute(attribute) == null) {
                errors.add("Unknown attribute: domain=" + domain + ", id=" + contentId + ", attribute=" + attribute);
            }
        }
    }

    private static void collectIndexFailures(String domain, @Nullable CustomIndexManager<?, ?> manager,
                                             List<String> errors) {
        if (manager == null) {
            errors.add("Missing common assets index manager: " + domain);
            return;
        }
        manager.getIndexReadFailureIds().forEach(id ->
                errors.add("Index JSON read failed: domain=" + domain + ", id=" + id));
        manager.getDataReadFailureIds().forEach(id ->
                errors.add("Data JSON read failed: domain=" + domain + ", id=" + id));
        manager.getConstructionFailureIds().forEach(id ->
                errors.add("Index construction failed: domain=" + domain + ", id=" + id));
    }
}
