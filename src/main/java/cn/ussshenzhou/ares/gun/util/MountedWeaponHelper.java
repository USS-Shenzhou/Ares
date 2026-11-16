package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.gun.item.AttachmentPath;
import cn.ussshenzhou.ares.gun.item.AttachmentSlot;
import cn.ussshenzhou.ares.gun.item.IAttachmentHolder;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.gun.item.IMountedWeapon;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import cn.ussshenzhou.ares.gun.custom.gun.GunIndex;
import cn.ussshenzhou.ares.gun.custom.underbarrel.UnderBarrelGunIndex;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Map;
import java.util.Optional;

/// @author TACZ, USS_Shenzhou
public class MountedWeaponHelper {
    /// > 在枪（以及其已安装的子配件树）中查找已安装的下挂武器物品堆。
    @Nonnull
    public static Optional<ItemStack> findInstalledMountedWeapon(@Nonnull ItemStack gunStack) {
        IGun gun = IGun.asGun(gunStack);
        if (gun == null) {
            return Optional.empty();
        }
        return findMountedWeaponInHolder(gun, gunStack, IAttachmentHolder.MAX_ATTACHMENT_DEPTH);
    }

    @Nonnull
    public static Optional<Identifier> findInstalledMountedWeaponId(@Nonnull ItemStack gunStack) {
        return findInstalledMountedWeapon(gunStack)
                .map(stack -> CustomItemDataHelper.getCustomId(stack))
                .filter(id -> !id.equals(CustomItemDataHelper.EMPTY_ID));
    }

    /// > 查找已安装下挂武器在宿主配件树中的槽位路径（供玩法层定位并回写下挂 NBT）。
    /// >
    /// > 与 [#findInstalledMountedWeapon] 同步遍历，但返回到达该下挂武器的 [AttachmentPath]，
    /// > 配合 [AttachmentTreeHelper#writeAttachmentBack] 把玩法修改自底向上回写到宿主枪。
    @Nonnull
    public static Optional<AttachmentPath> findInstalledMountedWeaponPath(@Nonnull ItemStack gunStack) {
        IGun gun = IGun.asGun(gunStack);
        if (gun == null) {
            return Optional.empty();
        }
        AttachmentPath found = findMountedWeaponPathInHolder(gun, gunStack, AttachmentPath.EMPTY, IAttachmentHolder.MAX_ATTACHMENT_DEPTH);
        return Optional.ofNullable(found);
    }

    /// > 对 [IGun] 与 [IMountedWeapon] 通用地解析其 [GunData]。
    /// >
    /// > 下挂武器的 data 为 `UnderBarrelGunData extends GunData`，玩法层（射击/换弹）只需 GunData 视图。
    @Nullable
    public static GunData resolveGunData(@Nonnull ItemStack stack) {
        if (stack.getItem() instanceof IGun iGun) {
            return iGun.getIndex(stack).map(GunIndex::getData).orElse(null);
        }
        if (stack.getItem() instanceof IMountedWeapon mw) {
            return mw.getIndex(stack).map(UnderBarrelGunIndex::getData).orElse(null);
        }
        return null;
    }

    /// > 对 [IGun] 与 [IMountedWeapon] 通用地解析其自定义注册 id；非自定义物品返回 null。
    @Nullable
    public static Identifier resolveCustomId(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Identifier id = CustomItemDataHelper.getCustomId(stack);
        return CustomItemDataHelper.EMPTY_ID.equals(id) ? null : id;
    }

    /// > 在 holder 的直接槽位中查找下挂武器，并递归进入任意带槽位的子配件。
    /// >
    /// > 全程使用 NBT 读取（[IAttachmentHolder#getStoredAttachment]），避免触发属性缓存递归。
    @Nonnull
    private static Optional<ItemStack> findMountedWeaponInHolder(@Nonnull IAttachmentHolder holder, @Nonnull ItemStack holderStack, int remaining) {
        if (remaining <= 0) {
            return Optional.empty();
        }
        for (var entry : holder.getAttachmentSlots(holderStack).entrySet()) {
            ItemStack attachment = holder.getStoredAttachment(holderStack, entry.getValue());
            if (attachment.isEmpty()) {
                continue;
            }
            if (attachment.getItem() instanceof IMountedWeapon) {
                return Optional.of(attachment);
            }
            /// > 递归进入带槽位的子配件继续查找
            if (attachment.getItem() instanceof IAttachmentHolder childHolder) {
                Optional<ItemStack> found = findMountedWeaponInHolder(childHolder, attachment, remaining - 1);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    /// > 与 [#findMountedWeaponInHolder] 同构，但累积并返回到达下挂武器的槽位路径。
    @Nullable
    private static AttachmentPath findMountedWeaponPathInHolder(@Nonnull IAttachmentHolder holder, @Nonnull ItemStack holderStack,
                                                                @Nonnull AttachmentPath prefix, int remaining) {
        if (remaining <= 0) {
            return null;
        }
        Map<Identifier, AttachmentSlot> slots = holder.getAttachmentSlots(holderStack);
        for (var entry : slots.entrySet()) {
            ItemStack attachment = holder.getStoredAttachment(holderStack, entry.getValue());
            if (attachment.isEmpty()) {
                continue;
            }
            AttachmentPath here = prefix.append(entry.getKey());
            if (attachment.getItem() instanceof IMountedWeapon) {
                return here;
            }
            if (attachment.getItem() instanceof IAttachmentHolder childHolder) {
                AttachmentPath found = findMountedWeaponPathInHolder(childHolder, attachment, here, remaining - 1);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
