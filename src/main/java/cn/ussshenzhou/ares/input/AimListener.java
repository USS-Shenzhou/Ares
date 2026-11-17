package cn.ussshenzhou.ares.input;

import cn.ussshenzhou.ares.Ares;
import net.neoforged.fml.common.EventBusSubscriber;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.gun.util.DualWieldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class AimListener {
    @SubscribeEvent
    public static void onAimPress(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) {
            return;
        }
        /// > 双持时右键被复用为「开火右手」（见 ShootListener），不再触发瞄准。
        /// > 「主手单手枪 + 副手可用特殊物品」形态下右键让位原版使用副手物品（盾牌格挡等），同样不瞄准。
        if (DualWieldHelper.isDualWielding(player) || DualWieldHelper.isMainOneHandWithOffhandItem(player)) {
            return;
        }
        IGunOperator.asGunHolder(player).ifPresent(cap -> {
            InteractionHand hand = DualWieldHelper.getPreferredHand(player);
            cap.aim(hand, ModKey.AIM_KEY.isDown());
        });
    }
}
