package cn.ussshenzhou.ares.client.input;

import cn.ussshenzhou.ares.client.ClientSetup;
import cn.ussshenzhou.ares.Ares;
import net.neoforged.fml.common.EventBusSubscriber;
import com.mojang.blaze3d.platform.InputConstants;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.gun.util.DualWieldHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.bus.api.SubscribeEvent;

/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class ReloadKey {
    public static final KeyMapping RELOAD_KEY = new KeyMapping("key.ares.reload.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_R,
            ClientSetup.KEY_CATEGORY);

    @SubscribeEvent
    public static void onReloadPress(InputEvent.Key event) {
        while (RELOAD_KEY.consumeClick()) {
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
