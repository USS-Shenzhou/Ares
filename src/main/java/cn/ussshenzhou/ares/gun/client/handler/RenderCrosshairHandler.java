package cn.ussshenzhou.ares.gun.client.handler;

/* FIXME rendering
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.minecraft.client.gui.GuiGraphics;
import cn.ussshenzhou.ares.gun.client.renderer.crosshair.CrosshairRenderer;
import cn.ussshenzhou.ares.gun.client.resource.display.GunDisplayInstance;
import cn.ussshenzhou.ares.gun.TimelessHelperC;
import cn.ussshenzhou.ares.gun.item.IGun;
*/
import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.attachment.entity.GunHolderState;
import cn.ussshenzhou.ares.attachment.entity.HandGunSession;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

/// @author TACZ, USS_Shenzhou
// FIXME rendering: the only subscriber is commented out, so this class is not registered as an event subscriber.
public class RenderCrosshairHandler {
    private static final Identifier HIT_ICON = Identifier.fromNamespaceAndPath(Ares.MOD_ID, "textures/crosshair/hit/hit_marker.png");
    private static long hitTimestamp = -1L;
    private static long killTimestamp = -1L;
    private static long headShotTimestamp = -1L;

    /* FIXME rendering
    /// > 持枪时全面接管原版准星：绘制自定义准星（或按规则隐藏），并渲染命中标记。
    @SubscribeEvent(receiveCanceled = true)
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        boolean holdingGun = IGun.asGun(player.getMainHandItem()) != null
                || IGun.asGun(player.getOffhandItem()) != null;
        if (!holdingGun) {
            return;
        }

        /// > 命中标记：由服务端权威结果（S2CGunHitMessage）驱动，客户端不自行判断
        if (ClientConfig.HIT_MARKER_ENABLED.get()) {
            renderHitMarker(event.getGuiGraphics(),
                    event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
        }

        /// > 准星隐藏规则：瞄准快完成 / 改装界面 / 换弹中；display 声明 show_crosshair 可强制显示
        boolean hide = shouldHideCrosshair(player, event.getPartialTick());
        boolean forceShow = TimelessHelperC.getGunDisplay(player.getMainHandItem())
                .map(GunDisplayInstance::isShowCrosshair).orElse(false);
        if (!hide || forceShow) {
            CrosshairRenderer.render(event.getGuiGraphics(), player,
                    event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
        }
        event.setCanceled(true);
    }
    */

    private static boolean shouldHideCrosshair(LocalPlayer player, float partialTick) {
        float aimingProgress = IGunOperator.asGunHolder(player)
                .map(cap -> cap.aimingProgress(partialTick)).orElse(0f);
        if (aimingProgress > 0.9) {
            return true;
        }
        HandGunSession session = IGunOperator.asGunHolder(player)
                .map(op -> op.getSession(InteractionHand.MAIN_HAND)).orElse(null);
        return session != null && session.getActiveStates().contains(GunHolderState.RELOADING);
    }

    /// > 服务端命中反馈入口：命中/爆头/击杀标记时间戳。爆头标记红色，击杀标记外扩。
    public static void markHit(boolean headshot, boolean kill) {
        long now = System.currentTimeMillis();
        if (headshot) {
            headShotTimestamp = now;
        }
        if (kill) {
            killTimestamp = now;
        } else if (!headshot) {
            hitTimestamp = now;
        }
    }

    /* FIXME rendering
    private static void renderHitMarker(GuiGraphics graphics, int width, int height) {
        long now = System.currentTimeMillis();
        long remainHitTime = now - hitTimestamp;
        long remainKillTime = now - killTimestamp;
        long remainHeadShotTime = now - headShotTimestamp;
        long keepTime = ClientConfig.HIT_MARKER_KEEP_TIME_MS.get();
        float offset = 4f;
        float fadeTime;
        if (remainKillTime > keepTime) {
            if (remainHitTime > keepTime) {
                return;
            }
            fadeTime = remainHitTime;
        } else {
            /// > 击杀时最大外扩 4 像素
            offset += (remainKillTime * 4f) / keepTime;
            fadeTime = remainKillTime;
        }

        float x = width / 2f - 8;
        float y = height / 2f - 8;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        if (remainHeadShotTime > keepTime) {
            RenderSystem.setShaderColor(1F, 1F, 1F, 1 - fadeTime / keepTime);
        } else {
            RenderSystem.setShaderColor(1F, 0, 0, 1 - fadeTime / keepTime);
        }

        graphics.blit(HIT_ICON, (int) (x - offset), (int) (y - offset), 0, 0, 8, 8, 16, 16);
        graphics.blit(HIT_ICON, (int) (x + 8 + offset), (int) (y - offset), 8, 0, 8, 8, 16, 16);
        graphics.blit(HIT_ICON, (int) (x - offset), (int) (y + 8 + offset), 0, 8, 8, 8, 16, 16);
        graphics.blit(HIT_ICON, (int) (x + 8 + offset), (int) (y + 8 + offset), 8, 8, 8, 8, 16, 16);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }
    */
}
