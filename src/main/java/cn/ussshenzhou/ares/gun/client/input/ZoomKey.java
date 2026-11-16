package cn.ussshenzhou.ares.gun.client.input;

import cn.ussshenzhou.ares.gun.client.ClientSetup;
import cn.ussshenzhou.ares.Ares;
import net.neoforged.fml.common.EventBusSubscriber;
import com.mojang.blaze3d.platform.InputConstants;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.attachment.entity.LocalGunOperator;
import cn.ussshenzhou.ares.gun.util.HeldGunHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.bus.api.SubscribeEvent;

/// > 变倍键（默认 V）：开镜时按下在当前激活瞄具的倍率档位间循环；开镜时按住并拨动滚轮进行无级变倍
/// > （仅瞄具声明了 {@code zoom_fov_range} 时）。未开镜的 V 不响应（预留近战）。
/// > <p>
/// > 侧瞄切换为独立组合键（默认 Alt+V，见 {@link SightSwitchKey}）。滚轮判定用
/// > {@link KeyMapping#isDown()}（尊重玩家自定义键位，不做物理键硬编码）；键位冲突由
/// > Forge 按键体系在设置界面按既有规则处理。
/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class ZoomKey {
    public static final KeyMapping ZOOM_KEY = new KeyMapping("key.ares.zoom.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_V,
            ClientSetup.KEY_CATEGORY);

    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || !ZOOM_KEY.consumeClick()) {
            return;
        }
        IGunOperator.asGunHolder(player)
                .filter(op -> op instanceof LocalGunOperator)
                .map(op -> (LocalGunOperator) op)
                .ifPresent(localOp -> {
                    InteractionHand hand = HeldGunHelper.getPreferredHand(player);
                    if (isAiming(localOp, hand)) {
                        localOp.requestZoomStep(hand);
                    }
                });
    }

    /// > 变倍键按住 + 开镜 + 当前瞄具支持无级变倍时，消费滚轮为变倍并阻止原版切换物品栏。
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!ZOOM_KEY.isDown() || event.getScrollDeltaY() == 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || mc.gui.screen() != null) {
            return;
        }
        IGunOperator.asGunHolder(player)
                .filter(op -> op instanceof LocalGunOperator)
                .map(op -> (LocalGunOperator) op)
                .ifPresent(localOp -> {
                    InteractionHand hand = HeldGunHelper.getPreferredHand(player);
                    if (isAiming(localOp, hand)) {
                        localOp.requestZoomDelta(hand, event.getScrollDeltaY());
                        event.setCanceled(true);
                    }
                });
    }

    private static boolean isAiming(LocalGunOperator op, InteractionHand hand) {
        return op.aimingProgress(hand, 1.0D) > 0.0F;
    }
}