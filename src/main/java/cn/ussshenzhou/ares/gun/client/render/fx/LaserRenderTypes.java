package cn.ussshenzhou.ares.gun.client.render.fx;

/* FIXME rendering
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import cn.ussshenzhou.ares.Ares;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;

/// > 镭射专用渲染类型。
/// >
/// > 继承 [RenderType] 仅用于访问 protected 的 RenderStateShard 常量，避免在业务渲染类里直接碰访问限制。
/// @author TACZ, USS_Shenzhou
public class LaserRenderTypes extends RenderType {
    private static final Identifier LASER_TEXTURE = Ares.modLoc("textures/entity/tracer.png");
    private static final Identifier LASER_DOT_TEXTURE = Ares.modLoc("textures/entity/laser_dot.png");

    private static final RenderType LASER_ADDITIVE = RenderType.create(
            "tacz_laser_additive",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            256,
            true,
            true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new TextureStateShard(LASER_TEXTURE, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false));

    /// > 落点光斑专用：径向渐变贴图 + 线性过滤（模糊），避免 32×32 渐变放大时出现阶梯。
    private static final RenderType LASER_DOT = RenderType.create(
            "tacz_laser_dot",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            256,
            true,
            true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new TextureStateShard(LASER_DOT_TEXTURE, false, true))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false));

    private LaserRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                             boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType laserAdditive() {
        return LASER_ADDITIVE;
    }

    public static RenderType laserDot() {
        return LASER_DOT;
    }
}
*/
