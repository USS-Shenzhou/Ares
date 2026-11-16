package cn.ussshenzhou.ares.gun.custom.attribute;

import com.mojang.logging.LogUtils;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import cn.ussshenzhou.ares.gun.attribute.GunAttribute;
import cn.ussshenzhou.ares.gun.attribute.GunAttributeModifier;
import cn.ussshenzhou.ares.gun.attribute.GunAttributes;
import cn.ussshenzhou.ares.gun.attribute.IGunProperties;
import cn.ussshenzhou.ares.gun.attribute.InaccuracyType;
import cn.ussshenzhou.ares.gun.item.*;
import cn.ussshenzhou.ares.gun.custom.CommonAssetsManager;
import cn.ussshenzhou.ares.gun.custom.ammo.AmmoData;
import cn.ussshenzhou.ares.gun.custom.ammo.AmmoIndex;
import cn.ussshenzhou.ares.gun.custom.gun.AmmoEntry;
import cn.ussshenzhou.ares.gun.custom.gun.FireModeEntry;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import cn.ussshenzhou.ares.gun.custom.gun.GunIndex;
import cn.ussshenzhou.ares.gun.custom.underbarrel.UnderBarrelGunIndex;
import cn.ussshenzhou.ares.gun.util.AmmoDataHelper;
import cn.ussshenzhou.ares.gun.init.ModDataComponents;
import cn.ussshenzhou.ares.gun.util.MountedWeaponHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.compress.utils.Lists;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// > 枪械属性缓存：由「枪械数据 + 配件树 + 弹种 + 开火模式」派生的属性值。
/// >
/// > **缓存契约**：
/// > - 修饰来源是封闭集合：枪械基础属性、配件树（含开关与变体形态）、下挂武器、弹种层、开火模式层。
/// >   新增来源必须同时登记到 [#from(ItemStack)] 的收集链与失效责任里。
/// > - 缓存按 [ItemStack] 实例身份区分（`weakKeys` 即 identity 比较）：实例换了就重算，
/// >   实例没换则沿用旧值。
/// > - 因此**写入方负责失效**：任何就地改写会影响上述来源的 NBT 之后，必须调用
/// >   [#invalidateCache(ItemStack)]。现有写入方：`AmmoDataHelper.setAmmoId/clearAmmoId`、
/// >   `FireModeHelper.setFireModeIndex`、`AttachmentTreeHelper.writeBack`。
///
/// @author TACZ, USS_Shenzhou
public class GunPropertiesCaches implements IGunProperties {
    public static final GunPropertiesCaches EMPTY = new GunPropertiesCaches(ImmutableMap.of(), ImmutableMap.of(), 0, null);

    private static final LoadingCache<ItemStack, GunPropertiesCaches> dataCache = CacheBuilder.newBuilder()
            .weakKeys()
            .removalListener(notification -> {
/// >                LogUtils.getLogger().debug("Removed cache for stack: {}", notification.getCause());
            })
            .build(new CacheLoader<>() {
                @Nonnull
                @Override
                public GunPropertiesCaches load(@Nonnull ItemStack stack) {
                    return GunPropertiesCaches.from(stack);
                }
            });

    @UnmodifiableView
    private final Map<Identifier, ItemStack> attachments;
    private final Map<Identifier, CachedValue> cachedAttribute;
    private final int magLevel;
    /// > 解析出的已安装下挂武器副本（无则为 null）。随宿主缓存生命周期存活：宿主 [ItemStack] 实例
    /// > 一变（服务端同步重建 / 换枪）即随缓存一起失效重解析，故永远反映宿主最新 NBT。
    /// >
    /// > 该副本由宿主 NBT 反序列化得到，与宿主 NBT 解耦。客户端预判可原地改写其弹量（纯表现，
    /// > 服务端同步重建缓存自愈）；调用方不应将其当作权威写入通道（权威落地走服务端 working copy）。
    @Nullable
    private final ItemStack mountedWeapon;

    private GunPropertiesCaches(
            ImmutableMap<Identifier, ItemStack> attachments,
            ImmutableMap<Identifier, CachedValue> cachedAttribute,
            int magLevel,
            @Nullable ItemStack mountedWeapon
    ) {
        this.cachedAttribute = cachedAttribute;
        this.magLevel = magLevel;
        this.attachments = attachments;
        this.mountedWeapon = mountedWeapon;
    }

    /// > 属性最终值。未注册的属性返回 0（调用方传的都是内置常量；脚本 / 状态机读值请用
    /// > [#getAttributeOrNull(Identifier)]，它能区分「未注册」与「值为 0」）。
    @Override
    public double getAttribute(Identifier attr) {
        Double value = getAttributeOrNull(attr);
        return value != null ? value : 0;
    }

    /// > 属性最终值：未注册的属性返回 null；已注册但本枪无来源时回落注册基准值（`default`）。
    @Nullable
    @Override
    public Double getAttributeOrNull(Identifier attr) {
        CachedValue instance = cachedAttribute.get(attr);
        if (instance != null) {
            return instance.modified();
        }
        GunAttribute definition = CommonAssetsManager.get().getAttribute(attr);
        return definition != null ? definition.defaultValue() : null;
    }

    /// > 该属性在本枪上是否有显式来源（基础属性声明，或任一修饰来源）。
    /// > 仅回落注册基准值不算「有」；未注册的属性恒为 false。
    @Override
    public boolean hasAttribute(Identifier attr) {
        return cachedAttribute.containsKey(attr);
    }

    @Override
    public Map<Identifier, CachedValue> getCachedAttribute() {
        return cachedAttribute;
    }

    /// > Cached attachment stacks from the gun's slots.
    /// > These stacks are snapshots and should be treated as read-only.
    @UnmodifiableView
    @Override
    public Map<Identifier, ItemStack> getAttachmentCaches() {
        return attachments;
    }

    /// > 缓存的已安装下挂武器副本（无则 null）。随宿主缓存生命周期存活，宿主实例变化时随缓存失效重解析。
    /// >
    /// > 客户端预判可原地改写其弹量（纯表现）；非权威写入通道。
    @Nullable
    @Override
    public ItemStack getMountedWeapon() {
        return mountedWeapon;
    }

    public static void invalidateCache(ItemStack stack) {
        dataCache.invalidate(stack);
        LogUtils.getLogger().debug("Invalidate cache for stack: {}", stack.hashCode());
    }

    public static void invalidateAll() {
        dataCache.invalidateAll();
    }

    public static GunPropertiesCaches of(ItemStack stack) {
        return dataCache.getUnchecked(stack);
    }

    @Nonnull
    public static GunPropertiesCaches from(ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof IGun iGun) {
            GunData data = iGun.getIndex(stack).map(GunIndex::getData).orElse(null);
            if (data == null) {
                return EMPTY;
            }

            Map<Identifier, AttachmentSlot> slots = iGun.getAttachmentSlots(stack);
            ImmutableMap.Builder<Identifier, ItemStack> builder = ImmutableMap.builder();
            for (var entry : slots.entrySet()) {
                ItemStack attachmentStack = iGun.getStoredAttachment(stack, entry.getValue());
                builder.put(entry.getKey(), attachmentStack);
            }
            var attachmentsMap = builder.build();

            /// > Collect modifier sources:
            /// > - direct attachments
            /// > - kit attachment itself
            /// > - attachments inside kit (one level only)
            List<ItemStack> modifierSources = collectModifierSources(attachmentsMap);

            Map<Identifier, List<GunAttributeModifier>> modifiers = new HashMap<>();
            int magLevel = 0;

            for (ItemStack src : modifierSources) {
                IAttachmentLike attachmentLike = IAttachmentLike.asAttachmentLike(src);
                if (attachmentLike == null) {
                    continue;
                }

                /// > Merge attribute modifiers
                var modifiersMap = attachmentLike.getModifiers(src);
                for (Map.Entry<Identifier, List<GunAttributeModifier>> entry : modifiersMap.entrySet()) {
                    modifiers.computeIfAbsent(entry.getKey(), k -> Lists.newArrayList()).addAll(entry.getValue());
                }

                /// > Only normal attachments and kit attachments can extend magazine.
                magLevel = Math.max(magLevel, readExtendedMagLevel(src));
            }

            /// > 弹种层修饰
            collectAmmoModifiers(stack, data, modifiers);

            /// > 开火模式层修饰
            collectFireModeModifiers(stack, data, modifiers);

            var attrMap = calcAttributes(modifiers, data);
            magLevel = Math.min(magLevel, 3);

            /// > 解析已安装下挂武器（递归整棵配件树），随宿主缓存生命周期存活。
            ItemStack mounted = MountedWeaponHelper.findInstalledMountedWeapon(stack).orElse(null);

            GunPropertiesCaches gunCache = new GunPropertiesCaches(attachmentsMap, attrMap, magLevel, mounted);
            dataCache.put(stack, gunCache);
            return gunCache;
        }

        /// > 下挂武器（IMountedWeapon）：叶子节点，无子配件树。
        /// > 自身属性仅来自 UnderBarrelGunData 的 base 属性 + 弹种层 / 开火模式层修饰。
        /// > 注意：IMountedWeapon.getModifiers() 是「作为配件装到宿主时给宿主加的修饰」，
        /// > 不作用于下挂武器自身，故此处不收集。
        if (!stack.isEmpty() && stack.getItem() instanceof IMountedWeapon mountedWeapon) {
            GunData data = mountedWeapon.getIndex(stack).map(UnderBarrelGunIndex::getData).orElse(null);
            if (data == null) {
                return EMPTY;
            }

            Map<Identifier, List<GunAttributeModifier>> modifiers = new HashMap<>();
            collectAmmoModifiers(stack, data, modifiers);
            collectFireModeModifiers(stack, data, modifiers);

            var attrMap = calcAttributes(modifiers, data);
            GunPropertiesCaches mountedCache = new GunPropertiesCaches(ImmutableMap.of(), attrMap, 0, null);
            dataCache.put(stack, mountedCache);
            return mountedCache;
        }
        return EMPTY;
    }

    private static int readExtendedMagLevel(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.getItem() instanceof IAttachment iAttachment) {
            return iAttachment.getExtendedMagLevel(stack);
        }
        return 0;
    }

    /// > 收集所有提供属性修饰的配件物品堆。
    /// >
    /// > 从枪械直接槽位出发，对任意 [IAttachmentHolder] 递归收集其子配件，
    /// > 深度以 [IAttachmentHolder#MAX_ATTACHMENT_DEPTH] 兜底防止异常数据导致无限递归。
    private static List<ItemStack> collectModifierSources(Map<Identifier, ItemStack> directSlots) {
        List<ItemStack> result = Lists.newArrayList();
        for (ItemStack stack : directSlots.values()) {
            collectRecursive(stack, result, IAttachmentHolder.MAX_ATTACHMENT_DEPTH);
        }
        return result;
    }

    private static void collectRecursive(ItemStack stack, List<ItemStack> out, int remaining) {
        if (stack == null || stack.isEmpty() || remaining <= 0) {
            return;
        }
        out.add(stack);

        /// > 任意带槽位的配件都可作为容器，递归收集其子配件。
        if (stack.getItem() instanceof IAttachmentHolder holder) {
            Map<AttachmentSlot, ItemStack> sub = holder.getAttachmentAndSlots(stack);
            for (ItemStack child : sub.values()) {
                collectRecursive(child, out, remaining - 1);
            }
        }
    }

    private static void collectAmmoModifiers(ItemStack gun, GunData gunData, Map<Identifier, List<GunAttributeModifier>> modifiers) {
        Identifier ammoId = AmmoDataHelper.getAmmoId(gun);
        if (ammoId == null) {
            return;
        }

        /// > 弹种自身的 modifiers
        AmmoData ammoData = CommonAssetsManager.get().getIndex(ammoId, AmmoIndex.TYPE)
                .map(AmmoIndex::getData).orElse(null);
        if (ammoData != null) {
            for (var entry : ammoData.getModifiers().entrySet()) {
                modifiers.computeIfAbsent(entry.getKey(), k -> Lists.newArrayList()).add(entry.getValue());
            }
        }

        /// > 枪械侧对该弹种的覆盖 modifiers（优先级更高，后加入列表）
        AmmoEntry ammoEntry = gunData.getAmmoEntry(ammoId);
        if (ammoEntry != null) {
            for (var entry : ammoEntry.getModifiers().entrySet()) {
                modifiers.computeIfAbsent(entry.getKey(), k -> Lists.newArrayList()).add(entry.getValue());
            }
        }
    }

    private static void collectFireModeModifiers(ItemStack gun, GunData data, Map<Identifier, List<GunAttributeModifier>> modifiers) {
        List<FireModeEntry> fireModes = data.getFireModes();
        if (fireModes.isEmpty()) {
            return;
        }
        // FIXME REVIEW the fire mode layer reads the FIRE_MODE_INDEX component instead of the FireModeIndex NBT key.
        int modeIndex = gun.getOrDefault(ModDataComponents.FIRE_MODE_INDEX, 0);
        if (modeIndex < 0 || modeIndex >= fireModes.size()) {
            return;
        }
        FireModeEntry entry = fireModes.get(modeIndex);
        for (var mod : entry.getModifiers().entrySet()) {
            modifiers.computeIfAbsent(mod.getKey(), k -> Lists.newArrayList()).add(mod.getValue());
        }
    }

    @Nonnull
    private static ImmutableMap<Identifier, CachedValue> calcAttributes(
            Map<Identifier, List<GunAttributeModifier>> modifiers,
            GunData data
    ) {
        Map<Identifier, CachedValue> result = new HashMap<>();

        /// > 1. 枪械数据里声明的属性：base 保留原始声明值，最终值按注册定义硬 clamp。
        for (var entry : data.getBaseAttributes().entrySet()) {
            Identifier id = entry.getKey();
            double base = entry.getValue();
            GunAttribute definition = CommonAssetsManager.get().getAttribute(id);
            double value = definition != null ? definition.clamp(base) : base;
            result.put(id, new CachedValue(base, value));
        }

        /// > 2. 修饰链结果覆盖；修饰可创建本枪未声明的属性，基准值取注册默认值。
        for (var entry : modifiers.entrySet()) {
            Identifier id = entry.getKey();
            GunAttribute definition = CommonAssetsManager.get().getAttribute(id);
            if (definition == null) {
                /// > 未注册的属性：加载期校验已报错，运行期忽略（不进入缓存）。
                continue;
            }
            double baseValue = data.hasAttribute(id) ? data.getBaseAttribute(id) : definition.defaultValue();
            double modified = definition.clamp(calculateAttribute(baseValue, entry.getValue()));
            result.put(id, new CachedValue(baseValue, modified));
        }
        return ImmutableMap.copyOf(result);
    }

    public static double calculateAttribute(double base, @Nonnull List<GunAttributeModifier> modifiers) {
        double percent = 1.0;
        double multiplyBase = 1.0;
        for (var modifier : modifiers) {
            switch (modifier.type()) {
                case ADDEND -> base += modifier.amount();
                case PERCENT -> percent += modifier.amount();
                case MULTIPLIER -> multiplyBase *= modifier.amount();
            }
        }
        percent = Math.max(0, percent);
        multiplyBase = Math.max(0, multiplyBase);

        return base * percent * multiplyBase;
    }

    @Override
    public double getInaccuracy(InaccuracyType inaccuracyType) {
        return switch (inaccuracyType) {
            case STAND -> getAttribute(GunAttributes.STAND_INACCURACY);
            case AIM -> getAttribute(GunAttributes.AIM_INACCURACY);
            case TACTICAL -> getAttribute(GunAttributes.TACTICAL_INACCURACY);
            case MOVE -> getAttribute(GunAttributes.MOVE_INACCURACY);
            case SNEAK -> getAttribute(GunAttributes.SNEAK_INACCURACY);
            case LIE -> getAttribute(GunAttributes.LIE_INACCURACY);
            default -> 0;
        };
    }

    @Override
    public int getMagLevel() {
        return magLevel;
    }
}
