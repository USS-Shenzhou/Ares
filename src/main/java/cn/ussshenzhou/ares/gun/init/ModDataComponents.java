package cn.ussshenzhou.ares.gun.init;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.capability.GunModuleMode;
import cn.ussshenzhou.ares.gun.custom.charm.GunCharmData;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// @author USS_Shenzhou
// FIXME REVIEW one data component per TACZ NBT key (CustomItemId, AmmoCount, AmmoId, Chambered, FireModeIndex, ModuleMode,
// ZoomIndex, ZoomProgress, RailOffset, Variant, ToggleEnabled, LaserEnabled, FlashlightEnabled, SlotAdapters, the per-slot
// attachment root keys and TaczCharms.charms), each with a persistent codec and a network codec.
// FIXME REVIEW CustomItemDataHelper, AmmoDataHelper, AttachmentDataHelper, AttachmentOffsetHelper, FireModeHelper,
// ModuleModeHelper, SlotAdapterHelper, ToggleHelper, VariantHelper, ZoomHelper and GunCharmDataHelper now read and write these
// components; their signatures are unchanged except CustomItemDataHelper.getCustomId/setCustomId.
public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Ares.MOD_ID);

    private static final Codec<GunModuleMode> MODULE_MODE_CODEC = Codec.INT.xmap(ModDataComponents::moduleModeByOrdinal, GunModuleMode::ordinal);
    private static final StreamCodec<ByteBuf, GunModuleMode> MODULE_MODE_STREAM_CODEC = ByteBufCodecs.VAR_INT.map(ModDataComponents::moduleModeByOrdinal, GunModuleMode::ordinal);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> CUSTOM_ITEM_ID = DATA_COMPONENTS.registerComponentType(
            "custom_item_id", builder -> builder.persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AMMO_COUNT = DATA_COMPONENTS.registerComponentType(
            "ammo_count", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> AMMO_ID = DATA_COMPONENTS.registerComponentType(
            "ammo_id", builder -> builder.persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CHAMBERED = DATA_COMPONENTS.registerComponentType(
            "chambered", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FIRE_MODE_INDEX = DATA_COMPONENTS.registerComponentType(
            "fire_mode_index", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT)
    );

    // FIXME REVIEW encoded by ordinal, the same value as the original NBT byte.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GunModuleMode>> MODULE_MODE = DATA_COMPONENTS.registerComponentType(
            "module_mode", builder -> builder.persistent(MODULE_MODE_CODEC).networkSynchronized(MODULE_MODE_STREAM_CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ZOOM_INDEX = DATA_COMPONENTS.registerComponentType(
            "zoom_index", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> ZOOM_PROGRESS = DATA_COMPONENTS.registerComponentType(
            "zoom_progress", builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> RAIL_OFFSET = DATA_COMPONENTS.registerComponentType(
            "rail_offset", builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> VARIANT = DATA_COMPONENTS.registerComponentType(
            "variant", builder -> builder.persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> TOGGLE_ENABLED = DATA_COMPONENTS.registerComponentType(
            "toggle_enabled", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LASER_ENABLED = DATA_COMPONENTS.registerComponentType(
            "laser_enabled", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> FLASHLIGHT_ENABLED = DATA_COMPONENTS.registerComponentType(
            "flashlight_enabled", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Map<Identifier, Identifier>>> SLOT_ADAPTERS = DATA_COMPONENTS.registerComponentType(
            "slot_adapters", builder -> builder
                    .persistent(Codec.unboundedMap(Identifier.CODEC, Identifier.CODEC))
                    .networkSynchronized(ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, Identifier.STREAM_CODEC))
    );

    /// TACZ stored each attachment under its own root key `AttachmentSlot.storeTag()`; here they share one map keyed by the same string.
    // FIXME REVIEW nested attachment and charm stacks are stored as ItemStackTemplate, because component values must be immutable
    // and compared by value, which ItemStack is not.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Map<String, ItemStackTemplate>>> ATTACHMENTS = DATA_COMPONENTS.registerComponentType(
            "attachments", builder -> builder
                    .persistent(Codec.unboundedMap(Codec.STRING, ItemStackTemplate.CODEC))
                    .networkSynchronized(ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ItemStackTemplate.STREAM_CODEC))
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<GunCharmData>>> CHARMS = DATA_COMPONENTS.registerComponentType(
            "charms", builder -> builder
                    .persistent(GunCharmData.CODEC.listOf())
                    .networkSynchronized(GunCharmData.STREAM_CODEC.apply(ByteBufCodecs.list()))
    );

    private static GunModuleMode moduleModeByOrdinal(int ordinal) {
        GunModuleMode[] values = GunModuleMode.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return GunModuleMode.PRIMARY;
        }
        return values[ordinal];
    }
}
