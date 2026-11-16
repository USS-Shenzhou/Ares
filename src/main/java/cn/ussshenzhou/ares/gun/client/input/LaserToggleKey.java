package cn.ussshenzhou.ares.gun.client.input;

import cn.ussshenzhou.ares.gun.client.ClientSetup;
import cn.ussshenzhou.ares.Ares;
import net.neoforged.fml.common.EventBusSubscriber;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.bus.api.SubscribeEvent;

/// > 镭射（激光指示器）开关按键。
/// > <p>
/// > 按下时翻转主手枪上全部「当前生效形态声明了镭射效果」的可开关配件（含枪自带镭射），
/// > 状态持久化于配件/枪自身 NBT（服务端权威 + 客户端乐观预判）。渲染判据直接读 NBT，
/// > 不再有全局布尔状态。详见 {@link EffectToggleInput}。
/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class LaserToggleKey {
    public static final KeyMapping LASER_TOGGLE_KEY = new KeyMapping("key.ares.laser_toggle.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_K,
            ClientSetup.KEY_CATEGORY);

    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event) {
        EffectToggleInput.toggleEffect(LASER_TOGGLE_KEY, "laser");
    }
}