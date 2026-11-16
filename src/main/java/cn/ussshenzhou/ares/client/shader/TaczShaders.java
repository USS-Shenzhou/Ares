package cn.ussshenzhou.ares.client.shader;

/* FIXME rendering
import net.neoforged.fml.common.EventBusSubscriber;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import cn.ussshenzhou.ares.Ares;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import javax.annotation.Nullable;

import java.io.IOException;

/// > 客户端自定义 shader 注册。当前提供 GUI 画圆 / 环形扇区用的 `circle` shader。
/// >
/// > circle shader 用 normal 属性编码 thickness / start / end，可在一次 quad 内画出实心圆、
/// > 圆环或扇区弧段，适合做径向轮盘（如弹种选择轮盘）。
/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class TaczShaders {

    private static final Identifier CIRCLE_SHADER_LOCATION = Ares.modLoc("circle");
    @Nullable
    private static ShaderInstance circleShader;

    /// > 画圆 shader 专用顶点格式：Position + Color + UV0 + Normal（Normal 复用为 thickness/start/end）。
    public static final VertexFormat HUD_CIRCLE = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                    .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
                    .put("Color", DefaultVertexFormat.ELEMENT_COLOR)
                    .put("UV0", DefaultVertexFormat.ELEMENT_UV0)
                    .put("Normal", DefaultVertexFormat.ELEMENT_NORMAL)
                    .build()
    );

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), CIRCLE_SHADER_LOCATION, HUD_CIRCLE),
                shaderInstance -> circleShader = shaderInstance
        );
    }

    @Nullable
    public static ShaderInstance getCircleShader() {
        return circleShader;
    }
}
*/
