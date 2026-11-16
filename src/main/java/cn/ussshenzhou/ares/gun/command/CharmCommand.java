package cn.ussshenzhou.ares.gun.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import cn.ussshenzhou.ares.gun.TimelessHelperS;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.gun.custom.charm.GunCharmData;
import cn.ussshenzhou.ares.gun.init.ModCustomItemTypes;
import cn.ussshenzhou.ares.gun.init.ModItems;
import cn.ussshenzhou.ares.gun.util.CustomItemDataHelper;
import cn.ussshenzhou.ares.gun.util.GunCharmDataHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.joml.Vector3f;

import java.util.List;

/// > 挂饰调试命令：在没有拾取 UI（阶段 5）之前，用命令把挂饰绑定到主手枪，用于验证渲染。
/// >
/// > 服务端权威写入枪械 NBT（[GunCharmDataHelper]），随同步下发到客户端渲染。
/// > 默认命中点取锚点骨骼原点、法线取 +Z、偏移 0。
/// @author TACZ, USS_Shenzhou
public class CharmCommand {
    private static final SuggestionProvider<CommandSourceStack> CHARM_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(
                    TimelessHelperS.getAllIndexes(ModCustomItemTypes.CHARM.get()).keySet(), builder);

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("charm")
                .then(Commands.literal("bind")
                        .then(Commands.argument("charm_id", StringArgumentType.string())
                                .suggests(CHARM_SUGGESTIONS)
                                .then(Commands.argument("bone", StringArgumentType.string())
                                        .executes(CharmCommand::bind)
                                )
                        )
                )
                .then(Commands.literal("unbind")
                        .executes(CharmCommand::unbind)
                )
        );
    }

    private static int bind(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        ItemStack gunStack = player.getMainHandItem();
        if (!(gunStack.getItem() instanceof IGun)) {
            source.sendFailure(Component.literal("主手不是枪械"));
            return 0;
        }

        String rawCharmId = StringArgumentType.getString(ctx, "charm_id");
        Identifier charmId = Identifier.tryParse(rawCharmId);
        if (charmId == null || TimelessHelperS.getIndex(charmId, ModCustomItemTypes.CHARM.get()).isEmpty()) {
            source.sendFailure(Component.literal("未知挂饰 id：" + rawCharmId));
            return 0;
        }
        String bone = StringArgumentType.getString(ctx, "bone");

        ItemStack charmStack = new ItemStack(ModItems.CHARM.get());
        CustomItemDataHelper.setCustomId(charmStack, charmId);

        GunCharmData data = new GunCharmData(
                ItemStackTemplate.fromNonEmptyStack(charmStack),
                IGun.asGun(gunStack).getDisplayId(gunStack),
                bone,
                new Vector3f(0, 0, 0),
                new Vector3f(0, 0, 1),
                new Vector3f(0, 0, 0)
        );
        GunCharmDataHelper.setSingle(gunStack, data);
        source.sendSuccess(() -> Component.literal("已绑定挂饰 " + charmId + " 到骨骼 " + bone), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int unbind(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        ItemStack gunStack = player.getMainHandItem();
        if (!(gunStack.getItem() instanceof IGun)) {
            source.sendFailure(Component.literal("主手不是枪械"));
            return 0;
        }
        List<ItemStack> removed = GunCharmDataHelper.removeAll(gunStack);
        source.sendSuccess(() -> Component.literal("已卸下 " + removed.size() + " 个挂饰"), false);
        return Command.SINGLE_SUCCESS;
    }
}
