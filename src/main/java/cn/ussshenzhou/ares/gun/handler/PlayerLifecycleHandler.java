package cn.ussshenzhou.ares.gun.handler;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.capability.IGunOperator;
import cn.ussshenzhou.ares.gun.capability.ServerPlayerGunCapability;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID)
public class PlayerLifecycleHandler {
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            IGunOperator.asGunHolder(serverPlayer).ifPresent(op -> {
                if (op instanceof ServerPlayerGunCapability cap) {
                    cap.reset();
                }
            });
        }
    }
}
