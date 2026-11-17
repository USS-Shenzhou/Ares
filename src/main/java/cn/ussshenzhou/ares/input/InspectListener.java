package cn.ussshenzhou.ares.input;

import cn.ussshenzhou.ares.Ares;
import net.neoforged.fml.common.EventBusSubscriber;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.gun.util.DualWieldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.SubscribeEvent;

/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class InspectListener {
    @SubscribeEvent
    public static void onInspectPress(InputEvent.Key event) {
        while (ModKey.INSPECT_KEY.consumeClick()) {
            Player player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            IGunOperator.asGunHolder(player).ifPresent(op -> op.inspect(DualWieldHelper.getPreferredHand(player)));
        }
    }
}
