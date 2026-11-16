package cn.ussshenzhou.ares.client.input;

import cn.ussshenzhou.ares.client.ClientSetup;
import cn.ussshenzhou.ares.Ares;
import net.neoforged.fml.common.EventBusSubscriber;
import com.mojang.blaze3d.platform.InputConstants;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.gun.util.DualWieldHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class AimKey {
    public static final KeyMapping AIM_KEY = new KeyMapping("key.ares.aim.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.MOUSE,
            InputConstants.MOUSE_BUTTON_RIGHT,
            ClientSetup.KEY_CATEGORY);

    @SubscribeEvent
    public static void onAimPress(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) {
            return;
        }
        /// > 双持时右键被复用为「开火右手」（见 ShootKey），不再触发瞄准。
        /// > 「主手单手枪 + 副手可用特殊物品」形态下右键让位原版使用副手物品（盾牌格挡等），同样不瞄准。
        if (DualWieldHelper.isDualWielding(player) || DualWieldHelper.isMainOneHandWithOffhandItem(player)) {
            return;
        }
        IGunOperator.asGunHolder(player).ifPresent(cap -> {
            InteractionHand hand = DualWieldHelper.getPreferredHand(player);
            cap.aim(hand, AIM_KEY.isDown());
        });
    }
}
