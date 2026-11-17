package cn.ussshenzhou.ares.input;

import cn.ussshenzhou.ares.Ares;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class ModKey {
    public static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(Ares.modLoc("gun"));

    public static final KeyMapping RELOAD_KEY = new KeyMapping("key.ares.reload.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_R,
            KEY_CATEGORY);

    public static final KeyMapping SHOOT_KEY = new KeyMapping("key.ares.shoot.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.MOUSE,
            InputConstants.MOUSE_BUTTON_LEFT,
            KEY_CATEGORY);

    public static final KeyMapping INSPECT_KEY = new KeyMapping("key.ares.inspect.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_H,
            KEY_CATEGORY);

    public static final KeyMapping AIM_KEY = new KeyMapping("key.ares.aim.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.MOUSE,
            InputConstants.MOUSE_BUTTON_RIGHT,
            KEY_CATEGORY);

    public static final KeyMapping FIRE_MODE_SWITCH_KEY = new KeyMapping("key.ares.fire_mode_switch.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_G,
            KEY_CATEGORY);

    public static final KeyMapping UNDER_BARREL_TOGGLE_KEY = new KeyMapping("key.ares.under_barrel_toggle.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_G,
            KEY_CATEGORY);

    public static final KeyMapping FLASHLIGHT_TOGGLE_KEY = new KeyMapping("key.ares.flashlight_toggle.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_L,
            KEY_CATEGORY);

    public static final KeyMapping LASER_TOGGLE_KEY = new KeyMapping("key.ares.laser_toggle.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_K,
            KEY_CATEGORY);

    public static final KeyMapping SIGHT_SWITCH_KEY = new KeyMapping("key.ares.sight_switch.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_V,
            KEY_CATEGORY);

    public static final KeyMapping ZOOM_KEY = new KeyMapping("key.ares.zoom.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_V,
            KEY_CATEGORY);

    @SubscribeEvent
    public static void onClientSetup(RegisterKeyMappingsEvent event) {
        event.registerCategory(KEY_CATEGORY);
        event.register(RELOAD_KEY);
        event.register(SHOOT_KEY);
        event.register(INSPECT_KEY);
        event.register(AIM_KEY);
        event.register(FIRE_MODE_SWITCH_KEY);
        event.register(UNDER_BARREL_TOGGLE_KEY);
        event.register(FLASHLIGHT_TOGGLE_KEY);
        event.register(LASER_TOGGLE_KEY);
        event.register(SIGHT_SWITCH_KEY);
        event.register(ZOOM_KEY);
    }
}
