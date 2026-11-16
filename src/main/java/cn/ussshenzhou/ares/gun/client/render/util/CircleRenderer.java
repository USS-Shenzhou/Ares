package cn.ussshenzhou.ares.gun.client.render.util;

/* FIXME rendering
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import cn.ussshenzhou.ares.gun.client.shader.TaczShaders;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

/// > 基于 {@link TaczShaders#getCircleShader()} 的 GUI 画圆工具。
/// > <p>
/// > 用一个 quad 覆盖目标区域，shader 在片元层面根据 UV 到中心的距离 + 角度决定是否着色，
/// > 因此可零成本画出抗锯齿的圆 / 环 / 扇区弧段。
/// @author TACZ, USS_Shenzhou
public class CircleRenderer {

    /// > 画完整圆环。
    /// >
    /// > @param thickness 环厚度占比（0~1，1 为实心圆）
    public static void drawRing(PoseStack poseStack, float cx, float cy, float radius, int color, float thickness) {
        draw(poseStack, cx, cy, radius, color, thickness, 0f, 0f);
    }

    /// > 画环形扇区（弧段）。
    /// >
    /// > @param thickness 环厚度占比（0~1）
    /// > @param start     起始角占比（0~1，从 12 点方向顺时针）
    /// > @param end       结束角占比（0~1）
    public static void drawSector(PoseStack poseStack, float cx, float cy, float radius, int color,
                                  float thickness, float start, float end) {
        draw(poseStack, cx, cy, radius, color, thickness, start, end);
    }

    private static void draw(PoseStack poseStack, float cx, float cy, float radius, int color,
                             float thickness, float start, float end) {
        ShaderInstance shader = TaczShaders.getCircleShader();
        if (shader == null) {
            /// > shader 尚未加载（资源未就绪）时优雅跳过
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> shader);

        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, TaczShaders.HUD_CIRCLE);

        /// > thickness / start / end 编码进 normal 属性
        buf.vertex(matrix, cx - radius, cy + radius, 0).color(r, g, b, a).uv(0, 1).normal(thickness, start, end).endVertex();
        buf.vertex(matrix, cx + radius, cy + radius, 0).color(r, g, b, a).uv(1, 1).normal(thickness, start, end).endVertex();
        buf.vertex(matrix, cx + radius, cy - radius, 0).color(r, g, b, a).uv(1, 0).normal(thickness, start, end).endVertex();
        buf.vertex(matrix, cx - radius, cy - radius, 0).color(r, g, b, a).uv(0, 0).normal(thickness, start, end).endVertex();

        RenderSystem.depthMask(false);
        BufferUploader.drawWithShader(buf.end());
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
}
*/
