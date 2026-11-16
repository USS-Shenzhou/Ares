package cn.ussshenzhou.ares.gun;

import cn.ussshenzhou.ares.gun.item.IAttachment;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.gun.item.IMountedWeapon;
import cn.ussshenzhou.ares.gun.client.resource.ClientAssetsManager;
import cn.ussshenzhou.ares.gun.client.resource.display.AmmoDisplayInstance;
import cn.ussshenzhou.ares.gun.client.resource.display.AttachmentDisplayInstance;
import cn.ussshenzhou.ares.gun.client.resource.display.CharmDisplayInstance;
import cn.ussshenzhou.ares.gun.client.resource.display.GunDisplayInstance;
import cn.ussshenzhou.ares.gun.client.resource.display.SlotAdapterDisplayInstance;
import cn.ussshenzhou.ares.gun.custom.attachment.ToggleConfig;
import cn.ussshenzhou.ares.gun.init.ModCustomItemTypes;
import cn.ussshenzhou.ares.gun.util.ToggleHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;

import java.util.Optional;

/// @author TACZ, USS_Shenzhou
public class TimelessHelperC {
    @Nonnull
    public static Optional<GunDisplayInstance> getGunDisplay(ItemStack stack) {
        var iGun = IGun.asGun(stack);
        if (iGun != null) {
            return ClientAssetsManager.INSTANCE.getGunDisplayWithUnderBarrelOverride(stack, iGun.getDisplayId(stack));
        }

        if (stack.getItem() instanceof IMountedWeapon mountedWeapon) {
            return ClientAssetsManager.INSTANCE.getGunDisplay(mountedWeapon.getDisplayId(stack));
        }

        return Optional.empty();
    }

    @Nonnull
    public static Optional<GunDisplayInstance> getMountedWeaponDisplayForGun(ItemStack gunStack, ItemStack mountedWeaponStack) {
        IGun gun = IGun.asGun(gunStack);
        if (gun == null) {
            return Optional.empty();
        }
        if (!(mountedWeaponStack.getItem() instanceof IMountedWeapon mountedWeapon)) {
            return Optional.empty();
        }
        return ClientAssetsManager.INSTANCE.getMountedDisplayWithUnderBarrelOverride(
                gunStack,
                gun.getDisplayId(gunStack),
                mountedWeapon.getDisplayId(mountedWeaponStack)
        );
    }

    @Nonnull
    public static Optional<AttachmentDisplayInstance> getAttachmentDisplay(ItemStack stack) {
        var iAttachment = IAttachment.asAttachment(stack);
        if (iAttachment == null) {
            return Optional.empty();
        }
        return ClientAssetsManager.INSTANCE.getAttachmentDisplay(iAttachment.getDisplayId(stack));
    }

    /// > 取某配件当前所选变体的 display；未选变体或变体不存在返回 empty。
    @Nonnull
    public static Optional<AttachmentDisplayInstance> getVariantDisplay(ItemStack attachment) {
        Identifier variantId = cn.ussshenzhou.ares.gun.util.VariantHelper.getVariant(attachment);
        if (variantId == null) {
            return Optional.empty();
        }
        var index = TimelessHelperS.getIndex(variantId, ModCustomItemTypes.ATTACHMENT_VARIANT.get()).orElse(null);
        if (index == null) {
            return Optional.empty();
        }
        return ClientAssetsManager.INSTANCE.getDisplay(index.getDisplayLocation());
    }

    /// > 取某配件「当前生效形态」的几何 display（统一入口）。
    /// >
    /// > 解析顺序：关态几何（当前生效形态 toggle 声明了 off_display 且开关关闭）→
    /// > 用户选择的变体 display → 空（调用方回退基础 display）。空态基础几何由
    /// > [#getAttachmentDisplay(ItemStack)] 提供。
    @Nonnull
    public static Optional<AttachmentDisplayInstance> getActiveDisplay(ItemStack attachment) {
        if (IAttachment.asAttachment(attachment) == null) {
            return Optional.empty();
        }
        /// > 关态几何：当前生效形态声明了 off_display
        ToggleConfig config = ToggleHelper.resolveActiveToggle(attachment);
        if (config != null && !ToggleHelper.isEnabled(attachment) && config.getOffDisplay() != null) {
            return ClientAssetsManager.INSTANCE.getDisplay(config.getOffDisplay());
        }
        /// > 用户选择的变体
        Identifier variantId = cn.ussshenzhou.ares.gun.util.VariantHelper.getVariant(attachment);
        if (variantId != null) {
            var index = TimelessHelperS.getIndex(variantId, ModCustomItemTypes.ATTACHMENT_VARIANT.get()).orElse(null);
            if (index != null) {
                return ClientAssetsManager.INSTANCE.getDisplay(index.getDisplayLocation());
            }
        }
        return Optional.empty();
    }

    /// > 取某适配器 id 对应的 display。
    @Nonnull
    public static Optional<SlotAdapterDisplayInstance> getSlotAdapterDisplay(Identifier adapterId) {
        if (adapterId == null) {
            return Optional.empty();
        }
        var index = TimelessHelperS.getIndex(adapterId, ModCustomItemTypes.SLOT_ADAPTER.get()).orElse(null);
        if (index == null) {
            return Optional.empty();
        }
        return ClientAssetsManager.INSTANCE.getSlotAdapterDisplay(index.getDisplayLocation());
    }

    /// > 取某弹药 id 对应的 display（含弹壳模型）。
    @Nonnull
    public static Optional<AmmoDisplayInstance> getAmmoDisplay(Identifier ammoId) {
        if (ammoId == null) {
            return Optional.empty();
        }
        var index = TimelessHelperS.getIndex(ammoId, ModCustomItemTypes.AMMO.get()).orElse(null);
        if (index == null) {
            return Optional.empty();
        }
        return ClientAssetsManager.INSTANCE.getAmmoDisplay(index.getDisplayLocation());
    }

    /// > 取某挂饰 id 对应的 display（含挂饰模型 / 贴图 / 渲染变换）。
    @Nonnull
    public static Optional<CharmDisplayInstance> getCharmDisplay(Identifier charmId) {
        if (charmId == null) {
            return Optional.empty();
        }
        var index = TimelessHelperS.getIndex(charmId, ModCustomItemTypes.CHARM.get()).orElse(null);
        if (index == null) {
            return Optional.empty();
        }
        return ClientAssetsManager.INSTANCE.getCharmDisplay(index.getDisplayLocation());
    }
}
