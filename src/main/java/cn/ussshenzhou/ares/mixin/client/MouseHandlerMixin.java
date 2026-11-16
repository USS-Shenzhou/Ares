package cn.ussshenzhou.ares.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.gun.client.util.AimFovHelper;
import cn.ussshenzhou.ares.gun.util.math.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW moved to cn.ussshenzhou.ares.mixin.client and listed in ares.mixins.json.
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void tacz$reduceZoomSensitivity(LocalPlayer player, double yaw, double pitch, Operation<Void> original) {
        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof IGun)) {
            original.call(player, yaw, pitch);
            return;
        }

        float progress = IGunOperator.asGunHolder(player).map(IGunOperator::aimingProgress).orElse(0.0F);
        if (progress <= 0.0F) {
            original.call(player, yaw, pitch);
            return;
        }
        float fovProgress = AimFovHelper.easeInExpo(progress);

        Minecraft minecraft = Minecraft.getInstance();
        double originalFov = minecraft.options.fov().get();
        double targetFov = AimFovHelper.getAimingWorldTargetFov(mainHandItem, originalFov);
        double currentFov = Mth.lerp(fovProgress, originalFov, targetFov);
        double sensitivityMultiplier = AimFovHelper.DEFAULT_ZOOM_SENSITIVITY_BASE_MULTIPLIER;
        sensitivityMultiplier = 1.0 + (sensitivityMultiplier - 1.0) * progress;
        double denominator = MathUtil.zoomSensitivityRatio(currentFov, originalFov,
                AimFovHelper.DEFAULT_SCREEN_DISTANCE_COEFFICIENT) * sensitivityMultiplier;

        original.call(player, yaw * denominator, tacz$getCrawlPitch(player, pitch, denominator));
    }

    @Unique
    private static double tacz$getCrawlPitch(LocalPlayer player, double pitch, double denominator) {
        double finalPitch = pitch * denominator;
        if (!player.isSwimming() && player.getPose() == Pose.SWIMMING) {
            float playerPitch = -player.getXRot();
            if (playerPitch > 45.0F) {
                finalPitch = Math.max(finalPitch, 0.0);
            }
            if (playerPitch < -30.0F) {
                finalPitch = Math.min(finalPitch, 0.0);
            }
        }
        return finalPitch;
    }
}
