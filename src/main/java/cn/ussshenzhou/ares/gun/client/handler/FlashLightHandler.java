package cn.ussshenzhou.ares.gun.client.handler;

/* FIXME rendering
import net.neoforged.fml.common.EventBusSubscriber;
import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.gun.client.render.fx.FlashLightRenderer;
import cn.ussshenzhou.ares.gun.client.renderer.entity.FirstPersonPoseCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Vector3f;

/// > 枪灯（手电）环境照明后处理触发器。
/// >
/// > 在场景渲染完成（[RenderLevelStageEvent.Stage#AFTER_LEVEL]）后，若本机玩家第一人称持枪
/// > 且当前帧存在 active 枪灯（枪自带或配件，受开关状态门控），则执行 [FlashLightRenderer]
/// > 的屏幕空间聚光灯后处理。光源参数由 `BedrockGunItemRenderer` 在 afterRender 写入
/// > [FirstPersonPoseCache]（与枪体 bob/sway/动画同步）。
/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class FlashLightHandler {
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (IGun.asGun(mainHand) == null || !FirstPersonPoseCache.isFlashlightActive()) {
            return;
        }

        Vector3f from = FirstPersonPoseCache.getFlashlightFrom();
        Vector3f forward = FirstPersonPoseCache.getFlashlightForward();
        float range = FirstPersonPoseCache.getFlashlightRange() > 0.0f
                ? FirstPersonPoseCache.getFlashlightRange() : 32.0f;
        Vector3f to = new Vector3f(from).add(new Vector3f(forward).normalize());

        Vector3f color = new Vector3f(FirstPersonPoseCache.getFlashlightColor());
        FlashLightRenderer.renderFlashlight(
                event.getProjectionMatrix(), from, to, color,
                FlashLightRenderer.DEFAULT_FLASHLIGHT_ANGLE, range, FlashLightRenderer.DEFAULT_FLASHLIGHT_LUMINANCE);
    }
}
*/
