package cn.ussshenzhou.ares.gun.client.input;

import cn.ussshenzhou.ares.gun.client.ClientSetup;
import cn.ussshenzhou.ares.Ares;
import net.neoforged.fml.common.EventBusSubscriber;
import com.mojang.blaze3d.platform.InputConstants;
import cn.ussshenzhou.ares.util.GunModuleMode;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.attachment.entity.LocalGunOperator;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import cn.ussshenzhou.ares.gun.custom.gun.GunIndex;
import cn.ussshenzhou.t88.network.NetworkHelper;
import cn.ussshenzhou.ares.gun.network.message.c2s.C2SFireModeSwitchMessage;
import cn.ussshenzhou.ares.gun.util.DualWieldHelper;
import cn.ussshenzhou.ares.gun.util.FireModeHelper;
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

/// > 快慢机切换按键：在枪械的开火模式（fireModes）之间循环切换。
/// > 下挂武器模式切换由 [UnderBarrelToggleKey] 独立处理。
/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class FireModeSwitchKey {
    public static final KeyMapping FIRE_MODE_SWITCH_KEY = new KeyMapping("key.ares.fire_mode_switch.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_G,
            ClientSetup.KEY_CATEGORY);

    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event) {
        while (FIRE_MODE_SWITCH_KEY.consumeClick()) {
            Player player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            IGunOperator.asGunHolder(player).ifPresent(op -> switchNext(player, op));
        }
    }

    private static void switchNext(Player player, IGunOperator op) {
        /// > 模式切换过渡期间禁止再次切换。
        if (op instanceof LocalGunOperator localOp && localOp.isModeSwitching()) {
            return;
        }
        /// > 只有空闲状态才允许切换开火模式
        if (!op.isIdle()) {
            return;
        }
        InteractionHand hand = DualWieldHelper.getPreferredHand(player);
        /// > 下挂模式下不响应快慢机切换（先用 B 切回主武器再切档）。
        if (op.getModuleMode(hand) == GunModuleMode.UNDER_BARREL) {
            return;
        }

        if (op instanceof LocalGunOperator localOp && !localOp.canSwitchWeaponMode(hand, GunModuleMode.PRIMARY)) {
            return;
        }
        ItemStack gun = op.getCurrentItem(hand);
        if (!(gun.getItem() instanceof IGun iGun)) {
            return;
        }
        GunData data = iGun.getIndex(gun).map(GunIndex::getData).orElse(null);
        if (data == null) {
            return;
        }

        int modeCount = data.getFireModes().size();
        if (modeCount <= 1) {
            return;
        }

        int currentSlot = FireModeHelper.getFireModeIndex(gun);
        if (currentSlot < 0 || currentSlot >= modeCount) {
            currentSlot = 0;
        }
        int nextSlot = (currentSlot + 1) % modeCount;

        /// > 无预测：起锁定窗口 + 发请求，权威 NBT 同步回来后由会话对账重建开火序列并播拨杆动画。
        if (op instanceof LocalGunOperator localOp) {
            localOp.requestModeSwitch(hand);
        }
        NetworkHelper.sendToServer(new C2SFireModeSwitchMessage(nextSlot, hand));
    }
}
