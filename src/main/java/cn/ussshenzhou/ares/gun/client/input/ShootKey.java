package cn.ussshenzhou.ares.gun.client.input;

import cn.ussshenzhou.ares.gun.client.ClientSetup;
import cn.ussshenzhou.ares.Ares;
import net.neoforged.fml.common.EventBusSubscriber;
import com.mojang.blaze3d.platform.InputConstants;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.gun.util.DualWieldHelper;
import cn.ussshenzhou.ares.gun.util.FireModeHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.bus.api.SubscribeEvent;

/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class ShootKey {
    public static final KeyMapping SHOOT_KEY = new KeyMapping("key.ares.shoot.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.MOUSE,
            InputConstants.MOUSE_BUTTON_LEFT,
            ClientSetup.KEY_CATEGORY);

    /// > 左键（射击键）边沿/全自动按住态。
    private static boolean leftWasDown = false;
    private static boolean leftHoldingAuto = false;
    /// > 右键（瞄准键）边沿/全自动按住态——仅双持时复用为「开火右手」。
    private static boolean rightWasDown = false;
    private static boolean rightHoldingAuto = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) {
            resetState();
            return;
        }

        boolean leftDown = SHOOT_KEY.isDown();
        boolean leftRising = leftDown && !leftWasDown;
        boolean leftFalling = !leftDown && leftWasDown;
        leftWasDown = leftDown;

        boolean rightDown = AimKey.AIM_KEY.isDown();
        boolean rightRising = rightDown && !rightWasDown;
        boolean rightFalling = !rightDown && rightWasDown;
        rightWasDown = rightDown;

        if (!leftDown) {
            leftHoldingAuto = false;
        }
        if (!rightDown) {
            rightHoldingAuto = false;
        }

        IGunOperator.asGunHolder(player).ifPresent(op -> {
            /// > 玩家正在使用物品（如举盾格挡、吃东西）时禁止开火。
            if (player.isUsingItem()) {
                leftHoldingAuto = false;
                rightHoldingAuto = false;
                return;
            }
            if (DualWieldHelper.isDualWielding(player)) {
                /// > 双持：左键开火左手（OFF_HAND），右键开火右手（MAIN_HAND）。两手互不影响。
                if (leftDown) {
                    leftHoldingAuto = fireSingle(op, InteractionHand.OFF_HAND, leftRising);
                } else if (leftFalling) {
                    fireReleaseIfNeeded(op, InteractionHand.OFF_HAND);
                }
                if (rightDown) {
                    rightHoldingAuto = fireSingle(op, InteractionHand.MAIN_HAND, rightRising);
                } else if (rightFalling) {
                    fireReleaseIfNeeded(op, InteractionHand.MAIN_HAND);
                }
            } else {
                /// > 单枪：仅左键开火当前手，右键交由 AimKey 处理瞄准。
                rightHoldingAuto = false;
                InteractionHand hand = DualWieldHelper.getPreferredHand(player);
                if (leftDown) {
                    leftHoldingAuto = fireSingle(op, hand, leftRising);
                } else if (leftFalling) {
                    fireReleaseIfNeeded(op, hand);
                }
            }
        });
    }

    /// > 单手开火：全自动首发由此触发、后续连射由 RenderTick 驱动；半自动仅按下瞬间触发一次。
    /// > 返回该手是否处于全自动按住态。
    private static boolean fireSingle(IGunOperator op, InteractionHand hand, boolean risingEdge) {
        boolean auto = FireModeHelper.isCurrentModeAuto(op.getCurrentItem(hand));
        if (risingEdge) {
            op.fire(hand);
        }
        return auto;
    }

    /// > 松手发射：仅当当前触发方式需要松手发射（charge / delayed / 脚本 trigger）时，扳机 falling-edge 调
    /// > `op.fireRelease(hand)`。instant 触发无需松手发射，跳过。
    private static void fireReleaseIfNeeded(IGunOperator op, InteractionHand hand) {
        if (FireModeHelper.currentTriggerNeedsRelease(op.getCurrentItem(hand))) {
            op.fireRelease(hand);
        }
    }

    private static void resetState() {
        leftWasDown = false;
        leftHoldingAuto = false;
        rightWasDown = false;
        rightHoldingAuto = false;
    }

    // FIXME REVIEW auto fire is driven by RenderFrameEvent.Pre in place of the Forge RenderTickEvent.
    @SubscribeEvent
    public static void onRenderTick(RenderFrameEvent.Pre event) {
        if (!leftHoldingAuto && !rightHoldingAuto) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) {
            return;
        }

        IGunOperator.asGunHolder(player).ifPresent(op -> {
            /// > 玩家正在使用物品（如举盾格挡、吃东西）时禁止全自动持续开火。
            if (player.isUsingItem()) {
                return;
            }
            if (DualWieldHelper.isDualWielding(player)) {
                /// > 双持全自动：各手按各自按住态持续请求开火，RPM 冷却在会话侧节流。
                if (leftHoldingAuto) {
                    op.fire(InteractionHand.OFF_HAND);
                }
                if (rightHoldingAuto) {
                    op.fire(InteractionHand.MAIN_HAND);
                }
            } else if (leftHoldingAuto) {
                /// > 单枪：仅全自动持续请求开火（RPM 冷却在会话侧节流）。
                op.fire(DualWieldHelper.getPreferredHand(player));
            }
        });
    }
}
