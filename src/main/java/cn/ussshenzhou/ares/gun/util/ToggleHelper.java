package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.gun.TimelessHelperS;
import cn.ussshenzhou.ares.gun.item.IAttachment;
import cn.ussshenzhou.ares.gun.custom.attachment.AttachmentIndex;
import cn.ussshenzhou.ares.gun.custom.attachment.ToggleConfig;
import cn.ussshenzhou.ares.gun.custom.variant.VariantIndex;
import cn.ussshenzhou.ares.gun.init.ModCustomItemTypes;
import cn.ussshenzhou.ares.gun.init.ModDataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

/// > 可开关配件 / 枪自带效果的开关状态 NBT 读写（双端共用）。
/// >
/// > 配件开关状态存于配件自身 ItemStack NBT（键 `ToggleEnabled`），随配件实例移动，
/// > 多层树场景由 [AttachmentTreeHelper] 的自底向上回写机制序列化到顶层枪械。
/// > 未写入时按「当前生效形态」的默认值（[ToggleConfig#isDefaultEnabled()]）解释；
/// > 形态不可开关时恒为开启（旧配件行为不变）。
/// >
/// > 枪自带镭射/灯开关存于枪自身 NBT（`LaserEnabled` / `FlashlightEnabled`），缺省开启。
/// @author TACZ, USS_Shenzhou
public class ToggleHelper {
    /// > 读取配件的开关状态；未写入 NBT 时按当前生效形态（变体优先）的默认值解释。
    /// > 形态不可开关 / 无形态时返回 true（恒开，保持旧配件行为）。
    public static boolean isEnabled(ItemStack attachment) {
        if (attachment.isEmpty()) {
            return true;
        }
        Boolean enabled = attachment.get(ModDataComponents.TOGGLE_ENABLED);
        if (enabled != null) {
            return enabled;
        }
        ToggleConfig config = resolveActiveToggle(attachment);
        return config == null || config.isDefaultEnabled();
    }

    /// > 写入配件的开关状态。
    public static void setEnabled(ItemStack attachment, boolean enabled) {
        if (attachment.isEmpty()) {
            return;
        }
        attachment.set(ModDataComponents.TOGGLE_ENABLED, enabled);
    }

    /// > 解析配件「当前生效形态」的开关能力配置（双端共用）。
    /// >
    /// > 优先取当前选中变体（[VariantHelper#getVariant]）自身声明的 toggle；
    /// > 变体不可开关（未声明 toggle）时返回 null（该形态整体不可开关，基础形态不越权）；
    /// > 无变体时回退基础形态（配件 index）的 toggle；均无可开关能力返回 null。
    @Nullable
    public static ToggleConfig resolveActiveToggle(ItemStack attachment) {
        if (attachment.isEmpty()) {
            return null;
        }
        Identifier variantId = VariantHelper.getVariant(attachment);
        if (variantId != null) {
            VariantIndex variant = TimelessHelperS.getIndex(variantId, ModCustomItemTypes.ATTACHMENT_VARIANT.get()).orElse(null);
            if (variant != null) {
                return variant.getToggle();
            }
        }
        IAttachment iAttachment = IAttachment.asAttachment(attachment);
        if (iAttachment == null) {
            return null;
        }
        AttachmentIndex index = iAttachment.getIndex(attachment).orElse(null);
        if (index == null) {
            return null;
        }
        return index.getToggle();
    }

    /// > 当前生效形态的开关能力配置是否声明了指定效果。
    /// > 未声明 toggle（不可开关）的形态永不匹配——效果恒生效由显示层按 display 配置兜底。
    public static boolean matchesEffect(ItemStack attachment, String effect) {
        if (effect == null || effect.isEmpty()) {
            return false;
        }
        ToggleConfig config = resolveActiveToggle(attachment);
        return config != null && effect.equals(config.getEffect());
    }

    /// > 读取枪自带效果的开关状态（镭射 / 枪灯），缺省开启。
    public static boolean getGunEffectEnabled(ItemStack gun, String effect) {
        if (gun.isEmpty()) {
            return true;
        }
        return gun.getOrDefault(gunEffectComponent(effect), true);
    }

    /// > 写入枪自带效果的开关状态。服务端调用（枪 NBT 随物品栏同步下发）。
    public static void setGunEffectEnabled(ItemStack gun, String effect, boolean enabled) {
        if (gun.isEmpty()) {
            return;
        }
        gun.set(gunEffectComponent(effect), enabled);
    }

    private static DataComponentType<Boolean> gunEffectComponent(String effect) {
        return "flashlight".equals(effect) ? ModDataComponents.FLASHLIGHT_ENABLED.get() : ModDataComponents.LASER_ENABLED.get();
    }
}