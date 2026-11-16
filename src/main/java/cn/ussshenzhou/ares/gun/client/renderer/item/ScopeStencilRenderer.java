package cn.ussshenzhou.ares.gun.client.renderer.item;

/* FIXME rendering
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import cn.ussshenzhou.ares.gun.client.model.BedrockAttachmentModel;
import cn.ussshenzhou.ares.gun.client.renderer.PoseStackTransformHelper;
import cn.ussshenzhou.ares.gun.client.renderer.RenderHelper;
import cn.ussshenzhou.ares.gun.client.util.AimViewHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import javax.annotation.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/// > 模板缓冲瞄具的单帧渲染管线。
/// @author TACZ, USS_Shenzhou
public class ScopeStencilRenderer {
    /// > 渲染本帧的瞄具模板会话。
    /// >
    /// > @param window 当前视图位的激活窗口：槽位为 null 表示激活的是本体视位（机瞄 / 拒枪），
    /// >               此时没有瞄具处于瞄准窗口内，全部按非激活渲染
    static void render(List<Scope> scopes, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                       int light, float partialTicks, AimViewHelper.ScopeWindow window) {
        List<ScopedSlots> slotted = new ArrayList<>(scopes.size());
        int active = indexOfSlot(scopes, window.activeSlotId());
        int pending = window.transitioning() ? indexOfSlot(scopes, window.pendingSlotId()) : -1;
        float progress = Mth.clamp(window.progress(), 0f, 1f);
        boolean transitioning = window.transitioning();

        /// > 目镜身份编号跨瞄具连续递增（mask 段 / plain 段各自独立），与状态位（bit7 / bit6）正交：
        /// > 整把枪上每个目镜因此都有唯一模板值，圆切与分划板能精确按镜隔离。
        int nextMaskId = 1;
        int nextPlainId = 1;
        for (int i = 0; i < scopes.size(); i++) {
            Scope scope = scopes.get(i);
            List<BedrockAttachmentModel.OcularSlot> mask = new ArrayList<>();
            List<BedrockAttachmentModel.OcularSlot> plain = new ArrayList<>();
            BedrockAttachmentModel.OcularIdRange range = scope.model().allocateOcularIds(nextMaskId, nextPlainId, mask, plain);
            nextMaskId = range.nextMaskId();
            nextPlainId = range.nextPlainId();
            boolean activeWindow = i == active || (transitioning && i == pending);
            float radiusScale = 1f;
            if (transitioning && i == active) {
                radiusScale = 1f - progress;
            } else if (transitioning && i == pending) {
                radiusScale = progress;
            }
            slotted.add(new ScopedSlots(scope, mask, plain, radiusScale, activeWindow));
        }

        RenderHelper.enableItemEntityStencilTest();
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);

        for (ScopedSlots ss : slotted) {
            withScopePose(ss.scope(), poseStack, () -> ss.scope().model().renderOcularRing(poseStack, bufferSource,
                    renderType(ss.scope().texture(), true), renderType(ss.scope().texture(), false), light));
        }
        for (ScopedSlots ss : slotted) {
            withScopePose(ss.scope(), poseStack, () -> ss.scope().model().renderOcularStencilValues(ss.mask(), poseStack, bufferSource,
                    renderType(ss.scope().texture(), true), renderType(ss.scope().texture(), false), light));
        }
        for (ScopedSlots ss : slotted) {
            if (!ss.mask().isEmpty()) {
                withScopePose(ss.scope(), poseStack, () -> ss.scope().model().renderScopeBody(poseStack, bufferSource,
                        renderType(ss.scope().texture(), true), renderType(ss.scope().texture(), false), light, false));
            }
        }
        for (ScopedSlots ss : slotted) {
            withScopePose(ss.scope(), poseStack, () -> ss.scope().model().renderOcularStencilValues(ss.plain(), poseStack, bufferSource,
                    renderType(ss.scope().texture(), true), renderType(ss.scope().texture(), false), light));
        }
        for (ScopedSlots ss : slotted) {
            if (!ss.activeWindow() && !ss.mask().isEmpty()) {
                withScopePose(ss.scope(), poseStack, () -> ss.scope().model().renderOcularGlass(ss.mask(), poseStack,
                        bufferSource, renderType(ss.scope().texture(), true), renderType(ss.scope().texture(), false), light));
            }
        }

        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        for (ScopedSlots ss : slotted) {
            if (ss.activeWindow()) {
                float radius = ss.scope().model().getBaseCircleRadius(partialTicks) * ss.radiusScale();
                withScopePose(ss.scope(), poseStack, () -> ss.scope().model().renderStencilCircles(ss.mask(), poseStack, radius));
            }
        }
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);

        for (ScopedSlots ss : slotted) {
            withScopePose(ss.scope(), poseStack, () -> ss.scope().model().renderMasksAndDivisions(ss.mask(), ss.plain(), poseStack,
                    bufferSource, renderType(ss.scope().texture(), true), renderType(ss.scope().texture(), false), light,
                    ss.activeWindow()));
        }
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderHelper.disableItemEntityStencilTest();

        for (ScopedSlots ss : slotted) {
            if (ss.mask().isEmpty()) {
                withScopePose(ss.scope(), poseStack, () -> ss.scope().model().renderScopeBody(poseStack, bufferSource,
                        renderType(ss.scope().texture(), true), renderType(ss.scope().texture(), false), light, true));
            }
        }
        for (ScopedSlots ss : slotted) {
            withScopePose(ss.scope(), poseStack, () -> ss.scope().model().renderRemaining(poseStack, bufferSource,
                    renderType(ss.scope().texture(), true), renderType(ss.scope().texture(), false), light));
        }
    }

    private static void withScopePose(Scope scope, PoseStack poseStack, Runnable draw) {
        poseStack.pushPose();
        PoseStackTransformHelper.mulPoseAndNormal(poseStack, scope.slotTransform());
        draw.run();
        poseStack.popPose();
    }

    /// > 按槽位 id 定位瞄具在本帧会话中的下标；找不到（含本体视位）返回 -1。
    private static int indexOfSlot(List<Scope> scopes, @Nullable Identifier slotId) {
        if (slotId == null) {
            return -1;
        }
        for (int i = 0; i < scopes.size(); i++) {
            if (slotId.equals(scopes.get(i).slotId())) {
                return i;
            }
        }
        return -1;
    }

    private static RenderType renderType(Identifier texture, boolean quad) {
        return quad ? RenderType.entityCutout(texture)
                : BedrockModelRenderTypes.polyMeshCutout(texture);
    }

    record Scope(BedrockAttachmentModel model, Identifier texture, Matrix4f slotTransform,
                 Identifier slotId, @Nullable Vector3f viewOffset) {
    }

    private record ScopedSlots(Scope scope, List<BedrockAttachmentModel.OcularSlot> mask,
                               List<BedrockAttachmentModel.OcularSlot> plain, float radiusScale,
                               boolean activeWindow) {
    }
}
*/
