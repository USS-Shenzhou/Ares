package cn.ussshenzhou.ares.gun.init;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.custom.CustomItemType;
import cn.ussshenzhou.ares.gun.custom.ammo.AmmoData;
import cn.ussshenzhou.ares.gun.custom.ammo.AmmoIndex;
import cn.ussshenzhou.ares.gun.custom.charm.CharmData;
import cn.ussshenzhou.ares.gun.custom.charm.CharmIndex;
import cn.ussshenzhou.ares.gun.custom.attachment.AttachmentData;
import cn.ussshenzhou.ares.gun.custom.attachment.AttachmentIndex;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import cn.ussshenzhou.ares.gun.custom.gun.GunIndex;
import cn.ussshenzhou.ares.gun.custom.slotadapter.SlotAdapterData;
import cn.ussshenzhou.ares.gun.custom.slotadapter.SlotAdapterIndex;
import cn.ussshenzhou.ares.gun.custom.underbarrel.UnderBarrelGunData;
import cn.ussshenzhou.ares.gun.custom.underbarrel.UnderBarrelGunIndex;
import cn.ussshenzhou.ares.gun.custom.variant.VariantData;
import cn.ussshenzhou.ares.gun.custom.variant.VariantIndex;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/// > 自定义物品类型注册，用于数据包驱动的物品类型<br/>
/// > 集成自动数据包配置加载和同步功能
/// @author TACZ, USS_Shenzhou
public class ModCustomItemTypes {
    public static final DeferredRegister<CustomItemType<?, ?>> CUSTOM_ITEM_TYPE = DeferredRegister.create(ModRegistries.CUSTOM_ITEM_TYPE, Ares.MOD_ID);

    /// > 普通的枪，可以安装配件
    public static DeferredHolder<CustomItemType<?, ?>, CustomItemType<GunIndex, GunData>> GUN = CUSTOM_ITEM_TYPE.register(
            "guns", () -> GunIndex.TYPE
    );

    /// > 普通的配件（可选携带子配件槽位，从而作为容器挂载子配件）
    public static DeferredHolder<CustomItemType<?, ?>, CustomItemType<AttachmentIndex, AttachmentData>> ATTACHMENT = CUSTOM_ITEM_TYPE.register(
            "attachments", () -> AttachmentIndex.TYPE
    );

    /// > 下挂武器，可以作为单独的枪使用，也可以作为下挂配件安装到枪上；<br/>
    /// > 为了简化结构，我们不允许下挂武器上再安装配件
    public static DeferredHolder<CustomItemType<?, ?>, CustomItemType<UnderBarrelGunIndex, UnderBarrelGunData>> UNDER_BARREL = CUSTOM_ITEM_TYPE.register(
            "under_barrels", () -> UnderBarrelGunIndex.TYPE
    );

    public static DeferredHolder<CustomItemType<?, ?>, CustomItemType<AmmoIndex, AmmoData>> AMMO = CUSTOM_ITEM_TYPE.register(
            "ammo", () -> AmmoIndex.TYPE
    );

    /// > 配件变体：依附于配件、不消耗独立物品的几何中间层（如带增高座的瞄具）。
    /// > 通过 targets 定向到具体配件 id，支持跨包扩展。
    public static DeferredHolder<CustomItemType<?, ?>, CustomItemType<VariantIndex, VariantData>> ATTACHMENT_VARIANT = CUSTOM_ITEM_TYPE.register(
            "attachment_variants", () -> VariantIndex.TYPE
    );

    /// > 槽位适配器：依附于宿主槽位、不消耗独立物品的中间层（如垫高的导轨）。
    /// > 通过 targets（宿主 id + 槽位 id）定向，可改写槽位声明，支持跨包扩展。
    public static DeferredHolder<CustomItemType<?, ?>, CustomItemType<SlotAdapterIndex, SlotAdapterData>> SLOT_ADAPTER = CUSTOM_ITEM_TYPE.register(
            "slot_adapters", () -> SlotAdapterIndex.TYPE
    );

    /// > 挂饰：挂在枪体某骨骼某 cube 表面的物理装饰物（独立物品，不并入配件体系）。
    public static DeferredHolder<CustomItemType<?, ?>, CustomItemType<CharmIndex, CharmData>> CHARM = CUSTOM_ITEM_TYPE.register(
            "charms", () -> CharmIndex.TYPE
    );
}
