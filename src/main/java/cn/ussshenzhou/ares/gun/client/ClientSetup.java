package cn.ussshenzhou.ares.gun.client;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.client.input.AimKey;
import cn.ussshenzhou.ares.gun.client.input.FireModeSwitchKey;
import cn.ussshenzhou.ares.gun.client.input.FlashlightToggleKey;
import cn.ussshenzhou.ares.gun.client.input.LaserToggleKey;
import cn.ussshenzhou.ares.gun.client.input.InspectKey;
import cn.ussshenzhou.ares.gun.client.input.ReloadKey;
import cn.ussshenzhou.ares.gun.client.input.ShootKey;
import cn.ussshenzhou.ares.gun.client.input.SightSwitchKey;
import cn.ussshenzhou.ares.gun.client.input.UnderBarrelToggleKey;
import cn.ussshenzhou.ares.gun.client.input.ZoomKey;
import cn.ussshenzhou.ares.gun.client.resource.ClientAssetsManager;
import cn.ussshenzhou.ares.gun.init.ModEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class ClientSetup {
    // FIXME REVIEW the key category uses KeyMapping.Category (ares:gun); default keys use the SDL InputConstants constants
    // instead of GLFW.
    public static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(Ares.modLoc("gun"));

    // FIXME REVIEW the refit, ammo wheel and pack list keys are removed.
    @SubscribeEvent
    public static void onClientSetup(RegisterKeyMappingsEvent event) {
        event.registerCategory(KEY_CATEGORY);
        event.register(ReloadKey.RELOAD_KEY);
        event.register(ShootKey.SHOOT_KEY);
        event.register(InspectKey.INSPECT_KEY);
        event.register(AimKey.AIM_KEY);
        event.register(FireModeSwitchKey.FIRE_MODE_SWITCH_KEY);
        event.register(UnderBarrelToggleKey.UNDER_BARREL_TOGGLE_KEY);
        event.register(FlashlightToggleKey.FLASHLIGHT_TOGGLE_KEY);
        event.register(LaserToggleKey.LASER_TOGGLE_KEY);
        event.register(SightSwitchKey.SIGHT_SWITCH_KEY);
        event.register(ZoomKey.ZOOM_KEY);
    }

    @SubscribeEvent
    public static void onRegisterResourceListener(AddClientReloadListenersEvent event) {
        ClientAssetsManager.INSTANCE.registerListeners(event::addListener);
        /// > 资源重载时丢弃枪口火光后处理链，使其在下次使用时按新资源重建（支持 F3+T 热重载 shader）。
        // FIXME RENDER event.registerReloadListener((ResourceManagerReloadListener) manager -> FlashLightRenderer.reset());
    }

    @SubscribeEvent
    public static void onEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // FIXME REVIEW BulletEntityRenderer is commented out; NoopRenderer keeps the entity render dispatcher from failing on bullets.
        event.registerEntityRenderer(ModEntities.BULLET.get(), NoopRenderer::new);
        // FIXME RENDER EntityRenderers.register(ModEntities.BULLET.get(), BulletEntityRenderer::new);
    }
}
