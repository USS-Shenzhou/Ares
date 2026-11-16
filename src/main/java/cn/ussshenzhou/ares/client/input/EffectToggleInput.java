package cn.ussshenzhou.ares.client.input;

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

import java.util.ArrayList;
import java.util.List;

/// > 效果开关快捷键的共享执行逻辑（镭射 / 枪灯）。
/// >
/// > 按下时对主手枪执行「乐观预判」：先本地翻转枪自带效果键与配件树中全部匹配的可开关配件
/// > （渲染立即生效），再发送 [C2SToggleEffectMessage] 由服务端权威执行；服务端随物品栏
/// > 同步覆盖自愈（预写自愈模式，与 ModuleModeHelper 注释一致）。
/// >
/// > 枪上既无自带效果、也无匹配配件时不发送请求（避免写入无意义 NBT）。
/// @author TACZ, USS_Shenzhou
public class EffectToggleInput {
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