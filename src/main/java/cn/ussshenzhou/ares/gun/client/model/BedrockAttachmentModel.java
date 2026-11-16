package cn.ussshenzhou.ares.gun.client.model;

/* FIXME rendering
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.lwjgl.opengl.GL11;
import cn.ussshenzhou.ares.gun.compat.oculus.OculusCompat;
*/
import com.mojang.logging.LogUtils;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBoneDefinition;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import javax.annotation.Nullable;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// > 普通配件模型（v2）。配件本身无动画，仅静态渲染。
/// > <p>
/// > 瞄具（scope / sight）走模板缓冲（stencil）渲染：先把目镜区域写入 stencil，
/// > 再据此渲染镜身遮罩、分划板与镜内可视圆形区域，并在枪体渲染时排除镜片内区域，
/// > 实现「透过瞄具看到的画面里不显示枪体几何」的效果。
/// > <p>
/// > 不考虑 OptiFine / 加速渲染兼容，仅实现标准渲染路径。
/// @author TACZ, USS_Shenzhou
public class BedrockAttachmentModel {
    /// > 模板值位域（8 位缓冲，全枪共用一份）：
    /// > <ul>
    /// >   <li>{@code bit7}（{@link #OCULAR_MASK_FLAG}）：像素落在某个瞄具的目镜遮罩内；</li>
    /// >   <li>{@code bit6}（{@link #OCULAR_WINDOW_FLAG}）：像素落在该目镜的屏幕空间镜窗内（已圆切）；</li>
    /// >   <li>{@code bit0..5}（{@link #IDENTITY_MASK}）：目镜身份编号，mask 段与 plain 段各自独立编号。</li>
    /// > </ul>
    /// > 于是缓冲里只有四种值：{@code 0}（目镜外）、{@code 0x80|编号}（目镜遮罩区）、
    /// > {@code 0xC0|编号}（镜内可视区）、{@code 1..0x3F}（红点 / 全息目镜，无镜窗）。
    /// > 身份编号与状态位正交，故三种模板测试都能写成一次按位与：只测身份（{@link #OCULAR_IDENTITY_TEST_MASK}）、
    /// > 只测状态（{@link #SCOPE_VIEW_CULL_MASK}）、全等（{@code 0xFF}）。
    public static final int OCULAR_MASK_FLAG = 0x80;
    public static final int OCULAR_WINDOW_FLAG = 0x40;

    /// > 状态位掩码（bit7 + bit6）。
    public static final int OCULAR_STATE_MASK = OCULAR_MASK_FLAG | OCULAR_WINDOW_FLAG;

    /// > 身份编号掩码：mask 段与 plain 段共用的值域（{@code 1..IDENTITY_MASK}）。
    public static final int IDENTITY_MASK = 0x3F;

    /// > 枪体剔除模板测试参考值 / 掩码（{@code GL_NOTEQUAL}）：模板状态位满足
    /// > {@code (v & 0xC0) == 0xC0}（即任一瞄具圆切出的镜内可视区）时不绘制枪体（镜内无枪体），
    /// > 其余区域正常绘制。该测试只读状态位，与身份编号、与场上有几个瞄具无关。
    public static final int SCOPE_VIEW_CULL_REF = OCULAR_STATE_MASK;
    public static final int SCOPE_VIEW_CULL_MASK = OCULAR_STATE_MASK;

    /// > 「命中本目镜区、无视是否已圆切」的模板测试掩码：含状态位 bit7 与全部身份位，排除 bit6。
    /// > 参考值取 {@link #ocularViewValue(int)}（{@code 0xC0|编号}），该值同时充当 REPLACE 的写入值：
    /// > 已在镜窗内的像素再次命中时写回同值（幂等），侧瞄切换过渡中两个镜的圆切重叠绘制也安全。
    public static final int OCULAR_IDENTITY_TEST_MASK = OCULAR_MASK_FLAG | IDENTITY_MASK;

    /// > 目镜遮罩区的模板值：{@code 0x80|身份编号}。
    public static int ocularMaskValue(int identity) {
        return OCULAR_MASK_FLAG | (identity & IDENTITY_MASK);
    }

    /// > 圆切（镜内可视区）的模板值：由目镜遮罩值 {@code 0x80|编号} 映射为 {@code 0xC0|编号}。
    /// > bit7 与目镜区一致，bit6 置 1 区分「圆内 / 圆外环」；身份编号保留，使每个瞄具的圆内值唯一，
    /// > 分划板仅在自己的圆内绘制，不会漏画进旁边镜子的镜内窗口。
    public static int ocularViewValue(int ocularValue) {
        return OCULAR_WINDOW_FLAG | OCULAR_MASK_FLAG | (ocularValue & IDENTITY_MASK);
    }

    private static final String SCOPE_BODY_NODE = "scope_body";
    private static final String OCULAR_RING_NODE = "ocular_ring";
    private static final String DIVISION_NODE = "division";
    private static final String OCULAR_NODE = "ocular";
    private static final String OCULAR_SIGHT_NODE = "ocular_sight";
    private static final String OCULAR_SCOPE_NODE = "ocular_scope";
    private static final Pattern OCULAR_PATTERN = Pattern.compile(
            "^(" + OCULAR_NODE + "|" + OCULAR_SIGHT_NODE + "|" + OCULAR_SCOPE_NODE + ")(_(\\d+))?$");

    private final TreeBedrockModel baseModel;
    private final TreeModelInstance instance;

    private final int scopeBodyIndex;
    private final int ocularRingIndex;
    private final List<Integer> ocularIndices = new ArrayList<>();
    private final List<Boolean> isScopeOcular = new ArrayList<>();
    private final List<Integer> divisionIndices = new ArrayList<>();

    private boolean isScope = false;
    private boolean isSight = false;
    private float scopeViewRadiusModifier = 1.0F;
    /// > 身份编号耗尽告警去重：模板值分配每帧都会执行，避免逐帧刷屏
    private boolean identityExhaustedWarned = false;

    public BedrockAttachmentModel(BedrockModelPOJO pojo) {
        this.baseModel = TreeBedrockModel.bake(pojo);
        this.instance = baseModel.createInstance();

        /// > 采集目镜骨骼（ocular / ocular_sight / ocular_scope，可带 _N 编号），按编号排序
        TreeMap<Integer, OcularEntry> oculars = new TreeMap<>();
        for (TreeBoneDefinition bone : baseModel.bones()) {
            Matcher matcher = OCULAR_PATTERN.matcher(bone.name());
            if (matcher.matches()) {
                int num = 1;
                String numStr = matcher.group(3);
                if (numStr != null) {
                    num = Integer.parseInt(numStr);
                }
                boolean scope = OCULAR_SCOPE_NODE.equals(matcher.group(1));
                oculars.put(num, new OcularEntry(bone.index(), scope));
            }
        }
        for (OcularEntry entry : oculars.values()) {
            ocularIndices.add(entry.index);
            isScopeOcular.add(entry.isScope);
        }

        /// > 采集分划板骨骼（division / division_N），并默认隐藏（只在 stencil 流程中显示）
        int divisionIndex = baseModel.getIndex(DIVISION_NODE);
        int i = 2;
        while (divisionIndex >= 0) {
            divisionIndices.add(divisionIndex);
            setBoneVisible(divisionIndex, false);
            divisionIndex = baseModel.getIndex(DIVISION_NODE + '_' + i++);
        }

        this.scopeBodyIndex = baseModel.getIndex(SCOPE_BODY_NODE);
        this.ocularRingIndex = baseModel.getIndex(OCULAR_RING_NODE);
    }

    public TreeModelInstance getInstance() {
        return instance;
    }

    /// > 模型自身坐标系下的渲染包围盒（来自 bedrock 模型 visible_bounds）。
    /// > 模型未声明 visible_bounds 时可能为 null。
    @Nullable
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return baseModel.getRenderBoundingBox();
    }

    @Nullable
    public BoneState getBone(String boneName) {
        return instance.getBone(boneName);
    }

    /// > 获取骨骼当前的全局变换矩阵（配件无动画，等价于 bind pose 变换）。
    @Nullable
    public Matrix4f getGlobalTransform(String boneName) {
        int index = baseModel.getIndex(boneName);
        return index >= 0 ? instance.getGlobalTransform(index) : null;
    }

    public void setIsScope(boolean isScope) {
        this.isScope = isScope;
    }

    public void setIsSight(boolean isSight) {
        this.isSight = isSight;
    }

    public boolean isScope() {
        return isScope;
    }

    public boolean isSight() {
        return isSight;
    }

    public void setScopeViewRadiusModifier(float scopeViewRadiusModifier) {
        this.scopeViewRadiusModifier = scopeViewRadiusModifier;
    }

    /// > 是否需要走模板缓冲渲染（瞄具且模型中含目镜骨骼）。
    public boolean needsStencil() {
        return (isScope || isSight) && !ocularIndices.isEmpty();
    }

    /* FIXME rendering
    public void renderToBuffer(PoseStack poseStack, MultiBufferSource bufferSource, RenderType quadRenderType,
                               RenderType triangleRenderType, int packedLight, int packedOverlay) {
        baseModel.renderToBuffer(instance, poseStack, bufferSource, quadRenderType, triangleRenderType, packedLight, packedOverlay,
                1, 1, 1, 1, true);
    }
    */

    /// > 分配本附件目镜的模板值：有遮罩（scope 类，规则 = 是 scope 目镜 或 附件非纯红点）目镜在前，
    /// > 无遮罩（红点 / 全息）目镜在后。
    /// > <p>
    /// > 身份编号稠密递增（1, 2, 3...，上限 {@link #IDENTITY_MASK}）并由调用方跨附件串接，因此整把枪上
    /// > 每个目镜都有唯一编号：目镜区值 = {@code 0x80|编号}（见 {@link #ocularMaskValue(int)}），
    /// > 圆内专属值 = {@code 0xC0|编号}（见 {@link #ocularViewValue(int)}）。圆切选区掩码是常量
    /// > {@link #OCULAR_IDENTITY_TEST_MASK}（只测状态位 bit7 与身份位、不测 bit6），因此「本目镜区，
    /// > 无论是否已圆切」可被精确命中，激活镜的分划板 / 圆切不会泄露进侧瞄。
    /// > 无遮罩（sight）目镜取 plain 段编号 {@code 1..0x3F}，无圆切、不设高位。
    /// > <p>
    /// > 编号耗尽时（同一把枪装了几十个目镜）按无遮罩目镜降级：宁可没有圆切与黑罩，也不产生越界值——
    /// > 越界值会落进 {@code (v & 0xC0) == 0xC0} 而被当成镜窗剔除枪体，留下「无分划板的黑洞」。
    /// >
    /// > @param maskIdStart  本附件可用的 mask 段编号起点（应由调用方从 1 起、跨附件连续递增）
    /// > @param plainIdStart 本附件可用的 plain 段编号起点（应由调用方从 1 起、跨附件连续递增）
    /// > @return 下一附件可用的编号起点
    public OcularIdRange allocateOcularIds(int maskIdStart, int plainIdStart, List<OcularSlot> maskOut, List<OcularSlot> plainOut) {
        for (int i = 0; i < ocularIndices.size(); i++) {
            boolean hasMask = isScopeOcular.get(i) || !isSight;
            if (hasMask && maskIdStart <= IDENTITY_MASK) {
                maskOut.add(new OcularSlot(i, ocularMaskValue(maskIdStart++)));
            } else if (plainIdStart <= IDENTITY_MASK) {
                /// > 无遮罩目镜，或 mask 段编号耗尽后的降级：按红点处理（无圆切、无黑罩）
                plainOut.add(new OcularSlot(i, plainIdStart++));
                if (hasMask) {
                    warnIdentityExhausted();
                }
            } else {
                /// > 两段编号都耗尽：该目镜不参与模板流程（几何与分划板均不绘制）
                warnIdentityExhausted();
            }
        }
        return new OcularIdRange(maskIdStart, plainIdStart);
    }

    /// > 身份编号耗尽（同一把枪上目镜过多）时的兜底告警，每个模型实例只提示一次。
    private void warnIdentityExhausted() {
        if (identityExhaustedWarned) {
            return;
        }
        identityExhaustedWarned = true;
        LogUtils.getLogger().warn("Ocular stencil identity exhausted (max {} per segment); extra oculars degrade to plain or are skipped: "
                        + "isScope={}, isSight={}, oculars={}", IDENTITY_MASK, isScope, isSight, ocularIndices.size());
    }

    /// > 目镜值段分配结果：下一附件可用的 mask / plain 编号起点。
    public record OcularIdRange(int nextMaskId, int nextPlainId) {
    }

    /// > 圆切基准半径（含瞄准进度缩放，不含切换过渡缩放）。
    public float getBaseCircleRadius(float partialTicks) {
        float rad = 80 * scopeViewRadiusModifier;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            float aimingProgress = IGunOperator.asGunHolder(player).map(cap -> cap.aimingProgress(partialTicks)).orElse(0f);
            rad *= aimingProgress;
        }
        return rad;
    }

    /* FIXME rendering
    /// > 渲染目镜外环（不写模板值）。调用方需处于 stencil 开启状态。
    public void renderOcularRing(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                 RenderType quadType, RenderType triangleType, int light) {
        if (ocularRingIndex >= 0) {
            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            renderBoneImmediate(ocularRingIndex, poseStack, bufferSource, quadType, triangleType, light);
        }
    }
    */

    /* FIXME rendering
    /// > 渲染镜身。{@code afterStencil} 为 true 时（纯红点路径）模板测试已关闭，直接渲染；
    /// > 否则仅在模板值 = 0 的区域渲染（镜片外），stencilOp 需保持 KEEP。
    public void renderScopeBody(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                RenderType quadType, RenderType triangleType, int light, boolean afterStencil) {
        if (scopeBodyIndex < 0) {
            return;
        }
        if (!afterStencil) {
            RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF);
        }
        renderBoneImmediate(scopeBodyIndex, poseStack, bufferSource, quadType, triangleType, light);
    }
    */

    /* FIXME rendering
    /// > 对有遮罩目镜执行屏幕空间圆形切出：{@code stencilFunc(EQUAL, 0xC0|编号, 0x80|0x3F)}——掩码含状态位
    /// > bit7 与全部身份位、排除 bit6，因此只命中**本目镜**的目镜区（{@code 0x80|编号}）与已切出的镜窗
    /// > （{@code 0xC0|编号}）：其他瞄具身份编号不同、红点段 bit7 为 0，都不会被命中，激活镜的圆切 /
    /// > 分划板不会污染侧瞄；REPLACE 写入该目镜的圆内专属值 {@code 0xC0|编号} →
    /// > 圆内 = 圆内专属值，圆外环保持 {@code 0x80|编号}。同一目镜的圆切测试与写入值由
    /// > {@link #ocularViewValue(int)} 统一推导（掩码不测 bit6，写入值由 ref 兼任）。
    /// > 调用方需已设置：stencilOp REPLACE、colorMask(false)、depthMask(false)、position shader。
    /// > 深度测试禁用：圆切线（z=-90 视深处）只写模板，不受场景深度影响。
    public void renderStencilCircles(List<OcularSlot> maskSlots, PoseStack poseStack, float rad) {
        if (maskSlots.isEmpty()) {
            return;
        }
        RenderSystem.disableDepthTest();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        for (OcularSlot slot : maskSlots) {
            /// > 仅接受目镜遮罩区值域（bit7 置位、bit6 清零）；越界值不圆切（值域边界保护）
            if ((slot.value & OCULAR_STATE_MASK) != OCULAR_MASK_FLAG) {
                continue;
            }
            RenderSystem.stencilFunc(GL11.GL_EQUAL, ocularViewValue(slot.value), OCULAR_IDENTITY_TEST_MASK);
            drawCircle(builder, poseStack, slot.index, rad);
        }
        RenderSystem.enableDepthTest();
    }
    */

    /* FIXME rendering
    /// > 仅渲染指定目镜的遮罩玻璃（不渲染分划板），用于**非激活窗口**的瞄具：在屏幕圆切之前先行
    /// > 盖上整片目镜黑罩。此时该目镜区仍为 {@code 0x80|编号}（未被激活镜的圆切改写——圆切掩码只命中
    /// > 激活镜自身的身份编号），玻璃条件必然命中；之后圆切若改写该区域，玻璃颜色已锁定，
    /// > 不会出现「未激活镜透出世界 / 漏画分划板」。
    public void renderOcularGlass(List<OcularSlot> maskSlots,
                                  PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                  RenderType quadType, RenderType triangleType, int light) {
        if (maskSlots.isEmpty()) {
            return;
        }
        RenderSystem.disableDepthTest();
        for (OcularSlot slot : maskSlots) {
            if ((slot.value & OCULAR_STATE_MASK) != OCULAR_MASK_FLAG) {
                continue;
            }
            RenderSystem.stencilFunc(GL11.GL_EQUAL, slot.value, 0xFF);
            renderBoneImmediate(ocularIndices.get(slot.index), poseStack, bufferSource, quadType, triangleType, light);
        }
        RenderSystem.enableDepthTest();
    }
    */

    /* FIXME rendering
    /// > 渲染遮罩与分划板：{@code renderGlass} 为 true 时（激活窗口）画圆外环黑罩（原值 {@code 0x80|编号}）；
    /// >  圆内专属值 {@code 0xC0|编号} 处渲染分划板（各镜唯一，不会漏画进其他镜的圆内）；
    /// >  无遮罩目镜 = 整片分划板（plain 段编号 {@code 1..IDENTITY_MASK}），与 {@code renderGlass} 无关。
    public void renderMasksAndDivisions(List<OcularSlot> maskSlots, List<OcularSlot> plainSlots,
                                        PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                        RenderType quadType, RenderType triangleType, int light, boolean renderGlass) {
        RenderSystem.disableDepthTest();
        for (OcularSlot slot : maskSlots) {
            if (slot.index >= divisionIndices.size()
                    || (slot.value & OCULAR_STATE_MASK) != OCULAR_MASK_FLAG) {
                continue;
            }
            if (renderGlass) {
                RenderSystem.stencilFunc(GL11.GL_EQUAL, slot.value, 0xFF);
                renderBoneImmediate(ocularIndices.get(slot.index), poseStack, bufferSource, quadType, triangleType, light);
            }
            RenderSystem.stencilFunc(GL11.GL_EQUAL, ocularViewValue(slot.value), 0xFF);
            renderBoneImmediate(divisionIndices.get(slot.index), poseStack, bufferSource, quadType, triangleType, light);
        }
        for (OcularSlot slot : plainSlots) {
            /// > plain 段限 1..IDENTITY_MASK：bit7 必须为 0，与目镜区（0x80|编号）/ 镜窗（0xC0|编号）隔离
            if (slot.value <= 0 || slot.value > IDENTITY_MASK || slot.index >= divisionIndices.size()) {
                continue;
            }
            RenderSystem.stencilFunc(GL11.GL_EQUAL, slot.value, 0xFF);
            renderBoneImmediate(divisionIndices.get(slot.index), poseStack, bufferSource, quadType, triangleType, light);
        }
        RenderSystem.enableDepthTest();
    }
    */

    /* FIXME rendering
    private void drawCircle(BufferBuilder builder, PoseStack poseStack, int ocularIndex, float rad) {
        Vector3f ocularCenter = getBoneCenter(poseStack, ocularIndices.get(ocularIndex));
        float centerX = ocularCenter.x() * 16 * 90;
        float centerY = ocularCenter.y() * 16 * 90;
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(centerX, centerY, -90.0D).color(255, 255, 255, 255).endVertex();
        for (int j = 0; j <= 90; j++) {
            float angle = j * ((float) Math.PI * 2F) / 90.0F;
            float sin = Mth.sin(angle);
            float cos = Mth.cos(angle);
            builder.vertex(centerX + cos * rad, centerY + sin * rad, -90.0D).color(255, 255, 255, 255).endVertex();
        }
        BufferUploader.drawWithShader(builder.end());
    }
    */

    /* FIXME rendering
    /// > 把指定目镜写入 stencil 缓冲（不写颜色 / 深度）。写入用 {@code GREATER + REPLACE}：只覆盖比自身小的值，
    /// > 等价于「重叠处编号大者胜出」的全序规则，因此与实际遍历顺序无关（无需排序）。
    /// > <p>
    /// > 模板几何与深度无关：写入期间禁用深度测试，避免瞄准近景时目镜几何因深度
    /// > 测试失败而不绘制、模板值漏写（漏写会让镜身盖满目镜 → 镜内全黑）。
    public void renderOcularStencilValues(List<OcularSlot> slots, PoseStack poseStack,
                                          MultiBufferSource.BufferSource bufferSource,
                                          RenderType quadType, RenderType triangleType, int light) {
        if (slots.isEmpty()) {
            return;
        }
        RenderSystem.disableDepthTest();
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);
        RenderSystem.stencilMask(0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        for (OcularSlot slot : slots) {
            if (slot.value > 0xFF) {
                throw new IllegalArgumentException("Ocular stencil value out of range for 8-bit buffer: " + slot.value);
            }
            RenderSystem.stencilFunc(GL11.GL_GREATER, slot.value, 0xFF);
            renderBoneImmediate(ocularIndices.get(slot.index), poseStack, bufferSource, quadType, triangleType, light);
        }
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
    }
    */

    /* FIXME rendering
    /// > 渲染配件模型中未被 stencil 流程单独处理的其余骨骼。
    /// > 临时隐藏 scope_body / ocular_ring / 目镜 / 分划板，渲染完成后恢复其可见性。
    public void renderRemaining(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                RenderType quadType, RenderType triangleType, int light) {
        List<Integer> hidden = new ArrayList<>();
        addSpecialIndex(hidden, scopeBodyIndex);
        addSpecialIndex(hidden, ocularRingIndex);
        hidden.addAll(ocularIndices);
        hidden.addAll(divisionIndices);

        boolean[] originalVisible = new boolean[hidden.size()];
        for (int i = 0; i < hidden.size(); i++) {
            BoneState bone = instance.getBone(hidden.get(i));
            if (bone != null) {
                originalVisible[i] = bone.visible;
                bone.visible = false;
            }
        }

        baseModel.renderToBuffer(instance, poseStack, bufferSource, quadType, triangleType, light, OverlayTexture.NO_OVERLAY);
        flush(bufferSource, quadType, triangleType);

        for (int i = 0; i < hidden.size(); i++) {
            BoneState bone = instance.getBone(hidden.get(i));
            if (bone != null) {
                bone.visible = originalVisible[i];
            }
        }
    }
    */

    private static void addSpecialIndex(List<Integer> list, int index) {
        if (index >= 0) {
            list.add(index);
        }
    }

    /* FIXME rendering
    /// > 立即渲染单个骨骼（含其子树）并强制 flush，使 stencil 状态与实际绘制同步。
    /// > <p>
    /// > 调用前 {@code poseStack} 应处于配件安装点坐标系；本方法会把它推进到目标骨骼的
    /// > 父骨骼全局坐标系，再交由 {@link TreeBedrockModel#renderBone} 应用骨骼自身变换。
    private void renderBoneImmediate(int boneIndex, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                     RenderType quadType, RenderType triangleType, int light) {
        if (boneIndex < 0) {
            return;
        }
        BoneState bone = instance.getBone(boneIndex);
        if (bone == null) {
            return;
        }
        boolean originalVisible = bone.visible;
        bone.visible = true;

        poseStack.pushPose();
        int parentIndex = bone.parentIndex();
        if (parentIndex >= 0) {
            Matrix4f parentTransform = instance.getGlobalTransform(parentIndex);
            poseStack.last().pose().mul(parentTransform);
            poseStack.last().normal().mul(new Matrix3f(parentTransform));
        }

        VertexConsumer quadBuffer = bufferSource.getBuffer(quadType);
        baseModel.renderBone(instance, boneIndex, poseStack, quadBuffer, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1, true);
        VertexConsumer triangleBuffer = bufferSource.getBuffer(triangleType);
        baseModel.renderBone(instance, boneIndex, poseStack, triangleBuffer, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1, false);
        flush(bufferSource, quadType, triangleType);

        poseStack.popPose();
        bone.visible = originalVisible;
    }
    */

    /* FIXME rendering
    /// > 强制提交缓冲。光影（Iris）下普通的 {@code endBatch(RenderType)} 不会立即绘制，
    /// > 必须通过 {@link OculusCompat#endBatch} 调用其全量 flush，才能让模板缓冲状态与绘制同步。
    private static void flush(MultiBufferSource.BufferSource bufferSource, RenderType quadType, RenderType triangleType) {
        if (!OculusCompat.endBatch(bufferSource)) {
            bufferSource.endBatch(quadType);
            bufferSource.endBatch(triangleType);
        }
    }
    */

    /* FIXME rendering
    /// > 取骨骼中心点在当前 {@code poseStack} 空间下的位置（用于屏幕空间圆心定位）。
    private Vector3f getBoneCenter(PoseStack poseStack, int boneIndex) {
        Matrix4f matrix = new Matrix4f(poseStack.last().pose()).mul(instance.getGlobalTransform(boneIndex));
        return matrix.getTranslation(new Vector3f());
    }
    */

    private void setBoneVisible(int boneIndex, boolean visible) {
        BoneState bone = instance.getBone(boneIndex);
        if (bone != null) {
            bone.visible = visible;
        }
    }

    private record OcularEntry(int index, boolean isScope) {
    }

    /// > stencil 会话中的目镜条目：模型内下标 + 分配模板值。
    public record OcularSlot(int index, int value) {
    }
}
