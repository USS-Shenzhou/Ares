package cn.ussshenzhou.ares.client.event;

import cn.ussshenzhou.ares.gun.gameplay.MuzzleReference;
import cn.ussshenzhou.ares.client.renderer.item.MuzzleFlashHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/// @author TACZ, USS_Shenzhou
public class ClientShootEventHandler {
    public static void onShootPacket(int shooterId, InteractionHand hand, MuzzleReference muzzleReference) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(shooterId);
        if (entity == null) {
            return;
        }
        MuzzleFlashHandler.onEntityShoot(entity, hand, muzzleReference);
    }
}
