package cn.ussshenzhou.ares.gun.client;

import net.neoforged.fml.common.EventBusSubscriber;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.TimelessHelperC;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.gun.client.renderer.animation.GunAnimationInstance;
import cn.ussshenzhou.ares.gun.client.resource.DebugReloadManager;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class ClientCommand {
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(Ares.MOD_ID)
                .then(Commands.literal("qreload")
                        .executes(ClientCommand::reloadGunDisplay)
                )
        );
    }

    private static int reloadGunDisplay(CommandContext<CommandSourceStack> context) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IGun gunItem)) {
            context.getSource().sendFailure(Component.literal("You are not holding a gun!"));
            return 0;
        }

        Identifier gunId = gunItem.getDisplayId(stack);
        context.getSource().sendSuccess(() -> Component.literal("Start reloading display for: " + gunId), false);

        StopWatch watch = StopWatch.createStarted();
        boolean success = DebugReloadManager.reloadGunDisplay(gunId);
        watch.stop();
        double time = watch.getTime(TimeUnit.MICROSECONDS) / 1000.0;
        String str = String.format("Reload successful! Time: %.3f ms", time);
        if (success) {
            context.getSource().sendSuccess(() -> Component.literal(str), false);
            
            /// > 尝试获取当前的动画实例并强制更新
            var ani = FirstPersonRenderHandler.getActiveAnimationInstance();
            if (ani instanceof GunAnimationInstance gunAni) {
                /// > 获取新加载的 DisplayInstance
                TimelessHelperC.getGunDisplay(stack).ifPresent(gunAni::reload);
                context.getSource().sendSuccess(() -> Component.literal("Animation instance updated in-place."), false);
            } else {
                context.getSource().sendSuccess(() -> Component.literal("No active animation instance found to update."), false);
            }
            
            return Command.SINGLE_SUCCESS;
        } else {
            context.getSource().sendFailure(Component.literal("Reload failed! Check log for details."));
            return 0;
        }
    }
}
