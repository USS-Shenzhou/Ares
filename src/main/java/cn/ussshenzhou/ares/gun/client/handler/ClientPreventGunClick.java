package cn.ussshenzhou.ares.gun.client.handler;

import net.neoforged.fml.common.EventBusSubscriber;
import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.item.GunItem;
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
            /// > 这个设置为 false 就能阻止客户端粒子的生成
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }
}
