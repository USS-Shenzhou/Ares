package cn.ussshenzhou.ares.input;

import cn.ussshenzhou.ares.Ares;
import net.neoforged.fml.common.EventBusSubscriber;
import cn.ussshenzhou.ares.gun.TimelessHelperC;
import cn.ussshenzhou.ares.gun.item.AttachmentPath;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.t88.network.NetworkHelper;
import cn.ussshenzhou.ares.gun.network.message.c2s.C2SToggleEffectMessage;
import cn.ussshenzhou.ares.gun.util.AttachmentTreeHelper;
import cn.ussshenzhou.ares.gun.util.ToggleHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class ToggleListener {
    /// > 枪灯（手电）开关按键。
    /// >
    /// > 按下时翻转主手枪上全部「当前生效形态声明了枪灯效果」的可开关配件（含枪自带枪灯），
    /// > 状态持久化于配件/枪自身 NBT（服务端权威 + 客户端乐观预判）。渲染判据直接读 NBT，
    /// > 不再有全局布尔状态。详见 [#toggleEffect(KeyMapping, String)]。
    @SubscribeEvent
    public static void onFlashlightKeyPress(InputEvent.Key event) {
        toggleEffect(ModKey.FLASHLIGHT_TOGGLE_KEY, "flashlight");
    }

    /// > 镭射（激光指示器）开关按键。
    /// >
    /// > 按下时翻转主手枪上全部「当前生效形态声明了镭射效果」的可开关配件（含枪自带镭射），
    /// > 状态持久化于配件/枪自身 NBT（服务端权威 + 客户端乐观预判）。渲染判据直接读 NBT，
    /// > 不再有全局布尔状态。详见 [#toggleEffect(KeyMapping, String)]。
    @SubscribeEvent
    public static void onLaserKeyPress(InputEvent.Key event) {
        toggleEffect(ModKey.LASER_TOGGLE_KEY, "laser");
    }

    /// > 效果开关快捷键的共享执行逻辑（镭射 / 枪灯）。
    /// >
    /// > 按下时对主手枪执行「乐观预判」：先本地翻转枪自带效果键与配件树中全部匹配的可开关配件
    /// > （渲染立即生效），再发送 [C2SToggleEffectMessage] 由服务端权威执行；服务端随物品栏
    /// > 同步覆盖自愈（预写自愈模式，与 ModuleModeHelper 注释一致）。
    /// >
    /// > 枪上既无自带效果、也无匹配配件时不发送请求（避免写入无意义 NBT）。
    public static void toggleEffect(KeyMapping key, String effect) {
        while (key.consumeClick()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            ItemStack gun = player.getMainHandItem();
            if (!(gun.getItem() instanceof IGun)) {
                return;
            }
            int slotIndex = player.getInventory().getSelectedSlot();

            /// > 目标集合（翻转不改变树结构，路径稳定）
            List<AttachmentPath> paths = new ArrayList<>();
            List<Boolean> enabledBefore = new ArrayList<>();
            AttachmentTreeHelper.forEachEffectToggleable(gun, effect, (path, attachment) -> {
                paths.add(path);
                enabledBefore.add(ToggleHelper.isEnabled(attachment));
            });
            boolean gunHasEffect = false;
            if (hasGunEffect(gun, effect)) {
                gunHasEffect = true;
                ToggleHelper.setGunEffectEnabled(gun, effect, !ToggleHelper.getGunEffectEnabled(gun, effect));
            }
            if (!gunHasEffect && paths.isEmpty()) {
                return;
            }
            for (int i = 0; i < paths.size(); i++) {
                AttachmentTreeHelper.setToggle(gun, paths.get(i), !enabledBefore.get(i));
            }

            NetworkHelper.sendToServer(new C2SToggleEffectMessage(slotIndex, effect));
        }
    }

    /// > 枪自带效果声明（客户端渲染资源判断；服务端不做此校验）。
    private static boolean hasGunEffect(ItemStack gun, String effect) {
        var display = TimelessHelperC.getGunDisplay(gun).orElse(null);
        if (display == null) {
            return false;
        }
        if ("flashlight".equals(effect)) {
            return display.getFlashlight() != null;
        }
        return display.getLaser() != null;
    }
}
