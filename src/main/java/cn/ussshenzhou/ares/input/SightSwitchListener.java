package cn.ussshenzhou.ares.input;

import cn.ussshenzhou.ares.Ares;
import net.neoforged.fml.common.EventBusSubscriber;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.attachment.entity.LocalGunOperator;
import cn.ussshenzhou.ares.gun.util.DualWieldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.SubscribeEvent;

/// > 瞄准视图位切换键（Alt+V）：在视图位列表（本体机瞄 / 战术拒枪位 / 安装的瞄具）中循环切换激活项。
/// > 列表构成见 `AimViewHelper`：装了模板瞄具时本体机瞄不进列表。
/// >
/// > 视点、模板圆切、倍率跟随都是纯客户端表现；拒枪姿态影响散布，由客户端把姿态结果 C2S 上报，
/// > 服务端权威落地。
/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class SightSwitchListener {
    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || !ModKey.SIGHT_SWITCH_KEY.consumeClick()) {
            return;
        }
        IGunOperator.asGunHolder(player).ifPresent(op -> {
            if (op instanceof LocalGunOperator localOp) {
                InteractionHand hand = DualWieldHelper.getPreferredHand(player);
                localOp.requestAimViewSwitch(hand);
            }
        });
    }
}
