package cn.ussshenzhou.ares.gun.handler;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.util.GunHolderState;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.attachment.entity.ServerPlayerGunOperator;
import cn.ussshenzhou.ares.gun.util.AttributeDataHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID)
public class LivingEntityHandler {
    public static final Identifier GUN_SPEED_MODIFIER_ID = Ares.modLoc("gun_weight_slowdown");

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        IGunOperator.asGunHolder(player).ifPresent(IGunOperator::tick);

        if (player instanceof ServerPlayer serverPlayer) {
            updateGunSpeedModifier(serverPlayer);
        }
    }

    /// > 真实换手（F 键对调主副手物品）：物品可能同 id 同值（值 + 槽启发区分不出），
    /// > 由服务端权威换手事件驱动强制重建持枪会话，与客户端动画 / 会话对齐。
    /// > 事件在物品实际对调前触发，operator 在下一 tick（物品已对调）消费。
    @SubscribeEvent
    public static void onSwapHandItems(LivingSwapItemsEvent.Hands event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            IGunOperator.asGunHolder(serverPlayer).ifPresent(op -> {
                if (op instanceof ServerPlayerGunOperator cap) {
                    cap.forceHandSwap();
                }
            });
        }
    }

    private static void updateGunSpeedModifier(ServerPlayer player) {
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof IGun)) {
            speedAttr.removeModifier(GUN_SPEED_MODIFIER_ID);
            return;
        }

        double speedMultiplier = AttributeDataHelper.moveSpeedMultiplier(mainHand);

        IGunOperator op = IGunOperator.asGunHolder(player).orElse(null);
        if (op instanceof ServerPlayerGunOperator cap) {
            if (cap.isAiming()) {
                speedMultiplier *= 0.6;
            }
            if (cap.getActiveStates().contains(GunHolderState.RELOADING)) {
                speedMultiplier *= 0.8;
            }
        }

        double modifierValue = speedMultiplier - 1;
        AttributeModifier existing = speedAttr.getModifier(GUN_SPEED_MODIFIER_ID);
        if (existing != null && Math.abs(existing.amount() - modifierValue) < 1e-6) {
            return;
        }
        speedAttr.removeModifier(GUN_SPEED_MODIFIER_ID);
        speedAttr.addTransientModifier(new AttributeModifier(
                GUN_SPEED_MODIFIER_ID, modifierValue, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
