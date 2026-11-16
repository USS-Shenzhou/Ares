package cn.ussshenzhou.ares.client.handler;

import net.neoforged.fml.common.EventBusSubscriber;
import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.item.GunItem;
import cn.ussshenzhou.ares.gun.util.DualWieldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.SubscribeEvent;

/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class ClientPreventGunClick {
    @SubscribeEvent
    public static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        /// > 主手有枪：禁止全部交互。
        if (mainHand.getItem() instanceof GunItem) {
            /// > 展示框可以交互
            HitResult hitResult = Minecraft.getInstance().hitResult;
            if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof ItemFrame) {
                return;
            }
            /// > 「主手单手枪 + 副手可用特殊物品」形态：左键开火主手枪需屏蔽（避免触发原版攻击），
            /// > 但右键放行——交由原版使用副手物品（盾牌格挡等）。
            ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
            if (DualWieldHelper.isMainOneHandWithOffhandItem(mainHand, offHand)) {
                if (event.isAttack()) {
                    event.setSwingHand(false);
                    event.setCanceled(true);
                }
                return;
            }
            /// > 这个设置为 false 就能阻止客户端粒子的生成
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }
        /// > 副手单手持枪（主手非枪）：左键已用于开火副手枪，屏蔽主手物品原本的左键（攻击）功能。
        /// > 仅单手枪在副手启用；双手枪放副手不渲染、不开火，等于未启用，不应屏蔽主手左键。
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        if (event.isAttack() && DualWieldHelper.isOneHandedGun(offHand)) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }
}
