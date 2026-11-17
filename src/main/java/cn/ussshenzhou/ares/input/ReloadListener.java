package cn.ussshenzhou.ares.input;

import cn.ussshenzhou.ares.Ares;
import net.neoforged.fml.common.EventBusSubscriber;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.gun.util.DualWieldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.SubscribeEvent;

/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class ReloadListener {
    @SubscribeEvent
    public static void onReloadPress(InputEvent.Key event) {
        while (ModKey.RELOAD_KEY.consumeClick()) {
            Player player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            IGunOperator.asGunHolder(player).ifPresent(op -> {
                if (DualWieldHelper.isDualWielding(player)) {
                    /// > 双持：一次触发两手各自换弹。
                    op.reload(InteractionHand.MAIN_HAND);
                    op.reload(InteractionHand.OFF_HAND);
                } else {
                    op.reload(DualWieldHelper.getPreferredHand(player));
                }
            });
        }
    }

}
