package cn.ussshenzhou.ares.client;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.custom.CommonAssetsManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class CommonAssetsClientLifecycle {
    @SubscribeEvent
    public static void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (Minecraft.getInstance().hasSingleplayerServer()) {
            return;
        }
        CommonAssetsManager.loadBuiltinForClient(event.getPlayer().registryAccess());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CommonAssetsManager.clearBuiltinForClient();
    }
}
