package cn.ussshenzhou.ares.client.resource;

import com.mojang.logging.LogUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import cn.ussshenzhou.ares.gun.resource.GunGsonHelper;
import cn.ussshenzhou.ares.client.resource.display.GunDisplayInstance;
import cn.ussshenzhou.ares.client.resource.display.UnderBarrelOverrideRule;
import cn.ussshenzhou.ares.gun.util.MountedWeaponHelper;
import cn.ussshenzhou.ares.gun.util.ResourceScanHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Map;
import java.util.Optional;

/// > 下挂安装后覆盖目标枪械 display 的动画/状态机（客户端资源）。
/// @author TACZ, USS_Shenzhou
public class UnderBarrelOverrideManager extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    public static final Marker MARKER = MarkerFactory.getMarker("UnderBarrelOverrideManager");

    private Map<OverrideKey, Rule> ruleMap = Map.of();
    private Map<OverrideKey, GunDisplayInstance> gunVariants = Map.of();
    private Map<OverrideKey, GunDisplayInstance> mountedVariants = Map.of();

    @Override
    @Nonnull
    protected Map<Identifier, JsonElement> prepare(@Nonnull ResourceManager manager, @Nonnull ProfilerFiller pProfiler) {
        return ResourceScanHelper.scanDirectory(manager, "display/under_barrels", GunGsonHelper.GSON);
    }

    @Override
    protected void apply(@Nonnull Map<Identifier, JsonElement> resources, @Nonnull ResourceManager manager, @Nonnull ProfilerFiller pProfiler) {
        Map<OverrideKey, Rule> temp = Maps.newHashMap();
        for (var entry : resources.entrySet()) {
            try {
                var obj = GsonHelper.convertToJsonObject(entry.getValue(), "under barrel override");
                UnderBarrelOverrideRule pojo = GunGsonHelper.GSON.fromJson(obj, UnderBarrelOverrideRule.class);
                if (pojo == null || pojo.underBarrelId == null || pojo.targetGunDisplayId == null || pojo.overrides == null) {
                    continue;
                }
                temp.put(new OverrideKey(pojo.targetGunDisplayId, pojo.underBarrelId), new Rule(pojo.mountedWeaponDisplayId, pojo.overrides));
            } catch (Exception e) {
                LogUtils.getLogger().error(MARKER, "Failed to load under barrel override: {}", entry.getKey(), e);
            }
        }
        ruleMap = ImmutableMap.copyOf(temp);

        Map<OverrideKey, GunDisplayInstance> tempGunVariants = Maps.newHashMap();
        Map<OverrideKey, GunDisplayInstance> tempMountedVariants = Maps.newHashMap();

        for (var entry : ruleMap.entrySet()) {
            OverrideKey key = entry.getKey();
            Rule rule = entry.getValue();
            UnderBarrelOverrideRule.Overrides overrides = rule.overrides;

            try {
                Optional<GunDisplayInstance> baseOpt = ClientAssetsManager.INSTANCE.getGunDisplay(key.targetDisplayId);
                if (baseOpt.isPresent()) {
                    GunDisplayInstance derived = GunDisplayInstance.deriveWithExtraAnimations(baseOpt.get(),
                            overrides.animationLocation,
                            overrides.defaultAnimationLocation);
                    tempGunVariants.put(key, derived);
                }
            } catch (Exception e) {
                LogUtils.getLogger().error(MARKER, "Failed to build under barrel variant display: target={}, underbarrel={}", key.targetDisplayId, key.mountedWeaponId, e);
            }

            if (rule.mountedWeaponDisplayId == null) {
                continue;
            }

            try {
                Optional<GunDisplayInstance> mountedBaseOpt = ClientAssetsManager.INSTANCE.getGunDisplay(rule.mountedWeaponDisplayId);
                if (mountedBaseOpt.isPresent()) {
                    GunDisplayInstance derivedMounted = GunDisplayInstance.deriveWithExtraAnimations(mountedBaseOpt.get(),
                            overrides.animationLocation,
                            overrides.defaultAnimationLocation);
                    tempMountedVariants.put(key, derivedMounted);
                }
            } catch (Exception e) {
                LogUtils.getLogger().error(MARKER, "Failed to build under barrel mounted display variant: mountedDisplay={}, underbarrel={}", rule.mountedWeaponDisplayId, key.mountedWeaponId, e);
            }
        }

        gunVariants = ImmutableMap.copyOf(tempGunVariants);
        mountedVariants = ImmutableMap.copyOf(tempMountedVariants);
    }

    public Optional<GunDisplayInstance> applyIfMatched(ItemStack stack, Identifier baseDisplayId, GunDisplayInstance baseDisplay) {
        Optional<Identifier> mountedIdOpt = MountedWeaponHelper.findInstalledMountedWeaponId(stack);
        if (mountedIdOpt.isEmpty()) {
            return Optional.of(baseDisplay);
        }
        Identifier mountedId = mountedIdOpt.get();
        OverrideKey key = new OverrideKey(baseDisplayId, mountedId);
        GunDisplayInstance variant = gunVariants.get(key);

        return Optional.ofNullable(variant != null ? variant : baseDisplay);
    }

    /// > 渲染枪本体时：如果命中 override 规则，返回已预构建的“下挂 display 变体”（用于下挂模型本体动画）。
    public Optional<GunDisplayInstance> applyMountedIfMatched(ItemStack gunStack,
                                                             Identifier gunDisplayId,
                                                             Identifier fallbackMountedDisplayId) {
        Optional<Identifier> mountedIdOpt = MountedWeaponHelper.findInstalledMountedWeaponId(gunStack);
        if (mountedIdOpt.isEmpty()) {
            return ClientAssetsManager.INSTANCE.getGunDisplay(fallbackMountedDisplayId);
        }

        Identifier mountedId = mountedIdOpt.get();
        OverrideKey key = new OverrideKey(gunDisplayId, mountedId);
        Rule rule = ruleMap.get(key);

        Identifier mountedDisplayId = fallbackMountedDisplayId;
        if (rule != null && rule.mountedWeaponDisplayId != null) {
            mountedDisplayId = rule.mountedWeaponDisplayId;
        }

        Optional<GunDisplayInstance> baseOpt = ClientAssetsManager.INSTANCE.getGunDisplay(mountedDisplayId);
        if (baseOpt.isEmpty()) {
            return Optional.empty();
        }

        GunDisplayInstance variant = mountedVariants.get(key);
        return Optional.of(variant != null ? variant : baseOpt.get());
    }

    private record OverrideKey(@Nonnull Identifier targetDisplayId, @Nonnull Identifier mountedWeaponId) {
    }

    private static class Rule {
        @Nullable
        private final Identifier mountedWeaponDisplayId;
        @Nonnull
        private final UnderBarrelOverrideRule.Overrides overrides;

        private Rule(@Nullable Identifier mountedWeaponDisplayId, @Nonnull UnderBarrelOverrideRule.Overrides overrides) {
            this.mountedWeaponDisplayId = mountedWeaponDisplayId;
            this.overrides = overrides;
        }
    }
}
