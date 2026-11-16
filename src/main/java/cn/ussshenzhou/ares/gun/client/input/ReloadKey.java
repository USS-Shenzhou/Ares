package cn.ussshenzhou.ares.gun.client.input;

import cn.ussshenzhou.ares.gun.client.ClientSetup;
import cn.ussshenzhou.ares.Ares;
import net.neoforged.fml.common.EventBusSubscriber;
import com.mojang.blaze3d.platform.InputConstants;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.gun.util.HeldGunHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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
            IGunOperator.asGunHolder(player).ifPresent(op -> op.reload(HeldGunHelper.getPreferredHand(player)));
        }
    }

}
