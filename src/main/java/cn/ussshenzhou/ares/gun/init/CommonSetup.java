package cn.ussshenzhou.ares.gun.init;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.command.RootCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW NetworkHandler.init (T88 registers its packets itself) and the GunPackLoader pack finder are removed; only command
// registration remains.
@EventBusSubscriber(modid = Ares.MOD_ID)
public class CommonSetup {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        RootCommand.register(event.getDispatcher());
    }
}
