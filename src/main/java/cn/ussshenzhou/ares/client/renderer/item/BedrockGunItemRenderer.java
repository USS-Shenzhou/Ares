package cn.ussshenzhou.ares.client.renderer.item;
/* FIXME rendering
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.lwjgl.opengl.GL11;
import cn.ussshenzhou.ares.client.handler.LaserHandler;
import cn.ussshenzhou.ares.client.render.fx.LaserRenderer;
import cn.ussshenzhou.ares.client.render.fx.ShellRenderer;
import cn.ussshenzhou.ares.client.renderer.RenderHelper;
import cn.ussshenzhou.ares.client.render.fx.LaserVisualResolver;
import cn.ussshenzhou.ares.gun.compat.oculus.OculusCompat;
import cn.ussshenzhou.ares.client.render.fx.LaserBeamManager;
*/
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.IFPGeoItemRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.renderer.AbstractGeoItemRendererV2;
import com.maydaymemory.mae.basic.*;
import com.maydaymemory.mae.blend.EulerAdditiveBlender;
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender;
import cn.ussshenzhou.ares.gun.TimelessHelperC;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.attachment.entity.LocalGunOperator;
import cn.ussshenzhou.ares.gun.item.AttachmentPath;
import cn.ussshenzhou.ares.gun.item.AttachmentSlot;
import cn.ussshenzhou.ares.gun.item.IAttachment;
import cn.ussshenzhou.ares.gun.item.IAttachmentHolder;
import cn.ussshenzhou.ares.gun.item.ICustomItem;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.client.model.BedrockAttachmentModel;
import cn.ussshenzhou.ares.client.model.BedrockGunModel;
import cn.ussshenzhou.ares.client.renderer.AttachmentMountHelper;
import cn.ussshenzhou.ares.client.renderer.FirstPersonSpaceHelper;
import cn.ussshenzhou.ares.client.renderer.animation.GunAnimationInstance;
import cn.ussshenzhou.ares.gun.custom.gun.GunIndex;
import cn.ussshenzhou.ares.client.renderer.entity.FirstPersonPoseCache;

import cn.ussshenzhou.ares.client.refit.RefitFocusSmoothing;
import cn.ussshenzhou.ares.client.refit.RefitTransform;
import cn.ussshenzhou.ares.client.resource.display.AttachmentDisplay;
import cn.ussshenzhou.ares.client.resource.display.GunDisplay;
import cn.ussshenzhou.ares.client.resource.display.SlotAdapterDisplay;
import cn.ussshenzhou.ares.client.resource.display.AttachmentSlotDisplayCfg;
import cn.ussshenzhou.ares.gun.util.AdapterLookupHelper;
import cn.ussshenzhou.ares.gun.util.CustomItemDataHelper;
import cn.ussshenzhou.ares.gun.util.DualWieldHelper;
import cn.ussshenzhou.ares.gun.util.SlotAdapterHelper;
import cn.ussshenzhou.ares.gun.util.ToggleHelper;
import cn.ussshenzhou.ares.gun.util.math.MathUtil;
import cn.ussshenzhou.ares.gun.util.math.SecondOrderDynamics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import org.joml.*;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import static net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND;

/// @author TACZ, USS_Shenzhou
// FIXME rendering AbstractGeoItemRendererV2 does not implement IFPGeoItemRenderer while its rendering part is commented,
// so it is implemented here to keep the logic overrides (animation instance, put-away duration and so on) active.
// FIXME rendering only the drawing methods are commented here and in the Bedrock*Model classes; logic methods such as
// createAnimationInstance, isSameItem and getPutAwayDuration stay active.
public class BedrockGunItemRenderer extends AbstractGeoItemRendererV2 implements IFPGeoItemRenderer {
    private static final EulerAdditiveBlender BLENDER = new SimpleEulerAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);
    /// > 用于生成瞄准动作的运动曲线，使动作看起来更平滑
    private static final SecondOrderDynamics AIMING_DYNAMICS = new SecondOrderDynamics(1.2f, 1.2f, 0.5f, new float[]{0});

    /// > 改装槽位聚焦在切换时固定起点与终点，再按绝对时间计算中间矩阵。
    private static final Matrix4f refitFocusMatrix = new Matrix4f();
    private static final Matrix4f refitFocusStartMatrix = new Matrix4f();
    private static final Matrix4f refitFocusTargetMatrix = new Matrix4f();
    private static boolean refitFocusInit = false;
    private static long refitFocusStartNanos = 0L;

    private static SecondOrderDynamics SWITCH_VIEW_DYNAMICS;

    /// > 记录本帧已在枪体渲染前完成模板缓冲渲染的瞄具槽位，避免在 renderAttachments 中重复绘制
    private final Set<Identifier> stencilHandledSlots = new HashSet<>();
    /* FIXME rendering
    /// > 本帧已收集的第一人称激光绘制条目（主枪 + 下挂）：下挂在 renderAttachments 阶段收集，
    /// > 主枪在 afterRender 的模板窗口内收集；同一帧内光束（窗口内）与光斑（窗口外）分 pass 绘制。
    private final List<LaserDrawEntry> frameLaserDraws = new ArrayList<>();
    */
    /// > 镜内剔除窗口是否仍生效：renderModel 画完枪体后保持开启，配件渲染后再关闭
    private boolean gunStencilCulling = false;

    private static final Map<InteractionHand, Map<ParticleEmitterInstance, String>> MUZZLE_EMITTER_LOCATORS =
            new EnumMap<>(InteractionHand.class);
    /// > updateParticleEmitterTransforms 捕获的渲染基准 pose（与 renderParticlesIfAny 同坐标系），
    /// > 用于把 afterRender 的枪口视图矩阵换算回该基准下的模型空间变换。主副手各存各的 pass 基准。
    private static final Map<InteractionHand, Matrix4f> CAPTURED_RENDER_POSE = new EnumMap<>(InteractionHand.class);
    private static final Set<InteractionHand> HAS_CAPTURED_RENDER_POSE = EnumSet.noneOf(InteractionHand.class);

    static {
        for (InteractionHand hand : InteractionHand.values()) {
            MUZZLE_EMITTER_LOCATORS.put(hand, new WeakHashMap<>());
        }
    }

    /// > 获取当前物品对应的枪械模型与贴图（不存在时返回 null）。
    @Nullable
    private GunDisplay getDisplay(ItemStack stack) {
        return TimelessHelperC.getGunDisplay(stack).orElse(null);
    }

    /// > 第一人称渲染上下文对应的手：左手 ctx → 副手，其余 → 主手。
    private static InteractionHand handForContext(ItemDisplayContext ctx) {
        return ctx == FIRST_PERSON_LEFT_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    /// > 解析本次渲染应使用的 display。第一人称下取对应手活跃动画实例的「生效 display」
    /// > （双持时为按手单手变体，模型骨架与该实例 pose 一致）；否则回退按物品取的本体 display。
    /// >
    /// > 这保证渲染所用的 [BedrockGunModel] 与动画实例 pose 的目标模型是同一对象，
    /// > 避免双持时主/副手套用了错误骨架（双手模型 vs 单手模型）。
    @Nullable
    private GunDisplay resolveRenderDisplay(ItemStack stack, ItemDisplayContext ctx) {
        if (ctx.firstPerson()) {
            var instance = FirstPersonRenderHandler.getActiveAnimationInstance(handForContext(ctx));
            if (instance instanceof GunAnimationInstance gunAni) {
                return gunAni.effectiveDisplay();
            }
        }
        return getDisplay(stack);
    }

    /* FIXME rendering
    @Override
    public boolean hasModel(ItemStack stack) {
        GunDisplay display = getDisplay(stack);
        return display != null && display.getGunModel() != null && display.getModelTexture() != null;
    }
    */

    /* FIXME rendering
    @Nullable
    @Override
    public Identifier getSlotTexture(ItemStack stack) {
        return TimelessHelperC.getGunDisplay(stack)
                .map(GunDisplay::getSlotTexture)
                .orElse(null);
    }
    */

    /* FIXME rendering
    /// > 与默认逻辑基本相同，额外加上了开镜放大视野后的削弱，避免放大视野后抖动太大影响游戏体验。
    @Override
    public void applyLevelCameraAnimation(ViewportEvent.ComputeCameraAngles event, ItemStack stack, Quaternionf animateRot, float partialTicks) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        Quaternionfc raw = new YXZRotationView(new Vector3f(
                (float) Math.toRadians(event.getPitch()),
                (float) Math.toRadians(event.getYaw()),
                (float) Math.toRadians(event.getRoll())
        )).asQuaternion();

        float multiplier = IGunOperator.asGunHolder(player)
                .map(cap->cap.aimingProgress(partialTicks)).orElse(0f);

        Quaternionf origin = new Quaternionf(raw);
        Quaternionf combined = origin.mul(animateRot.nlerp(MathUtil.IDENTITY_QUATERNION, multiplier * 0.5f));

        YXZRotationView rotationView = new YXZRotationView(combined);
        Vector3fc eulerAngle = rotationView.asEulerAngle();
        event.setYaw((float) Math.toDegrees(eulerAngle.y()));
        event.setPitch((float) Math.toDegrees(eulerAngle.x()));
        event.setRoll((float) -Math.toDegrees(eulerAngle.z()));
    }
    */

    /* FIXME rendering
    /// > 与默认逻辑基本相同，额外加上了开镜放大视野后的削弱，避免放大视野后抖动太大影响游戏体验。
    @Override
    public void applyItemInHandCameraAnimation(PoseStack poseStack, ItemStack stack, Quaternionf animateRot, float partialTicks) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        float multiplier = IGunOperator.asGunHolder(player)
                .map(cap->cap.aimingProgress(partialTicks)).orElse(0f);
        Quaternionf quaternion = animateRot.nlerp(MathUtil.IDENTITY_QUATERNION, multiplier * 0.5f);

        poseStack.mulPose(quaternion);
    }
    */

    /* FIXME rendering
    @Override
    protected void beforeRender(PoseStack poseStack, ItemDisplayContext ctx, ItemStack stack, float partialTicks) {
        super.beforeRender(poseStack, ctx, stack, partialTicks);

        GunDisplay displayInstance = resolveRenderDisplay(stack, ctx);
        if (displayInstance == null) {
            return;
        }
        BedrockGunModel model = displayInstance.getGunModel();
        if (model == null) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (ctx.firstPerson()) {
            prepareFirstPerson(poseStack, model, stack, partialTicks, player, ctx);
        } else {
            /// > 第三人称下不需要渲染手
            model.setRenderHand(false);
        }

        IGun iGun = IGun.asGun(stack);
        if (iGun == null) {
            return;
        }

        Map<Identifier, AttachmentSlot> slots = iGun.getAttachmentSlots(stack);
        for (var entry : slots.entrySet()) {
            var slotDisplay = displayInstance.getSlot(entry.getKey());
            if (slotDisplay == null) {
                continue;
            }

            var attachment = iGun.getAttachment(stack, entry.getValue());
            var iAttachment = IAttachmentLike.asAttachmentLike(attachment);
            boolean builtin = IAttachment.isBuiltinAttachment(attachment);

            String defaultModelGroup = slotDisplay.defaultModelGroup();
            if (defaultModelGroup != null) {
                BoneState bone = model.getBone(defaultModelGroup);
                if (bone != null) {
                    /// > 槽位空置、或装的是原厂配件（几何寄生于此）时，保持宿主自带几何可见；
                    /// > 装独立配件时隐藏。
                    bone.visible = (iAttachment == null) || builtin;
                }
            }

            String adapterModelGroup = slotDisplay.adapter();
            if (adapterModelGroup != null) {
                BoneState bone = model.getBone(adapterModelGroup);
                if (bone != null) {
                    /// > 适配器仅在装独立配件时显示；原厂配件不需要适配器。
                    bone.visible = (iAttachment != null) && !builtin;
                }
            }
        }
    }
    */

    /* FIXME rendering
    /// > 渲染枪械模型（v2 instance），应用动画 pose 并完成几何 + 手臂渲染。
    /// >
    /// > 若装有需要模板缓冲渲染的瞄具，会在枪体之前先渲染瞄具并写入 stencil，
    /// > 随后据此剔除「镜片内」区域的枪体几何（仅长筒镜 / 组合镜需要剔除）。
    @Override
    protected void renderModel(PoseStack poseStack, ItemDisplayContext ctx, ItemStack stack,
                               MultiBufferSource bufferSource, int light, int overlay, float partialTicks) {
        GunDisplay displayInstance = resolveRenderDisplay(stack, ctx);
        if (displayInstance == null) {
            return;
        }
        BedrockGunModel model = displayInstance.getGunModel();
        Identifier texture = displayInstance.getModelTexture();
        if (model == null || texture == null) {
            return;
        }

        RenderType quadType = RenderType.entityCutout(texture);
        RenderType triangleType = BedrockModelRenderTypes.polyMeshCutout(texture);

        /// > 第一人称下处理瞄具的模板缓冲渲染：镜片先于枪体绘制。
        /// > 光影阴影 pass 下跳过 stencil，仅做常规渲染，避免污染阴影贴图。
        stencilHandledSlots.clear();
        boolean canStencil = ctx.firstPerson() && !OculusCompat.isRenderShadow()
                && bufferSource instanceof MultiBufferSource.BufferSource;
        LocalGunOperator.AimViewState aimViewState = resolveAimViewState(ctx);
        List<ScopeStencilRenderer.Scope> scopes = canStencil
                ? collectAllStencilScopes(stack, model, displayInstance, false)
                : List.of();
        boolean gunCulled = false;
        if (!scopes.isEmpty()) {
            MultiBufferSource.BufferSource bs = (MultiBufferSource.BufferSource) bufferSource;
            for (ScopeStencilRenderer.Scope scope : scopes) {
                stencilHandledSlots.add(scope.slotId());
            }
            ScopeStencilRenderer.render(scopes, poseStack, bs, light, partialTicks,
                    AimViewHelper.scopeWindow(AimViewHelper.collect(stack, displayInstance),
                            aimViewState.activeIndex(), aimViewState.pendingIndex(), aimViewState.progress()));

            /// > 仅筒状放大瞄具需要剔除镜内枪体（红点 / 全息不剔除，可透过瞄具看到枪身）。
            /// > 统一剔除规则：模板值落在 scope 圆内值域（(v & 0xC0) == 0xC0）不绘制枪体，其余区域正常绘制。
            if (scopes.stream().anyMatch(scope -> scope.model().isScope())) {
                RenderHelper.enableItemEntityStencilTest();
                RenderSystem.stencilFunc(GL11.GL_NOTEQUAL, BedrockAttachmentModel.SCOPE_VIEW_CULL_REF, BedrockAttachmentModel.SCOPE_VIEW_CULL_MASK);
                RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
                gunCulled = true;
            }
        }

        model.renderToBuffer(poseStack, bufferSource, quadType, triangleType, light, overlay);

        if (gunCulled) {
            /// > 枪体几何必须在 stencil 测试仍生效时立即绘制，故强制 flush
            /// > （光影下普通 endBatch 不会立即提交，需走 OculusCompat 的全量 flush）
            MultiBufferSource.BufferSource bs = (MultiBufferSource.BufferSource) bufferSource;
            if (!OculusCompat.endBatch(bs)) {
                bs.endBatch(quadType);
                bs.endBatch(triangleType);
            }
            /// > 保持 stencil 测试开启，使随后在 afterRender 中渲染的其它配件
            /// > 同样被「镜内剔除」；关闭与清空延后到 finishStencilCulling()。
            gunStencilCulling = true;
        }
    }
    */

    /* FIXME rendering
    /// > 结束镜内剔除：在配件渲染完成后强制 flush 并关闭 / 清空 stencil。
    /// > 与 [#renderModel] 中开启的 stencil 测试配对，调用方需保证 stencil 测试仍生效。
    private void finishStencilCulling(MultiBufferSource bufferSource) {
        if (!gunStencilCulling) {
            return;
        }
        gunStencilCulling = false;
        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            if (!OculusCompat.endBatch(bs)) {
                bs.endBatch();
            }
        }
        RenderHelper.disableItemEntityStencilTest();
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
    }
    */

    /* FIXME rendering
    /// > 收集当前装在枪上、需要模板缓冲渲染的瞄具（长筒镜 / 红点 / 组合镜）。
    /// > 顺序 = 槽位声明顺序（LinkedHashMap）。收集直接安装在枪械槽位上的全部匹配瞄具
    /// > （不含套件子配件），全部通过统一的单 stencil 会话编排渲染：sight 的分划板 / 镜面内容
    /// > 恒常驻（无开镜之分）；完整的模板圆仅作用于当前激活（主视角）的 scope，其余 scope 视为
    /// > 非瞄准状态（见 [ScopeStencilRenderer]）。
    /// >
    /// > @param bindMounts 瞄准定位使用静态挂点，避免视点取逆与动画约束重复抵消枪体动画；实体渲染使用动态挂点。
    private List<ScopeStencilRenderer.Scope> collectAllStencilScopes(ItemStack stack, BedrockGunModel model,
                                                     GunDisplay displayInstance, boolean bindMounts) {
        IGun iGun = IGun.asGun(stack);
        if (iGun == null) {
            return List.of();
        }
        List<ScopeStencilRenderer.Scope> result = new ArrayList<>();
        Map<Identifier, AttachmentSlot> slots = iGun.getAttachmentSlots(stack);
        for (SightScopeHelper.InstalledSight sight : SightScopeHelper.collectInstalledSights(stack)) {
            AttachmentSlot slot = slots.get(sight.slotId());
            if (slot == null) {
                continue;
            }
            ItemStack attachmentStack = sight.stack();
            var slotDisplay = displayInstance.getSlot(sight.slotId());
            if (slotDisplay == null || slotDisplay.posGroup() == null) {
                continue;
            }
            var renderData = resolveAttachmentRender(attachmentStack);
            if (renderData == null) {
                continue;
            }
            BedrockAttachmentModel attachmentModel = renderData.model();
            if (attachmentModel == null || !attachmentModel.needsStencil()) {
                continue;
            }
            Matrix4f slotTransform = bindMounts
                    ? model.getBindGlobalTransform(slotDisplay.posGroup())
                    : model.getGlobalTransform(slotDisplay.posGroup());
            if (slotTransform == null) {
                continue;
            }
            /// > 导轨微调：瞄具走 stencil 路径，同样需要叠加偏移
            slotTransform = AttachmentMountHelper.applyRailOffset(slotTransform, slot, attachmentStack);
            /// > 仅唯一匹配当前宿主与槽位的适配器可抬高瞄具落点
            SlotAdapterDisplay adapterDisplay = resolveValidSlotAdapterDisplay(stack, sight.slotId());
            if (adapterDisplay != null) {
                slotTransform = applyMountOffset(slotTransform, adapterDisplay.getMountOffset());
            }
            result.add(new ScopeStencilRenderer.Scope(attachmentModel, renderData.texture(), slotTransform,
                    sight.slotId(), renderData.mountOffset()));
        }
        return result;
    }
    */

    /// > 从当前玩家对应手位的会话读取瞄准视图位切换状态；无会话 / 非本机玩家时返回稳态默认。
    private static LocalGunOperator.AimViewState resolveAimViewState(ItemDisplayContext ctx) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return LocalGunOperator.AimViewState.NONE;
        }
        InteractionHand hand = handForContext(ctx);
        return IGunOperator.asGunHolder(player)
                .filter(op -> op instanceof LocalGunOperator)
                .map(op -> ((LocalGunOperator) op).aimViewState(hand))
                .orElse(LocalGunOperator.AimViewState.NONE);
    }

    /* FIXME rendering
    /// > 按激活瞄具（含切换过渡插值）计算瞄准视点矩阵：静态瞄具挂点变换 × 附件模型 scope_view 骨骼。
    /// > 缺失 scope_view 或矩阵不可用时返回 null（调用方回退枪模型 iron_view）。
    @Nullable
    private static Matrix4f scopeViewOf(ScopeStencilRenderer.Scope scope) {
        Matrix4f view = scope.model().getGlobalTransform("scope_view");
        if (view == null) {
            return null;
        }
        Matrix4f result = new Matrix4f(scope.slotTransform());
        Vector3f offset = scope.viewOffset();
        if (offset != null && (offset.x() != 0f || offset.y() != 0f || offset.z() != 0f)) {
            result.translate(offset);
        }
        result.mul(view);
        float determinant = result.determinant();
        return result.isFinite() && Float.isFinite(determinant) && Math.abs(determinant) > 1.0e-6f
                ? result : null;
    }
    */

    /// > 配件的有效渲染数据：考虑配件变体（replace 模式换整模型）后的最终模型与贴图，
    /// > 以及变体声明的 overlay 叠加模型 / 下游挂点偏移。
    /// >
    /// > @param model         用于渲染本配件几何的模型（replace 变体则为变体模型）\
    /// > @param texture       对应贴图\
    /// > @param baseDisplay   基础配件 display（子槽位 / scope_view 等结构以此为准）\
    /// > @param overlayModel  变体 overlay 模式下要额外叠加渲染的模型；无则 null\
    /// > @param overlayTexture overlay 模型贴图；无则 null\
    /// > @param overlayBone   overlay 模型挂载到 model 的骨骼名；无则 null\
    /// > @param mountOffset   变体对下游（子挂点 / 瞄准视点）施加的额外平移；无则 null
    private record AttachmentRenderData(BedrockAttachmentModel model, Identifier texture,
                                        AttachmentDisplay baseDisplay,
                                        @Nullable BedrockAttachmentModel overlayModel,
                                        @Nullable Identifier overlayTexture,
                                        @Nullable String overlayBone,
                                        @Nullable Vector3f mountOffset) {
    }

    /// > 解析配件的有效渲染数据（含形态处理：关态几何 / 用户变体）。基础 display 缺失返回 null。
    @Nullable
    private static AttachmentRenderData resolveAttachmentRender(ItemStack attachmentStack) {
        AttachmentDisplay baseDisplay = TimelessHelperC.getAttachmentDisplay(attachmentStack).orElse(null);
        if (baseDisplay == null || baseDisplay.getModel() == null) {
            return null;
        }
        /// > 当前生效形态的几何：关态 off_display 优先，其次用户变体，最后基础 display
        AttachmentDisplay active = TimelessHelperC.getActiveDisplay(attachmentStack).orElse(null);
        if (active == null || active.getModel() == null) {
            return new AttachmentRenderData(baseDisplay.getModel(), baseDisplay.getModelTexture(), baseDisplay,
                    null, null, null, null);
        }
        if (active.isOverlayVariant()) {
            /// > overlay：基础模型不变，额外叠加变体模型
            return new AttachmentRenderData(baseDisplay.getModel(), baseDisplay.getModelTexture(), baseDisplay,
                    active.getModel(), active.getModelTexture(), active.getVariantAttachBone(),
                    active.getVariantMountOffset());
        }
        /// > replace：用变体模型整体替换基础配件模型几何，但子槽位 / scope_view 结构仍以基础 display 为准
        return new AttachmentRenderData(active.getModel(), active.getModelTexture(), baseDisplay,
                null, null, null, active.getVariantMountOffset());
    }

    /// > 把一个平移向量叠加到挂点变换上（base * translate(offset)）。offset 为空时返回 base。
    private static Matrix4f applyMountOffset(Matrix4f base, @Nullable Vector3f offset) {
        if (offset == null) {
            return base;
        }
        return new Matrix4f(base).translate(offset);
    }

    /* FIXME rendering
    @Override
    protected void afterRender(PoseStack poseStack, ItemDisplayContext ctx, ItemStack stack, MultiBufferSource bufferSource, int light, float partialTicks) {
        super.afterRender(poseStack, ctx, stack, bufferSource, light, partialTicks);
        GunDisplay displayInstance = resolveRenderDisplay(stack, ctx);
        BedrockGunModel model = displayInstance == null ? null : displayInstance.getGunModel();
        if (model == null) {
            return;
        }

        /// > 光影阴影 pass 下 poseStack 为「光源视角」、投影矩阵为阴影正交投影，
        /// > 不能用来覆盖主相机视角缓存。阴影 pass 不清除、不写入缓存。
        boolean shadowPass = OculusCompat.isRenderShadow();
        InteractionHand hand = handForContext(ctx);
        boolean cacheMuzzles = ctx.firstPerson() && !shadowPass;
        if (cacheMuzzles) {
            /// > 本帧开始先清空该手的所有来源，避免切枪/卸下挂后残留上一帧位置。
            FirstPersonPoseCache.clear(hand);
        }

        /// > 我们在此处渲染配件
        frameLaserDraws.clear();
        renderAttachments(stack, model, poseStack, bufferSource, light, hand, cacheMuzzles);
        if (ctx.firstPerson()) {
            spawnAndBindMuzzleParticles(poseStack, model, stack, hand);
            renderParticlesInStencilWindow(hand, bufferSource, light, partialTicks);
            collectFirstPersonLaserViews(poseStack, model, stack, displayInstance, hand, frameLaserDraws);
            collectFirstPersonLaserBeams(poseStack, model, displayInstance, hand, false, frameLaserDraws);
            for (LaserDrawEntry draw : frameLaserDraws) {
                draw.renderBeamPart(bufferSource);
            }
        }
        /// > 配件渲染完成后再结束镜内剔除（关闭并清空 stencil）
        finishStencilCulling(bufferSource);

        /// > 第一人称时缓存枪口骨骼偏移，供曳光弹 / 镭射 / 手电使用
        if (ctx.firstPerson()) {
            if (cacheMuzzles) {
                cacheMuzzleOffsets(poseStack, model, displayInstance, hand, MuzzleReference.primary());
                collectFirstPersonFlashlightViews(poseStack, model, stack, displayInstance);
            }
            /// > 激光落点光斑（模板窗口外：镜内亦可见远处命中点）
            for (LaserDrawEntry draw : frameLaserDraws) {
                draw.renderDotPart(bufferSource);
            }
            /// > 抛壳：消费动画 shell_eject 事件生成弹壳，并渲染存活弹壳。
            spawnAndRenderShells(poseStack, model, stack, displayInstance, bufferSource, light, hand);
            /// > 挂饰：刚性贴在锚点骨骼上渲染（无物理，随动画跟随）。
            CharmRenderer.render(stack, model, poseStack, bufferSource, light);
        } else if (!shadowPass) {
            FirstPersonPoseCache.clear(hand);
        }

        /// > 改装态：把各槽位挂点投影到屏幕
        if (!shadowPass) {
            if (ctx.firstPerson() && RefitTransform.isActive()) {
                RefitScreenCacheCollector.cacheSlotPositions(poseStack, model, stack, displayInstance);
            } else {
                RefitSlotScreenCache.clear();
            }
        }

        /// > 挂饰拾取态：把枪体各 cube 投影到屏幕
        if (!shadowPass) {
            if (ctx.firstPerson() && RefitTransform.isActive() && CharmPickSession.isActive()) {
                RefitScreenCacheCollector.cacheCharmCubePositions(poseStack, model);
            } else {
                CharmCubeScreenCache.clear();
            }
        }

        /// > 清理本帧姿态，回到 bind 状态，避免可见性/位移残留
        model.resetPose();
    }
    */

    /* FIXME rendering
    private void prepareFirstPerson(PoseStack poseStack, BedrockGunModel model, ItemStack stack, float partialTicks, LocalPlayer player, ItemDisplayContext context) {
        /// > 按照玩家喵准进度，反转 mc 自带的转视角迟滞动画。
        if (player != null) {
            IGunOperator.asGunHolder(player).ifPresent(cap -> {
                InteractionHand hand = handForContext(context);
                var instance = FirstPersonRenderHandler.getActiveAnimationInstance(hand);
                if (instance instanceof GunAnimationInstance ani) {
                    Pose pose = ani.getCachedPose();
                    model.applyPose(pose);
                    BoneState cameraBone = model.getBone("camera");
                    if (cameraBone != null) {
                        ani.setCameraRotation(new Quaternionf(cameraBone.rotation));
                    }

                    float aimingProgress = cap.aimingProgress(partialTicks);
                    long shootTimeStamp = cap.getLastFireTime();
                    float xRotOffset = Mth.lerp(partialTicks, player.xBobO, player.xBob);
                    float yRotOffset = Mth.lerp(partialTicks, player.yBobO, player.yBob);
                    float xRot = player.getViewXRot(partialTicks) - xRotOffset;
                    float yRot = player.getViewYRot(partialTicks) - yRotOffset;
                    poseStack.mulPose(Axis.XP.rotationDegrees(xRot * -0.075F * aimingProgress));
                    poseStack.mulPose(Axis.YP.rotationDegrees(yRot * -0.075F * aimingProgress));

                    ShootDynamicsUtil.applyShootSwayAndRotation(model, aimingProgress, shootTimeStamp);
                    JumpDynamicsUtil.applyJumpingSway(model, aimingProgress, partialTicks);
                    LocalGunOperator.AimViewState aimViewState = cap instanceof LocalGunOperator localOp
                            ? localOp.aimViewState(hand)
                            : LocalGunOperator.AimViewState.NONE;
                    if (aimViewState == null) {
                        aimViewState = LocalGunOperator.AimViewState.NONE;
                    }
                    applyFirstPersonPositioningTransform(poseStack, model, stack, aimingProgress, resolveRenderDisplay(stack, context), aimViewState);
                    GunAnimationConstraintHelper.apply(poseStack, model, aimingProgress, ani.getCachedAnimationPose(), hand);
                }
            });
        }
        /// > 改装界面下隐藏手臂，否则正常渲染
        model.setRenderHand(!RefitTransform.isActive());
    }
    */

    /* FIXME rendering
    private void cacheMuzzleOffsets(PoseStack poseStack, BedrockGunModel model, GunDisplay display,
                                    InteractionHand hand, MuzzleReference source) {
        for (int muzzleIndex = 0; muzzleIndex < display.getMuzzleBones().size(); muzzleIndex++) {
            Matrix4f muzzleTransform = model.getGlobalTransform(display.getMuzzleBone(muzzleIndex));
            if (muzzleTransform == null) {
                continue;
            }
            Matrix4f muzzleViewTransform = FirstPersonSpaceHelper.toViewTransform(poseStack.last().pose(), muzzleTransform);
            FirstPersonSpaceHelper.ViewPose viewPose = FirstPersonSpaceHelper.resolveWorldFovViewPose(muzzleViewTransform);
            FirstPersonPoseCache.setMuzzle(hand,
                    new MuzzleReference(source.underBarrel(), muzzleIndex), viewPose.position(), viewPose.forward());
        }
    }
    */

    /* FIXME rendering
    /// > 收集第一人称 active 枪灯（受配件/枪开关状态门控），写入 [FirstPersonPoseCache]
    /// > 供 AFTER_LEVEL 阶段的手电后处理使用。优先枪自身配置，其次配件树中第一个 active 枪灯；
    /// > 屏幕后处理单灯合成，多灯只取第一个。
    private void collectFirstPersonFlashlightViews(PoseStack poseStack, BedrockGunModel model, ItemStack gunStack,
                                                   GunDisplay displayInstance) {
        FirstPersonPoseCache.clearFlashlight();
        IGun iGun = IGun.asGun(gunStack);
        if (iGun == null || displayInstance == null) {
            return;
        }
        /// > 枪自身灯源（受枪 NBT 效果开关门控，缺省开启）
        GunDisplay.Flashlight gunLight = displayInstance.getFlashlight();
        if (gunLight != null && ToggleHelper.getGunEffectEnabled(gunStack, "flashlight")) {
            Matrix4f boneTransform = model.getGlobalTransform(gunLight.boneName());
            if (boneTransform != null) {
                writeFlashlightCache(FirstPersonSpaceHelper.toViewTransform(poseStack.last().pose(), boneTransform), gunLight);
                return;
            }
        }
        /// > 配件树递归找第一个 active 枪灯
        for (var entry : iGun.getAttachmentSlots(gunStack).entrySet()) {
            AttachmentSlot slot = entry.getValue();
            ItemStack attachment = iGun.getAttachment(gunStack, slot);
            if (attachment.isEmpty()) {
                continue;
            }
            AttachmentSlotDisplayCfg slotDisplay =displayInstance.getSlot(slot.id());
            if (slotDisplay == null || slotDisplay.posGroup() == null) {
                continue;
            }
            Matrix4f mount = model.getGlobalTransform(slotDisplay.posGroup());
            if (mount == null) {
                continue;
            }
            mount = AttachmentMountHelper.applyRailOffset(mount, slot, attachment);
            if (collectFlashlightNode(poseStack.last().pose(), mount, attachment)) {
                return;
            }
        }
    }
    */

    /// > 递归收集单个配件节点及其子配件中的枪灯源（与镭射收集同一挂点链）。
    /// >
    /// > @return 是否找到并写入 active 枪灯（短路停止）
    private boolean collectFlashlightNode(Matrix4f viewBase, Matrix4f mountTransform, ItemStack attachment) {
        AttachmentDisplay baseDisplay = TimelessHelperC.getAttachmentDisplay(attachment).orElse(null);
        if (baseDisplay == null) {
            return false;
        }
        AttachmentDisplay active = TimelessHelperC.getActiveDisplay(attachment).orElse(null);
        AttachmentDisplay effectDisplay = active != null ? active : baseDisplay;
        GunDisplay.Flashlight light = effectDisplay.getFlashlight();
        if (light != null && effectDisplay.getModel() != null && ToggleHelper.isEnabled(attachment)) {
            Matrix4f local = effectDisplay.getModel().getGlobalTransform(light.boneName());
            if (local != null) {
                writeFlashlightCache(FirstPersonSpaceHelper.toViewTransform(viewBase, mountTransform, local), light);
                return true;
            }
        }
        if (!(attachment.getItem() instanceof IAttachmentHolder holder)) {
            return false;
        }
        for (var slotEntry : holder.getAttachmentSlots(attachment).entrySet()) {
            AttachmentSlot subSlot = slotEntry.getValue();
            ItemStack subAttachment = holder.getAttachment(attachment, subSlot);
            if (subAttachment.isEmpty()) {
                continue;
            }
            AttachmentSlotDisplayCfg slotDisplay = baseDisplay.getSlot(subSlot.id());
            if (slotDisplay == null || slotDisplay.posGroup() == null) {
                continue;
            }
            Matrix4f slotTransform = effectDisplay.getModel().getGlobalTransform(slotDisplay.posGroup());
            if (slotTransform == null) {
                continue;
            }
            slotTransform = applyMountOffset(slotTransform, effectDisplay.getVariantMountOffset());
            slotTransform = AttachmentMountHelper.applyRailOffset(slotTransform, subSlot, subAttachment);
            if (collectFlashlightNode(viewBase, new Matrix4f(mountTransform).mul(slotTransform), subAttachment)) {
                return true;
            }
        }
        return false;
    }

    /// > 把枪灯骨骼的视图空间变换写入缓存（含第一人称 FOV 修正，与枪口缓存同源）。
    private static void writeFlashlightCache(Matrix4f viewTransform, GunDisplay.Flashlight light) {
        FirstPersonSpaceHelper.ViewPose viewPose = FirstPersonSpaceHelper.resolveWorldFovViewPose(viewTransform);
        FirstPersonPoseCache.setFlashlight(viewPose.position(), viewPose.forward(), light.color(), light.range());
    }

    /* FIXME rendering
    /// > 收集第一人称镭射指示器（受配件/枪开关状态门控）的视图几何。
    /// > 能量束（开火激光）由 [#collectFirstPersonLaserBeams] 单独收集，不受镭射开关门控。
    /// >
    /// > 镭射 provider 变换链：
    /// > - 配件镭射：`poseStack × gunSlotMount × attachmentLaserBone`
    /// > - 枪自身镭射：`poseStack × gunLaserBone`
    /// >
    /// > 这样镭射与当前帧 bob/sway/动画完全共用同一套矩阵，不再经过 LevelStage 缓存链路。
    private void collectFirstPersonLaserViews(PoseStack poseStack, BedrockGunModel model, ItemStack gunStack,
                                              GunDisplay displayInstance, InteractionHand hand,
                                              List<LaserDrawEntry> out) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || displayInstance == null) {
            return;
        }
        IGun iGun = IGun.asGun(gunStack);
        if (iGun == null) {
            return;
        }

        /// > 递归整棵配件树收集镭射源：当前生效形态含 laser 配置且开关开启的配件。
        /// > 不按附件 type 判定（效果由形态 toggle 或生效 display 配置表达）；
        /// > 未声明 toggle 的旧镭射配件恒视为开启（ToggleHelper 兜底）。
        for (var entry : iGun.getAttachmentSlots(gunStack).entrySet()) {
            AttachmentSlot slot = entry.getValue();
            ItemStack attachment = iGun.getAttachment(gunStack, slot);
            if (attachment.isEmpty()) {
                continue;
            }
            AttachmentSlotDisplayCfg slotDisplay =displayInstance.getSlot(slot.id());
            if (slotDisplay == null || slotDisplay.posGroup() == null) {
                continue;
            }
            Matrix4f mount = model.getGlobalTransform(slotDisplay.posGroup());
            if (mount == null) {
                continue;
            }
            mount = AttachmentMountHelper.applyRailOffset(mount, slot, attachment);
            collectLaserNode(poseStack.last().pose(), mount, attachment, player, out,
                    IAttachmentHolder.MAX_ATTACHMENT_DEPTH);
        }

        /// > 枪自身也可以作为 laser provider（受枪 NBT 效果开关门控，缺省开启）。
        GunDisplay.Laser gunLaser = displayInstance.getLaser();
        if (gunLaser != null && ToggleHelper.getGunEffectEnabled(gunStack, "laser")) {
            Matrix4f boneTransform = model.getGlobalTransform(gunLaser.boneName());
            if (boneTransform != null) {
                Matrix4f providerTransform = FirstPersonSpaceHelper.toViewTransform(poseStack.last().pose(), boneTransform);
                collectLaserProvider(providerTransform, gunLaser, player, out);
            }
        }
    }

    /// > 递归收集单个配件节点及其子配件中的镭射源（与 [#renderAttachmentNode] 的挂点链一致）。
    /// >
    /// > @param viewBase       枪模型视图变换（poseStack.last().pose()）\
    /// > @param mountTransform 该节点在其宿主坐标系下的挂点变换（含导轨偏移/变体抬高）\
    /// > @param attachment     当前配件物品堆\
    /// > @param remaining      剩余可递归深度，兜底防止异常数据导致无限递归
    private void collectLaserNode(Matrix4f viewBase, Matrix4f mountTransform, ItemStack attachment, LocalPlayer player,
                                  List<LaserDrawEntry> out, int remaining) {
        AttachmentDisplay baseDisplay = TimelessHelperC.getAttachmentDisplay(attachment).orElse(null);
        if (baseDisplay == null) {
            return;
        }
        /// > 当前生效形态的几何：关态 off_display 优先，其次用户变体，最后基础 display
        AttachmentDisplay active = TimelessHelperC.getActiveDisplay(attachment).orElse(null);
        AttachmentDisplay effectDisplay = active != null ? active : baseDisplay;
        GunDisplay.Laser laser = effectDisplay.getLaser();
        if (laser != null && effectDisplay.getModel() != null && ToggleHelper.isEnabled(attachment)) {
            Matrix4f local = effectDisplay.getModel().getGlobalTransform(laser.boneName());
            if (local != null) {
                Matrix4f providerTransform = FirstPersonSpaceHelper.toViewTransform(viewBase, mountTransform, local);
                collectLaserProvider(providerTransform, laser, player, out);
            }
        }

        if (remaining <= 0 || !(attachment.getItem() instanceof IAttachmentHolder holder)) {
            return;
        }
        Map<Identifier, AttachmentSlot> subSlots = holder.getAttachmentSlots(attachment);
        for (var slotEntry : subSlots.entrySet()) {
            AttachmentSlot subSlot = slotEntry.getValue();
            ItemStack subAttachment = holder.getAttachment(attachment, subSlot);
            if (subAttachment.isEmpty()) {
                continue;
            }
            /// > 子槽位结构以基础 display 为准（与渲染一致）；几何骨骼取自当前生效模型
            AttachmentSlotDisplayCfg slotDisplay =baseDisplay.getSlot(subSlot.id());
            if (slotDisplay == null || slotDisplay.posGroup() == null) {
                continue;
            }
            Matrix4f slotTransform = effectDisplay.getModel().getGlobalTransform(slotDisplay.posGroup());
            if (slotTransform == null) {
                continue;
            }
            slotTransform = applyMountOffset(slotTransform, effectDisplay.getVariantMountOffset());
            slotTransform = AttachmentMountHelper.applyRailOffset(slotTransform, subSlot, subAttachment);
            collectLaserNode(viewBase, new Matrix4f(mountTransform).mul(slotTransform), subAttachment, player, out,
                    remaining - 1);
        }
    }
    */

    /* FIXME rendering
    /// > 按枪口循环收集第一人称能量束视图（主枪 / 下挂由 `underBarrel` 区分来源）。
    private void collectFirstPersonLaserBeams(PoseStack poseStack, BedrockGunModel model, GunDisplay display,
                                              InteractionHand hand, boolean underBarrel, List<LaserDrawEntry> out) {
        for (int muzzleIndex = 0; muzzleIndex < display.getMuzzleBones().size(); muzzleIndex++) {
            collectFirstPersonLaserBeam(poseStack, model, display, hand,
                    new MuzzleReference(underBarrel, muzzleIndex), out);
        }
    }

    private void collectLaserProvider(Matrix4f providerViewTransform, GunDisplay.Laser laser,
                                      LocalPlayer player, List<LaserDrawEntry> out) {
        Minecraft mc = Minecraft.getInstance();
        float range = laser.length() > 0.0f ? laser.length() : 64.0f;
        float beamAlpha = LaserHandler.computeBeamAlpha(player, mc.getFrameTime());
        LaserRenderer.FirstPersonLaserView view = LaserRenderer.computeFirstPerson(
                providerViewTransform, mc.gameRenderer.getMainCamera(), player, range, beamAlpha);
        if (view != null) {
            out.add(new LaserDrawEntry(view, null, laser.color(), 0, 0, 0));
        }
    }
    */

    /* FIXME rendering
    /// > 收集第一人称能量束视图：本机玩家持枪时若该手有活跃束流，用当前帧枪口骨骼视图变换取起点、
    /// > 服务端权威命中点为终点（与枪体 bob/sway/动画同步）。起点骨骼取 `muzzle_flash`
    /// > （与曳光弹/枪口粒子同源）。按当前渲染手取对应束流，双持时主副手各画各的；
    /// > 其他玩家/第三人称由 `LaserBeamHandler` 走世界空间路径。
    private void collectFirstPersonLaserBeam(PoseStack poseStack, BedrockGunModel model, GunDisplay display,
                                             InteractionHand hand, MuzzleReference source, List<LaserDrawEntry> out) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        LaserBeamManager.Entry pulse = LaserBeamManager.getPulse(player.getId(), hand, source);
        if (pulse == null) {
            return;
        }
        Matrix4f muzzleTransform = model.getGlobalTransform(display.getMuzzleBone(source.muzzleIndex()));
        if (muzzleTransform == null) {
            return;
        }
        Matrix4f providerViewTransform = FirstPersonSpaceHelper.toViewTransform(poseStack.last().pose(), muzzleTransform);
        LaserVisualResolver.LaserVisual visual = LaserVisualResolver.resolve(
                player, hand, source, pulse.startTimeMs(), pulse.color());
        LaserRenderer.FirstPersonBeamView view = LaserRenderer.computeFirstPersonBeam(
                Minecraft.getInstance().gameRenderer.getMainCamera(), providerViewTransform, pulse.hitPoint(),
                pulse.hitType());
        if (view != null) {
            out.add(new LaserDrawEntry(null, view, visual.color(),
                    visual.coreHalfWidth(), visual.glowHalfWidth(), visual.roll()));
        }
    }
    */

    /* FIXME rendering
    /// > 第一人称激光绘制条目：视图几何已计算，光束（模板窗口内）与光斑（窗口外）分开绘制。
    private record LaserDrawEntry(@Nullable LaserRenderer.FirstPersonLaserView laserView,
                                  @Nullable LaserRenderer.FirstPersonBeamView beamView,
                                  float[] color, float coreHalfWidth, float glowHalfWidth, float rollDegrees) {
        void renderBeamPart(MultiBufferSource bufferSource) {
            if (laserView != null) {
                LaserRenderer.renderFirstPersonBeamPart(bufferSource, laserView, color);
            }
            if (beamView != null) {
                LaserRenderer.renderFirstPersonBeamPart(bufferSource, beamView, color, coreHalfWidth, glowHalfWidth, rollDegrees);
            }
        }

        void renderDotPart(MultiBufferSource bufferSource) {
            if (laserView != null) {
                LaserRenderer.renderFirstPersonDotPart(bufferSource, laserView, color);
            }
            if (beamView != null) {
                LaserRenderer.renderFirstPersonDotPart(bufferSource, beamView, color);
            }
        }
    }
    */

    /* FIXME rendering
    /// > 由 `FirstPersonRenderHandler` 在渲染物品前调用。此时 poseStack 尚未叠加
    /// > 第一人称定位变换，仅作为「渲染粒子时的基准坐标系」捕获下来（粒子渲染也使用同一基准）。
    /// >
    /// > 真正的 emitter 创建与枪口绑定延迟到 [#afterRender] 完成——那里 poseStack 已含完整
    /// > 定位变换，可取到枪口骨骼的真实视图矩阵。
    @Override
    public void updateParticleEmitterTransforms(FirstPersonParticleSystem system, PoseStack poseStack, InteractionHand hand) {
        CAPTURED_RENDER_POSE.computeIfAbsent(hand, h -> new Matrix4f()).set(poseStack.last().pose());
        HAS_CAPTURED_RENDER_POSE.add(hand);
    }
    */

    /// > 粒子在枪体模型的模板窗口内渲染（模板测试仍开启）：让镜内（镜头圆切区域）的火光/烟雾
    /// > 被瞄具模板剔除，不穿透镜片画面。返回 true 后 SBM 事件末尾不再自动渲染该手粒子，避免双重绘制。
    @Override
    public boolean renderParticlesInModelWindow() {
        return true;
    }

    /* FIXME rendering
    /// > 立即渲染指定手的活跃粒子，模板测试仍生效（调用方必须保证处于 stencil 开启区间）。
    /// > 渲染基准取 [#updateParticleEmitterTransforms] 捕获的 base pose——emitterTransform
    /// > 是相对该基准的变换，直接使用已叠加定位的 poseStack 会双重变换导致粒子错位。
    private void renderParticlesInStencilWindow(InteractionHand hand, MultiBufferSource bufferSource,
                                                int light, float partialTicks) {
        Matrix4f base = CAPTURED_RENDER_POSE.get(hand);
        if (base == null) {
            return;
        }
        PoseStack basePose = new PoseStack();
        basePose.last().pose().set(base);
        FirstPersonRenderHandler.renderParticlesNow(hand, basePose, bufferSource, light, partialTicks);
    }
    */

    /* FIXME rendering
    /// > 生成并绑定动画 `particle_effects` 关键帧触发的枪口粒子（火光/枪烟）。
    /// >
    /// > 坐标空间换算：渲染粒子时 `BillboardHelper` 用 `基准pose × emitterTransform`，
    /// > 而枪口在视图中的真实矩阵为 `poseStack(已定位) × 骨骼矩阵`。令二者相等可得
    /// > `emitterTransform = 基准pose⁻¹ × poseStack × 骨骼矩阵`；世界对齐变换用于脱离枪口
    /// > 飘散的粒子（如枪烟），为 `相机旋转⁻¹ × poseStack × 骨骼矩阵`。
    private void spawnAndBindMuzzleParticles(PoseStack poseStack, BedrockGunModel model, ItemStack stack, InteractionHand hand) {
        if (!HAS_CAPTURED_RENDER_POSE.contains(hand)) {
            return;
        }
        var instance = FirstPersonRenderHandler.getActiveAnimationInstance(hand);
        if (!(instance instanceof GunAnimationInstance gunAni)) {
            return;
        }
        FirstPersonParticleSystem system = FirstPersonRenderHandler.getParticleSystem();
        GunDisplay display = getDisplay(stack);
        String fallbackBone = "muzzle_flash";
        if (display != null && display.getMuzzleFlash() != null) {
            fallbackBone = display.getMuzzleFlash().boneName();
        }

        /// > 该手专属的 emitter→locator 映射（按手隔离，双持时主副手互不串扰）。
        Map<ParticleEmitterInstance, String> handLocators = MUZZLE_EMITTER_LOCATORS.get(hand);

        /// > 1. 消费本帧动画触发的粒子请求，创建发射器并记录其 locator。
        List<ParticleEmitterInstance> newEmitters = new ArrayList<>();
        for (ParticleEffectData data : gunAni.consumePendingParticles()) {
            ParticleEffectDefinition def = ParticleDefinitionLoader.getInstance().getDefinition(data.effect());
            if (def == null) {
                continue;
            }
            ParticleEmitterInstance emitter = system.addEmitter(def, hand);
            String locator = (data.locator() != null && !data.locator().isEmpty()) ? data.locator() : fallbackBone;
            handLocators.put(emitter, locator);
            newEmitters.add(emitter);
        }

        if (handLocators.isEmpty()) {
            return;
        }

        /// > 2. 预备坐标换算所需矩阵。
        FirstPersonSpaceHelper.ParticleSpace particleSpace = FirstPersonSpaceHelper.createParticleSpace(
                CAPTURED_RENDER_POSE.get(hand), Minecraft.getInstance().gameRenderer.getMainCamera(),
                CameraStateCache.getCameraRollRadians());
        Matrix4f gunViewPose = poseStack.last().pose();

        /// > 3. 为本手所有存活发射器按当前帧枪口骨骼矩阵刷新变换。
        for (var entry : handLocators.entrySet()) {
            ParticleEmitterInstance emitter = entry.getKey();
            Matrix4f boneTransform = model.getGlobalTransform(entry.getValue());
            if (boneTransform == null) {
                continue;
            }
            Matrix4f muzzleView = FirstPersonSpaceHelper.toViewTransform(gunViewPose, boneTransform);
            Matrix4f emitterTransform = particleSpace.toEmitterTransform(muzzleView);
            Matrix4f worldTransform = particleSpace.toWorldTransform(muzzleView);
            emitter.setEmitterTransform(emitterTransform, worldTransform);
        }

        /// > 4. 新发射器立即 tick(0) 触发 instant 发射，使本帧即可见。
        for (ParticleEmitterInstance emitter : newEmitters) {
            emitter.tick(0f);
        }

        /// > 5. 清理已自然消亡的发射器引用。
        handLocators.keySet().removeIf(ParticleEmitterInstance::isFinished);
    }
    */

    /* FIXME rendering
    /// > 消费动画 `shell_eject` 事件生成弹壳，并渲染所有存活弹壳。
    /// >
    /// > 坐标基准：此处 `poseStack` 已含完整第一人称定位变换，`poseStack × 抛壳窗骨骼`即抛壳口
    /// > 在视图空间的真实变换，作为弹壳的冻结起点矩阵交给 [ShellRenderer]。抛壳窗骨骼名约定为
    /// > `shell`（窗 0）/ `shell_N`（窗 N），缺失时回退枪口骨骼。
    private void spawnAndRenderShells(PoseStack poseStack, BedrockGunModel model, ItemStack stack,
                                      GunDisplay display, MultiBufferSource bufferSource, int light, InteractionHand hand) {
        GunDisplay.ShellEjection config = display.getShellEjection();
        ShellRenderer shellRenderer = ShellRenderer.getInstance();
        if (config == null) {
            shellRenderer.clear();
            return;
        }

        /// > 解析当前弹药的弹壳模型/贴图。
        Identifier ammoId = AmmoDataHelper.getAmmoId(stack);
        AmmoDisplay ammoDisplay = ammoId == null ? null : TimelessHelperC.getAmmoDisplay(ammoId).orElse(null);
        BedrockShellModel shellModel = ammoDisplay == null ? null : ammoDisplay.getShellModel();
        Identifier shellTexture = ammoDisplay == null ? null : ammoDisplay.getShellTexture();

        /// > 消费本帧抛壳事件，按抛壳窗骨骼冻结视图矩阵生成弹壳。
        var instance = FirstPersonRenderHandler.getActiveAnimationInstance(hand);
        if (instance instanceof GunAnimationInstance gunAni && shellModel != null && shellTexture != null) {
            Matrix4f gunViewPose = poseStack.last().pose();
            for (int window : gunAni.consumePendingShellEjects()) {
                String boneName = window == 0 ? "shell" : "shell_" + window;
                Matrix4f boneTransform = model.getGlobalTransform(boneName);
                if (boneTransform == null) {
                    /// > 回退枪口骨骼
                    String fallback = display.getMuzzleFlash() != null ? display.getMuzzleFlash().boneName() : "muzzle_flash";
                    boneTransform = model.getGlobalTransform(fallback);
                }
                if (boneTransform == null) {
                    continue;
                }
                Matrix4f frozen = FirstPersonSpaceHelper.toViewTransform(gunViewPose, boneTransform);
                shellRenderer.spawn(frozen, config);
            }
        }

        /// > 每帧渲染存活弹壳。
        shellRenderer.render(bufferSource, shellModel, shellTexture, config, light);
    }
    */

    /* FIXME rendering
    public void renderFirstPerson(LocalPlayer player, ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource,
                                  int light, float partialTicks) {
        /// > 取消掉默认的位移，全部由定位组接管
/// >        if (ctx == FIRST_PERSON_LEFT_HAND) {
/// >            return;
/// >        }
        render(stack, ctx, poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY, partialTicks);
    }
    */

    /* FIXME rendering
    /// > 渲染配件（递归渲染整棵配件树）
    public void renderAttachments(ItemStack stack, BedrockGunModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                  int light, InteractionHand hand, boolean cacheMuzzles) {
        IGun iGun = IGun.asGun(stack);
        if (iGun == null) {
            return;
        }
        /// > 遍历全部槽位（含空槽）：空槽也可能装有「适配器」需要渲染（如垫高的空导轨）
        Map<Identifier, AttachmentSlot> allSlots = iGun.getAttachmentSlots(stack);
        GunDisplay displayInstance = TimelessHelperC.getGunDisplay(stack).orElse(null);
        if (displayInstance == null) {
            return;
        }

        Identifier gunDisplayId = iGun.getDisplayId(stack);

        for (var slotEntry : allSlots.entrySet()) {
            AttachmentSlot slot = slotEntry.getValue();
            ItemStack attachment = iGun.getAttachment(stack, slot);
            var slotDisplay = displayInstance.getSlot(slot.id());
            if (slotDisplay == null || slotDisplay.posGroup() == null) {
                continue;
            }
            Matrix4f slotTransform = model.getGlobalTransform(slotDisplay.posGroup());
            if (slotTransform == null) {
                continue;
            }
            slotTransform = AttachmentMountHelper.applyRailOffset(slotTransform, slot, attachment);
            /// > 槽位适配器：渲染 overlay 并把子配件落点抬高（顶层槽位适配器存于枪上）。
            /// > 注意：空槽与走 stencil 路径的瞄具槽，适配器自身几何同样需要在此渲染。
            slotTransform = applySlotAdapter(stack, slot.id(), slotTransform, poseStack, bufferSource, light);

            /// > 空槽：仅适配器需要渲染，无配件本体
            if (attachment.isEmpty()) {
                continue;
            }

            /// > 该槽瞄具已在枪体渲染前完成模板缓冲渲染，跳过配件本体（但适配器已在上面画好）
            if (stencilHandledSlots.contains(slot.id())) {
                continue;
            }

            renderAttachmentNode(stack, gunDisplayId, attachment, slotTransform, poseStack, bufferSource, light,
                    IAttachmentHolder.MAX_ATTACHMENT_DEPTH, model, displayInstance, hand, cacheMuzzles);
        }
    }
    */

    /* FIXME rendering
    /// > 递归渲染单个配件节点及其子配件。
    /// >
    /// > @param mountTransform 该节点在父坐标系下的安装点变换\
    /// > @param remaining      剩余可递归深度，兜底防止异常数据导致无限递归\
    /// > @param gunModel       宿主枪模型（原厂配件的几何/子槽位骨骼寄生于此）\
    /// > @param gunDisplay     宿主枪 display（原厂配件的子槽位 display 复用此处）
    private void renderAttachmentNode(ItemStack gunStack, Identifier gunDisplayId, ItemStack attachmentStack,
                                      Matrix4f mountTransform, PoseStack poseStack, MultiBufferSource bufferSource,
                                      int light, int remaining, BedrockGunModel gunModel, GunDisplay gunDisplay,
                                      InteractionHand hand, boolean cacheMuzzles) {
        if (attachmentStack.isEmpty() || remaining <= 0) {
            return;
        }

        /// > 下挂武器是叶子节点，走专用渲染（带姿态），不递归
        if (attachmentStack.getItem() instanceof IMountedWeapon) {
            renderMountedWeaponAttachment(gunStack, gunDisplayId, poseStack, bufferSource, light,
                    mountTransform, attachmentStack, hand, cacheMuzzles);
            return;
        }

        /// > 原厂配件：几何已由宿主枪模型画出（default_model_group），此处不渲染自身几何，
        /// > 但仍需递归其子配件——子配件的 posGroup/slot display 复用宿主枪模型与 display，
        /// > 相对宿主模型绝对定位（不叠加原厂件的 mountTransform）。
        if (IAttachment.isBuiltinAttachment(attachmentStack)) {
            renderBuiltinChildren(gunStack, gunDisplayId, attachmentStack, poseStack, bufferSource, light,
                    remaining, gunModel, gunDisplay, hand, cacheMuzzles);
            return;
        }

        var renderData = resolveAttachmentRender(attachmentStack);
        if (renderData == null) {
            return;
        }
        BedrockAttachmentModel attachmentModel = renderData.model();
        AttachmentDisplay attachmentDisplay = renderData.baseDisplay();

        poseStack.pushPose();
        PoseStackTransformHelper.mulPoseAndNormal(poseStack, mountTransform);

        /// > 渲染配件本身（replace 变体则为变体模型）
        Identifier texture = renderData.texture();
        RenderType quadType = RenderType.entityCutout(texture);
        RenderType triangleType = BedrockModelRenderTypes.polyMeshCutout(texture);
        attachmentModel.renderToBuffer(poseStack, bufferSource, quadType, triangleType, light, OverlayTexture.NO_OVERLAY);

        /// > overlay 变体：在 attach_bone 处叠加渲染变体模型
        renderVariantOverlay(renderData, attachmentModel, poseStack, bufferSource, light);

        /// > 渲染其子配件（任意带槽位的配件都可作为容器）。遍历全部槽位（含空槽），
        /// > 因为空子槽也可能装有适配器需要渲染。
        var iHolder = attachmentStack.getItem();
        if (iHolder instanceof IAttachmentHolder holder) {
            Map<Identifier, AttachmentSlot> subSlots = holder.getAttachmentSlots(attachmentStack);
            for (var slotEntry : subSlots.entrySet()) {
                AttachmentSlot subSlot = slotEntry.getValue();
                ItemStack subAttachment = holder.getAttachment(attachmentStack, subSlot);
                var slotDisplay = attachmentDisplay.getSlot(subSlot.id());
                if (slotDisplay == null || slotDisplay.posGroup() == null) {
                    continue;
                }
                Matrix4f slotTransform = attachmentModel.getGlobalTransform(slotDisplay.posGroup());
                if (slotTransform == null) {
                    continue;
                }
                /// > 变体可能抬高其上的子配件挂点
                slotTransform = applyMountOffset(slotTransform, renderData.mountOffset());
                slotTransform = AttachmentMountHelper.applyRailOffset(slotTransform, subSlot, subAttachment);
                /// > 槽位适配器：渲染 overlay 并把子配件落点抬高
                slotTransform = applySlotAdapter(attachmentStack, subSlot.id(), slotTransform,
                        poseStack, bufferSource, light);
                if (subAttachment.isEmpty()) {
                    continue;
                }
                renderAttachmentNode(gunStack, gunDisplayId, subAttachment, slotTransform, poseStack, bufferSource, light,
                        remaining - 1, gunModel, gunDisplay, hand, cacheMuzzles);
            }
        }

        poseStack.popPose();
    }
    */

    /* FIXME rendering
    /// > 渲染 overlay 模式变体的叠加模型（在 attach_bone 骨骼变换处）。
    private void renderVariantOverlay(AttachmentRenderData data, BedrockAttachmentModel baseModel,
                                      PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        if (data.overlayModel() == null || data.overlayTexture() == null) {
            return;
        }
        Matrix4f attachTransform = null;
        if (data.overlayBone() != null) {
            attachTransform = baseModel.getGlobalTransform(data.overlayBone());
        }
        poseStack.pushPose();
        if (attachTransform != null) {
            PoseStackTransformHelper.mulPoseAndNormal(poseStack, attachTransform);
        }
        Identifier texture = data.overlayTexture();
        RenderType quadType = RenderType.entityCutout(texture);
        RenderType triangleType = BedrockModelRenderTypes.polyMeshCutout(texture);
        data.overlayModel().renderToBuffer(poseStack, bufferSource, quadType, triangleType, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
    */

    /// > 解析当前槽位唯一有效的适配器 display。NBT 中的 index 已丢失、宿主不匹配或 target 重复时失败关闭。
    @Nullable
    private static SlotAdapterDisplay resolveValidSlotAdapterDisplay(ItemStack holderStack,
                                                                     Identifier slotId) {
        Identifier adapterId = SlotAdapterHelper.getAdapter(holderStack, slotId);
        if (adapterId == null) {
            return null;
        }
        Identifier hostId = CustomItemDataHelper.getCustomId(holderStack);
        if (ICustomItem.EMPTY_ID.equals(hostId)
                || AdapterLookupHelper.getAdapterTarget(adapterId, hostId, slotId) == null) {
            return null;
        }
        return TimelessHelperC.getSlotAdapterDisplay(adapterId).orElse(null);
    }

    /* FIXME rendering
    /// > 处理某槽位上的适配器：渲染适配器 overlay 小模型，并返回叠加适配器 mount_offset 之后的子配件落点变换。
    /// >
    /// > 未选适配器、适配器目标无效或 display 缺失时，原样返回 slotTransform。
    /// >
    /// > @param holderStack   该槽位所属的宿主物品堆（适配器选择存于此）\
    /// > @param slotId        槽位 id\
    /// > @param slotTransform 子配件原始落点变换（已含导轨偏移等）\
    /// > @return 应用适配器偏移后的子配件落点变换
    private Matrix4f applySlotAdapter(ItemStack holderStack, Identifier slotId, Matrix4f slotTransform,
                                      PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        SlotAdapterDisplay adapterDisplay = resolveValidSlotAdapterDisplay(holderStack, slotId);
        if (adapterDisplay == null || adapterDisplay.getModel() == null) {
            return slotTransform;
        }
        /// > 适配器模型渲染在槽位挂点处
        poseStack.pushPose();
        PoseStackTransformHelper.mulPoseAndNormal(poseStack, slotTransform);
        Identifier texture = adapterDisplay.getModelTexture();
        RenderType quadType = RenderType.entityCutout(texture);
        RenderType triangleType = BedrockModelRenderTypes.polyMeshCutout(texture);
        adapterDisplay.getModel().renderToBuffer(poseStack, bufferSource, quadType, triangleType, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        /// > 子配件落点被适配器抬高
        return applyMountOffset(slotTransform, adapterDisplay.getMountOffset());
    }
    */

    /* FIXME rendering
    /// > 渲染原厂配件的子配件：子槽位的 posGroup 与 slot display 复用宿主枪模型/display，
    /// > 子配件相对宿主模型绝对定位。
    private void renderBuiltinChildren(ItemStack gunStack, Identifier gunDisplayId, ItemStack builtinStack,
                                       PoseStack poseStack, MultiBufferSource bufferSource, int light, int remaining,
                                       BedrockGunModel gunModel, GunDisplay gunDisplay, InteractionHand hand,
                                       boolean cacheMuzzles) {
        if (!(builtinStack.getItem() instanceof IAttachmentHolder holder)) {
            return;
        }
        /// > 遍历全部子槽位（含空槽）：空子槽也可能装有适配器需要渲染
        Map<Identifier, AttachmentSlot> subSlots = holder.getAttachmentSlots(builtinStack);
        for (var slotEntry : subSlots.entrySet()) {
            AttachmentSlot subSlot = slotEntry.getValue();
            ItemStack subAttachment = holder.getAttachment(builtinStack, subSlot);
            var slotDisplay = gunDisplay.getSlot(subSlot.id());
            if (slotDisplay == null || slotDisplay.posGroup() == null) {
                continue;
            }
            Matrix4f slotTransform = gunModel.getGlobalTransform(slotDisplay.posGroup());
            if (slotTransform == null) {
                continue;
            }
            slotTransform = AttachmentMountHelper.applyRailOffset(slotTransform, subSlot, subAttachment);
            /// > 原厂配件子槽的适配器选择存于原厂件自身
            slotTransform = applySlotAdapter(builtinStack, subSlot.id(), slotTransform, poseStack, bufferSource, light);
            if (subAttachment.isEmpty()) {
                continue;
            }
            renderAttachmentNode(gunStack, gunDisplayId, subAttachment, slotTransform, poseStack, bufferSource, light,
                    remaining - 1, gunModel, gunDisplay, hand, cacheMuzzles);
        }
    }
    */

    /* FIXME rendering
    private void renderMountedWeaponAttachment(ItemStack gunStack, Identifier gunDisplayId, PoseStack poseStack,
                                               MultiBufferSource bufferSource, int light, Matrix4f slotTransform,
                                               ItemStack mountedWeaponStack, InteractionHand hand, boolean cacheMuzzles) {
        TimelessHelperC.getMountedWeaponDisplayForGun(gunStack, mountedWeaponStack)
                .or(() -> TimelessHelperC.getGunDisplay(mountedWeaponStack))
                .ifPresent(mountedDisplay -> {
            BedrockGunModel mountedModel = mountedDisplay.getGunModel();
            if (mountedModel == null) {
                return;
            }

            mountedModel.setRenderHand(false);
            var ani = FirstPersonRenderHandler.getActiveAnimationInstance(hand);
            if (ani instanceof GunAnimationInstance gunAni) {
                Pose pose = gunAni.getMountedCachedPose();
                if (pose != null) {
                    mountedModel.applyPose(pose);
                }
            }

            poseStack.pushPose();
            PoseStackTransformHelper.mulPoseAndNormal(poseStack, slotTransform);
            Identifier texture = mountedDisplay.getModelTexture();
            RenderType quadType = RenderType.entityCutout(texture);
            RenderType triangleType = BedrockModelRenderTypes.polyMeshCutout(texture);
            mountedModel.renderToBuffer(poseStack, bufferSource, quadType, triangleType, light, OverlayTexture.NO_OVERLAY);
            if (cacheMuzzles) {
                cacheMuzzleOffsets(poseStack, mountedModel, mountedDisplay, hand, MuzzleReference.underBarrel(0));
                collectFirstPersonLaserBeams(poseStack, mountedModel, mountedDisplay, hand, true, frameLaserDraws);
            }
            poseStack.popPose();
            mountedModel.resetPose();
        });
    }
    */

    /* FIXME rendering
    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemDisplayContext transformType, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource pBuffer,
                             int pPackedLight, int pPackedOverlay) {
        /// > 第三人称副手也不渲染了
/// >        if (transformType == THIRD_PERSON_LEFT_HAND) {
/// >            return;
/// >        }
        super.renderByItem(stack, transformType, poseStack, pBuffer, pPackedLight, pPackedOverlay);
    }
    */

    @Nullable
    @Override
    public IFPAnimationInstance createAnimationInstance(ItemStack stack, Entity entity) {
        return createAnimationInstance(stack, entity, InteractionHand.MAIN_HAND);
    }

    /// > 「同一持有物」语义：忽略弹药 / 配件等 NBT 变化，仅比较枪械自定义 ID。
    /// >
    /// > SBM 用此判定是否需要重建渲染实例（重掏）。默认实现只比 Item 类，会把「同格子换成另一把
    /// > 不同 ID 的枪」误判为同一把而不重建（渲染旧枪）；此处按 customId 比较修正。
    @Override
    public boolean isSameItem(ItemStack oldStack, ItemStack newStack) {
        return DualWieldHelper.sameHeldGun(oldStack, newStack);
    }

    @Nullable
    @Override
    public IFPAnimationInstance createAnimationInstance(ItemStack stack, Entity entity, InteractionHand hand) {
        if (entity instanceof LocalPlayer player) {
            return TimelessHelperC.getGunDisplay(stack).map(display -> {
                return new GunAnimationInstance(player, stack, display, hand);
            }).orElse(null);
        }
        return null;
    }

    @Override
    public long getPutAwayDuration(ItemStack stack) {
        if (stack.getItem() instanceof IGun iGun) {
            return iGun.getIndex(stack)
                    .map(GunIndex::getData)
                    .map(data -> (long) (data.getPutAwayTime() * 1000))
                    .orElse(500L);
        }
        return 500L;
    }

    @Override
    public boolean blockOffhandRender(ItemStack stack) {
        /// > 粗判兜底：双手长枪霸占视野。注意主手「是否霸占副手」的权威判定已移至
        /// > GunAnimationInstance.occupiesView()（绑定实例固定渲染形态，过渡期稳定）；
        /// > 此方法仅作 SBM 接口的物品级兜底，不参与双持核心逻辑。
        return !DualWieldHelper.isOneHandedGun(stack);
    }

    @Override
    public boolean canRenderInHand(ItemStack stack, InteractionHand hand) {
        /// > 副手只渲染单手枪：双手枪放副手不在副手呈现（避免套用双手骨架）。
        if (hand == InteractionHand.OFF_HAND) {
            return DualWieldHelper.isOneHandedGun(stack);
        }
        return true;
    }

    /// > 渲染变体键：当前生效 display 对象本身。双持⇄单持切换会令同一把枪在双手 base 与按手单手
    /// > 变体之间切换，返回的 display 实例随之变化，SBM 据此经统一 put_away→draw 通路重建渲染实例。
    /// > 与 [GunAnimationInstance#effectiveDisplay()] 的解析口径保持一致。
    @Override
    @Nullable
    public Object getRenderVariantKey(ItemStack stack, InteractionHand hand) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        GunDisplay base = getDisplay(stack);
        if (base == null) {
            return null;
        }
        if (base.hasOneHandVariants() && DualWieldHelper.isOneHandedForm(player, hand)) {
            return base.forHand(hand);
        }
        return base;
    }

    /* FIXME rendering
    private void applyFirstPersonPositioningTransform(PoseStack poseStack, BedrockGunModel model, ItemStack stack, float aimingProgress,
                                                             @Nullable GunDisplay display,
                                                             @Nullable LocalGunOperator.AimViewState aimViewState) {
        AIMING_DYNAMICS.target(0, aimingProgress);
        float smoothT = AIMING_DYNAMICS.get(0);

        Matrix4f idleViewMatrix = new Matrix4f(model.getGlobalTransform("idle_view"));
        Matrix4f aimViewMatrix = resolveAimViewMatrix(model, stack, display,
                aimViewState == null ? LocalGunOperator.AimViewState.NONE : aimViewState);

        MathUtil.applyMatrixLerp(idleViewMatrix, aimViewMatrix, idleViewMatrix, smoothT);

        /// > 改装定位：在「正常定位矩阵」与「改装视角矩阵」之间按开启进度插值。
        /// > 改装视角矩阵来自模型的 `refit_view`（总览）或选中槽位的 `refit_<slot>_view` 骨骼。
        float openProgress = RefitTransform.getOpeningProgress();
        if (openProgress > 1.0e-3f) {
            Matrix4f refitTarget = resolveRefitViewMatrix(model, stack);
            if (refitTarget != null) {
                updateRefitFocusMatrix(refitTarget, System.nanoTime());
                applyRefitViewMatrix(idleViewMatrix, refitFocusMatrix, openProgress);
            }
        } else {
            refitFocusInit = false;
            refitFocusStartNanos = 0L;
        }

        /// > 改装拖拽旋转：在视图空间绕「相机视线轴（屏幕中心）× 枪体深度」旋转。
        /// > refit_view 把枪摆到屏幕中央，故屏幕视觉中心 = 视线轴在枪深度处的点；
        /// > 用模型几何中心投影到视图空间取其深度 z，绕 (0,0,z) 自转即对准屏幕中心。
        float dragYaw = RefitTransform.getDragYaw() * openProgress;
        float dragPitch = RefitTransform.getDragPitch() * openProgress;
        boolean applyDrag = openProgress > 1.0e-3f
                && (Math.abs(dragYaw) > 1.0e-3f || Math.abs(dragPitch) > 1.0e-3f);
        if (applyDrag) {
            /// > modelToView = idleViewMatrix.invert()（取反前先复制，避免后续就地 invert 影响）
            Matrix4f modelToView = new Matrix4f(idleViewMatrix).invert();
            Vector3f viewCenter = modelToView.transformPosition(model.getModelCenter(), new Vector3f());
            poseStack.translate(0, 0, viewCenter.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(dragYaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(dragPitch));
            poseStack.translate(0, 0, -viewCenter.z);
        }

        poseStack.mulPoseMatrix(idleViewMatrix.invert());
    }
    */

    /* FIXME rendering
    /// > 解析当前（含切换过渡）瞄准视点矩阵。
    /// >
    /// > 本体视位（机瞄 / 战术拒枪）直接取定位组骨骼在动画后的全局变换；瞄具取「静态挂点 × 附件模型
    /// > scope_view」（静态挂点避免视点取逆与动画约束重复抵消枪体动画）。过渡中在旧 / 新视点间插值；
    /// > 无视图位（缺 display）或取不到矩阵时回退枪模型的 `iron_view`。
    private Matrix4f resolveAimViewMatrix(BedrockGunModel model, ItemStack stack,
                                          @Nullable GunDisplay display,
                                          LocalGunOperator.AimViewState state) {
        Matrix4f fallback = new Matrix4f(model.getGlobalTransform("iron_view"));
        if (display == null) {
            return fallback;
        }
        List<AimView> views = AimViewHelper.collect(stack, display);
        if (views.isEmpty()) {
            return fallback;
        }
        int activeIndex = Mth.clamp(state.activeIndex(), 0, views.size() - 1);
        List<ScopeStencilRenderer.Scope> scopes = collectAllStencilScopes(stack, model, display, true);
        Matrix4f activeView = aimViewMatrixOf(model, scopes, views.get(activeIndex));
        if (activeView == null) {
            return fallback;
        }
        int pendingIndex = state.pendingIndex();
        if (pendingIndex >= 0) {
            int pendingClamped = Mth.clamp(pendingIndex, 0, views.size() - 1);
            if (pendingClamped != activeIndex) {
                Matrix4f pendingView = aimViewMatrixOf(model, scopes, views.get(pendingClamped));
                if (pendingView != null) {
                    MathUtil.applyMatrixLerp(activeView, pendingView, activeView,
                            Mth.clamp(state.progress(), 0f, 1f));
                }
            }
        }
        return activeView;
    }
    */

    /* FIXME rendering
    /// > 单个视图位的视点矩阵；取不到返回 null（调用方回退 iron_view）。
    @Nullable
    private static Matrix4f aimViewMatrixOf(BedrockGunModel model, List<ScopeStencilRenderer.Scope> scopes, AimView view) {
        if (view instanceof AimView.BodyView body) {
            Matrix4f boneTransform = model.getGlobalTransform(body.viewBone());
            return boneTransform == null ? null : new Matrix4f(boneTransform);
        }
        Identifier slotId = ((AimView.ScopeView) view).slotId();
        for (ScopeStencilRenderer.Scope scope : scopes) {
            if (slotId.equals(scope.slotId())) {
                return scopeViewOf(scope);
            }
        }
        return null;
    }
    */

    /// > 推进改装槽位焦点。一次切换期间始终使用固定的起点和终点，避免把上一帧的中间矩阵反馈为下一帧目标。
    private static void updateRefitFocusMatrix(Matrix4f target, long now) {
        if (!refitFocusInit) {
            refitFocusMatrix.set(target);
            refitFocusStartMatrix.set(target);
            refitFocusTargetMatrix.set(target);
            refitFocusStartNanos = now;
            refitFocusInit = true;
            return;
        }

        interpolateRigidMatrix(refitFocusStartMatrix, refitFocusTargetMatrix, refitFocusMatrix,
                RefitFocusSmoothing.progressForElapsedNanos(now - refitFocusStartNanos));
        if (!refitFocusTargetMatrix.equals(target, 1.0e-6f)) {
            refitFocusStartMatrix.set(refitFocusMatrix);
            refitFocusTargetMatrix.set(target);
            refitFocusStartNanos = now;
        }
        interpolateRigidMatrix(refitFocusStartMatrix, refitFocusTargetMatrix, refitFocusMatrix,
                RefitFocusSmoothing.progressForElapsedNanos(now - refitFocusStartNanos));
    }

    /// > 在正常第一人称定位与改装定位之间直接混合刚体变换。
    /// >
    /// > 不复用 [MathUtil#applyMatrixLerp]：改装视角中存在大角度骨骼，其欧拉角相对旋转在角度分支处会跳变。
    private static void applyRefitViewMatrix(Matrix4f current, Matrix4f target, float alpha) {
        interpolateRigidMatrix(current, target, current, alpha);
    }

    private static void interpolateRigidMatrix(Matrix4f from, Matrix4f to, Matrix4f result, float alpha) {
        if (alpha >= 0.9999f) {
            result.set(to);
            return;
        }
        Vector3f translation = from.getTranslation(new Vector3f())
                .lerp(to.getTranslation(new Vector3f()), alpha);
        Quaternionf rotation = from.getNormalizedRotation(new Quaternionf())
                .slerp(to.getNormalizedRotation(new Quaternionf()), alpha);
        result.translationRotate(translation.x(), translation.y(), translation.z(), rotation);
    }

    /// > 解析当前改装视角对应的骨骼全局变换（枪模型空间，bind 静态）。
    /// >
    /// > 无聚焦时用 `refit_view`（总览）。聚焦某槽位（含嵌套子配件）时：
    /// > - 骨骼优先：若该槽位所属 holder 的 display 为其声明了 `refit_view_group` 骨骼，
    /// >   取该骨骼在 holder 模型空间的变换，累积父挂点得到枪模型空间视角矩阵。
    /// > - 挂点兜底：否则用该槽位挂点位置，套用顶层段 refit 视角的旋转，合成朝向该部位的视角。
    /// >
    /// > 任意环节失败回退到顶层 refit view，最终回退 `refit_view`。
    @Nullable
    private static Matrix4f resolveRefitViewMatrix(BedrockGunModel model, ItemStack stack) {
        AttachmentPath focus = RefitTransform.getFocusPath();
        if (focus == null || focus.isEmpty()) {
            return bindOrNull(model, "refit_view");
        }
        GunDisplay gunDisplay = TimelessHelperC.getGunDisplay(stack).orElse(null);
        if (gunDisplay == null || !(stack.getItem() instanceof IGun)) {
            return bindOrNull(model, "refit_view");
        }

        /// > 顶层段的 refit 视角骨骼名（用于兜底旋转）
        Identifier topSlot = focus.slots().getFirst();
        AttachmentSlotDisplayCfg topSlotDisplay = gunDisplay.getSlot(topSlot);
        String topViewBone = (topSlotDisplay != null && topSlotDisplay.refitView() != null && !topSlotDisplay.refitView().isEmpty())
                ? topSlotDisplay.refitView() : "refit_view";

        /// > 顶层聚焦：直接用顶层骨骼（与旧逻辑一致）
        if (focus.depth() == 1) {
            return bindOrNull(model, topViewBone);
        }

        /// > 嵌套聚焦：沿路径累积到目标槽位的挂点，并尝试取目标 holder display 的 refit 骨骼
        NestedView nested = resolveNestedView(model, gunDisplay, stack, focus);
        if (nested == null) {
            return bindOrNull(model, topViewBone);
        }
        if (nested.viewMatrix != null) {
            return nested.viewMatrix;
        }
        /// > 兜底：把顶层视角「相机相对部位的偏移」平移到嵌套部位，避免相机贴在挂点上。
        /// > 相机锚点 = 嵌套挂点 + (顶层视角锚点 - 顶层挂点)，旋转沿用顶层视角。
        Matrix4f topView = model.getBindGlobalTransform(topViewBone);
        Matrix4f fallback = new Matrix4f();
        Vector3f nestedPos = nested.mount.getTranslation(new Vector3f());
        Vector3f offset = new Vector3f();
        if (topView != null) {
            AttachmentSlotDisplayCfg topSd = gunDisplay.getSlot(topSlot);
            Matrix4f topMount = (topSd != null && topSd.posGroup() != null)
                    ? model.getBindGlobalTransform(topSd.posGroup()) : null;
            if (topMount != null) {
                /// > offset = 顶层视角锚点 - 顶层挂点（相机相对该部位的位移）
                topView.getTranslation(offset).sub(topMount.getTranslation(new Vector3f()));
            } else {
                topView.getTranslation(offset);
            }
        }
        fallback.translation(nestedPos.add(offset));
        if (topView != null) {
            fallback.rotate(topView.getNormalizedRotation(new Quaternionf()));
        }
        return fallback;
    }

    private static Matrix4f bindOrNull(BedrockGunModel model, String boneName) {
        Matrix4f m = model.getBindGlobalTransform(boneName);
        if (m == null) {
            m = model.getBindGlobalTransform("refit_view");
        }
        return m == null ? null : new Matrix4f(m);
    }

    /// > 嵌套视角解析结果：目标槽位挂点（枪模型空间）+ 可选的 refit 骨骼视角矩阵。
    private record NestedView(Matrix4f mount, @Nullable Matrix4f viewMatrix) {
    }

    /// > 沿聚焦路径累积到目标槽位的挂点变换（枪模型空间），并尝试取目标槽位所属 holder
    /// > display 声明的 refit_view 骨骼。变换规则与改装槽位投影一致。
    @Nullable
    private static NestedView resolveNestedView(BedrockGunModel gunModel, GunDisplay gunDisplay,
                                                ItemStack gunStack, AttachmentPath focus) {
        if (!(gunStack.getItem() instanceof IGun iGun)) {
            return null;
        }
        /// > 当前 holder 链：从枪开始逐层下降
        ItemStack holderStack = gunStack;
        IAttachmentHolder holder = iGun;
        /// > 父挂点累积（枪模型空间）
        Matrix4f accMount = new Matrix4f();
        /// > 解析到目标槽位（focus 末端）
        for (int i = 0; i < focus.depth(); i++) {
            Identifier slotId = focus.slots().get(i);
            AttachmentSlot slot = holder.getAttachmentSlots(holderStack).get(slotId);
            if (slot == null) {
                return null;
            }
            boolean builtinHolder = (holderStack != gunStack) && IAttachment.isBuiltinAttachment(holderStack);

            /// > 取该槽位挂点（在当前 holder 坐标系），以及对应的 slot display
            Matrix4f mount;
            AttachmentSlotDisplayCfg slotDisplay;
            if (holderStack == gunStack) {
                /// > 顶层：枪模型空间
                slotDisplay = gunDisplay.getSlot(slotId);
                if (slotDisplay == null || slotDisplay.posGroup() == null) {
                    return null;
                }
                Matrix4f m = gunModel.getBindGlobalTransform(slotDisplay.posGroup());
                if (m == null) {
                    return null;
                }
                mount = AttachmentMountHelper.applyRailOffset(m, slot, holder.getStoredAttachment(holderStack, slot));
            } else if (builtinHolder) {
                /// > 原厂配件子槽：复用宿主枪模型，绝对定位
                slotDisplay = gunDisplay.getSlot(slotId);
                if (slotDisplay == null || slotDisplay.posGroup() == null) {
                    return null;
                }
                Matrix4f m = gunModel.getBindGlobalTransform(slotDisplay.posGroup());
                if (m == null) {
                    return null;
                }
                mount = AttachmentMountHelper.applyRailOffset(m, slot, holder.getStoredAttachment(holderStack, slot));
            } else {
                /// > 独立配件子槽：父挂点 × 配件模型挂点
                AttachmentDisplay ad = TimelessHelperC.getAttachmentDisplay(holderStack).orElse(null);
                if (ad == null || ad.getModel() == null) {
                    return null;
                }
                slotDisplay = ad.getSlot(slotId);
                if (slotDisplay == null || slotDisplay.posGroup() == null) {
                    return null;
                }
                Matrix4f local = ad.getModel().getGlobalTransform(slotDisplay.posGroup());
                if (local == null) {
                    return null;
                }
                mount = new Matrix4f(accMount).mul(local);
                mount = AttachmentMountHelper.applyRailOffset(mount, slot, holder.getStoredAttachment(holderStack, slot));
            }

            if (i == focus.depth() - 1) {
                /// > 末端槽位：尝试取 refit 骨骼视角
                Matrix4f viewMatrix = null;
                if (slotDisplay.refitView() != null && !slotDisplay.refitView().isEmpty()) {
                    if (holderStack == gunStack || builtinHolder) {
                        /// > 视角骨骼在枪模型空间
                        viewMatrix = bindOrNull(gunModel, slotDisplay.refitView());
                    } else {
                        /// > 视角骨骼在配件模型空间，累积父挂点
                        AttachmentDisplay ad = TimelessHelperC.getAttachmentDisplay(holderStack).orElse(null);
                        if (ad != null && ad.getModel() != null) {
                            Matrix4f vb = ad.getModel().getGlobalTransform(slotDisplay.refitView());
                            if (vb != null) {
                                viewMatrix = new Matrix4f(accMount).mul(vb);
                            }
                        }
                    }
                }
                return new NestedView(mount, viewMatrix);
            }

            /// > 继续下降：进入该槽位的子配件
            ItemStack child = holder.getStoredAttachment(holderStack, slot);
            if (child.isEmpty() || !(child.getItem() instanceof IAttachmentHolder childHolder)) {
                return null;
            }
            accMount = mount;
            holderStack = child;
            holder = childHolder;
        }
        return null;
    }

}
