package cn.ussshenzhou.ares.gun.command;

import cn.ussshenzhou.ares.Ares;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW the command root is /ares instead of /tacz; only charm bind/unbind is kept.
public class RootCommand {
    private static final String ROOT_NAME = Ares.MOD_ID;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT_NAME)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

        CharmCommand.register(root);

        dispatcher.register(root);
    }
}
