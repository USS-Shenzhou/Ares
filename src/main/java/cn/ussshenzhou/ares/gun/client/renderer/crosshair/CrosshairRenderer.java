package cn.ussshenzhou.ares.gun.client.renderer.crosshair;

/* FIXME rendering
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import cn.ussshenzhou.ares.gun.attribute.InaccuracyType;
import cn.ussshenzhou.ares.gun.custom.attribute.GunPropertiesCaches;
import cn.ussshenzhou.ares.capability.IGunOperator;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.gun.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/// > 持枪准星渲染：STATIC 模式绘制配置的贴图准星；DYNAMIC 模式按当前散布绘制程序化准星
/// > （间距随散布收缩/扩散，瞄准收缩、移动扩散、开火脉冲）。散布为双端可推导数据，客户端本地计算即可。
/// @author TACZ, USS_Shenzhou
public class CrosshairRenderer {
    private static final long BLOOM_DURATION_MS = 250;

    public static void render(GuiGraphics graphics, LocalPlayer player, int width, int height) {
        if (ClientConfig.CROSSHAIR_MODE.get() == CrosshairMode.DYNAMIC) {
            renderDynamic(graphics, player, width, height);
        } else {
            renderStatic(graphics, width, height);
        }
    }

    private static void renderStatic(GuiGraphics graphics, int width, int height) {
        Identifier texture = ClientConfig.CROSSHAIR_TYPE.get().getTextureLocation();
        int x = width / 2 - 8;
        int y = height / 2 - 8;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1F, 1F, 1F, 0.9F);
        graphics.blit(texture, x, y, 0, 0, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    private static void renderDynamic(GuiGraphics graphics, LocalPlayer player, int width, int height) {
        ItemStack stack = player.getMainHandItem();
        if (IGun.asGun(stack) == null) {
            return;
        }
        float inaccuracy = effectiveInaccuracy(player, stack);
        /// > 散布角（度）→ 屏幕像素间距：gap = tan(inaccuracy°) * focalPx
        double fov = Minecraft.getInstance().options.fov().get();
        double focal = (height / 2.0) / Math.tan(Math.toRadians(fov / 2.0));
        double gap = Math.tan(Math.toRadians(inaccuracy)) * focal;

        if (ClientConfig.CROSSHAIR_BLOOM_ON_FIRE.get()) {
            long elapsed = System.currentTimeMillis() - IGunOperator.asGunHolder(player)
                    .map(IGunOperator::getLastFireTime).orElse(-1L);
            if (elapsed >= 0 && elapsed < BLOOM_DURATION_MS) {
                gap *= 1 + (1 - (double) elapsed / BLOOM_DURATION_MS) * 0.5;
            }
        }

        int g = (int) Math.round(Math.min(gap, 200.0));
        int cx = width / 2;
        int cy = height / 2;
        int len = 6;
        int half = 1;
        int color = 0xE6FFFFFF;
        /// > 四段线 + 中心点
        graphics.fill(cx - g - len, cy - half, cx - g, cy + half, color);
        graphics.fill(cx + g, cy - half, cx + g + len, cy + half, color);
        graphics.fill(cx - half, cy - g - len, cx + half, cy - g, color);
        graphics.fill(cx - half, cy + g, cx + half, cy + g + len, color);
        graphics.fill(cx - half, cy - half, cx + half, cy + half, color);
    }

    /// > 当前有效散布（度）：姿态散布与瞄准散布按瞄准进度平滑过渡，避免 {@link InaccuracyType} 的二值跳变。
    private static float effectiveInaccuracy(LocalPlayer player, ItemStack stack) {
        GunPropertiesCaches cache = GunPropertiesCaches.of(stack);
        float aimProgress = IGunOperator.asGunHolder(player).map(IGunOperator::aimingProgress).orElse(0f);
        InaccuracyType current = InaccuracyType.getInaccuracyType(player);
        float stance = (float) cache.getInaccuracy(current.isAim() ? InaccuracyType.STAND : current);
        /// > 开镜散布取当前瞄准姿态：机瞄 / 瞄具用 aim，拒枪位用自己的数值，保持与服务端判定一致
        float aim = (float) cache.getInaccuracy(current.isAim() ? current : InaccuracyType.AIM);
        float t = smoothstep(aimProgress);
        return stance + (aim - stance) * t;
    }

    private static float smoothstep(float t) {
        float x = Math.max(0.0F, Math.min(1.0F, t));
        return x * x * (3 - 2 * x);
    }
}
*/
