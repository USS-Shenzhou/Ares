package cn.ussshenzhou.ares.gun.client.input;

import cn.ussshenzhou.ares.gun.client.ClientSetup;
import cn.ussshenzhou.ares.Ares;
import net.neoforged.fml.common.EventBusSubscriber;
import com.mojang.blaze3d.platform.InputConstants;
import cn.ussshenzhou.ares.util.GunModuleMode;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.attachment.entity.LocalGunOperator;
import cn.ussshenzhou.t88.network.NetworkHelper;
import cn.ussshenzhou.ares.gun.network.message.c2s.C2SFireModeSwitchMessage;
import cn.ussshenzhou.ares.gun.util.DualWieldHelper;
import cn.ussshenzhou.ares.gun.util.FireModeHelper;
import cn.ussshenzhou.ares.gun.util.MountedWeaponHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.bus.api.SubscribeEvent;

/// > 下挂武器模式切换按键：在主武器模式（PRIMARY）和下挂武器模式（UNDER_BARREL）之间切换。
/// > 仅在安装了下挂武器时有效。快慢机切换由 [FireModeSwitchKey] 独立处理。
/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class UnderBarrelToggleKey {
    public static final KeyMapping UNDER_BARREL_TOGGLE_KEY = new KeyMapping("key.ares.under_barrel_toggle.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_G,
            ClientSetup.KEY_CATEGORY);

    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event) {
        while (UNDER_BARREL_TOGGLE_KEY.consumeClick()) {
            Player player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            IGunOperator.asGunHolder(player).ifPresent(op -> toggle(player, op));
        }
    }

    private static void toggle(Player player, IGunOperator op) {
        /// > 模式切换过渡期间禁止再次切换。
        if (op instanceof LocalGunOperator localOp && localOp.isModeSwitching()) {
            return;
        }
        /// > 只有空闲状态才允许切换模式。
        if (!op.isIdle()) {
            return;
        }

        InteractionHand hand = DualWieldHelper.getPreferredHand(player);
        GunModuleMode targetMode = op.getModuleMode(hand) == GunModuleMode.UNDER_BARREL ? GunModuleMode.PRIMARY : GunModuleMode.UNDER_BARREL;
        if (op instanceof LocalGunOperator localOp && !localOp.canSwitchWeaponMode(hand, targetMode)) {
            return;
        }
        ItemStack gun = op.getCurrentItem(hand);
        if (!(gun.getItem() instanceof IGun)) {
            return;
        }
        /// > 未安装下挂武器时无法进入下挂模式。
        boolean hasUnderBarrel = MountedWeaponHelper.findInstalledMountedWeapon(gun).isPresent();
        if (!hasUnderBarrel) {
            return;
        }

        if (op.getModuleMode(hand) == GunModuleMode.UNDER_BARREL) {
            /// > 切回主武器：恢复到当前 NBT 记录的快慢机档位。无预测，起锁定窗口 + 发请求。
            int fireMode = FireModeHelper.getFireModeIndex(gun);
            if (op instanceof LocalGunOperator localOp) {
                localOp.requestModeSwitch(hand);
            }
            NetworkHelper.sendToServer(new C2SFireModeSwitchMessage(fireMode, hand));
        } else {
            /// > 切到下挂模式。无预测，起锁定窗口 + 发请求。
            if (op instanceof LocalGunOperator localOp) {
                localOp.requestModeSwitch(hand);
            }
            NetworkHelper.sendToServer(new C2SFireModeSwitchMessage(C2SFireModeSwitchMessage.UNDER_BARREL_MODE, hand));
        }
    }
}
