package cn.ussshenzhou.ares.gun.client.charm.render;

/* FIXME rendering
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
*/
import cn.ussshenzhou.ares.gun.client.charm.physics.CharmFrameForces;
import cn.ussshenzhou.ares.gun.client.model.BedrockCharmModel;
import cn.ussshenzhou.ares.gun.client.model.BedrockGunModel;
import cn.ussshenzhou.ares.gun.custom.charm.CharmPhysicsSettings;
import net.minecraft.client.Minecraft;
import org.joml.Vector3f;

/// > 挂饰渲染器（阶段 3：分层物理摆动）。
/// > <p>
/// > 对接 SimpleBedrockModel v2：用 {@link BedrockGunModel#getGlobalTransform(String)} 取锚点骨骼矩阵，
/// > 进入其局部空间后按 layout 把挂饰拆为「固定环 + 动态环 + 摆体」三段：固定环贴模型根不摆，动态环绕
/// > 固定枢轴主摆 + 吊环铰链扭转，摆体绕动态枢轴主摆 + 轴向滚转。物理用 Verlet 质点（锚点惯性 + 视角
/// > 重力驱动），纯客户端表现，状态按绑定缓存。
/// @author TACZ, USS_Shenzhou
public class CharmRenderer {
    private static final float CAMERA_MOTION_ACCELERATION = 1.35f;
    private static final float CAMERA_FLICK_ACCELERATION = 29.0f;
    private static final float CAMERA_FLICK_DEGREES = 16.0f;
    private static final float CAMERA_REFERENCE_DELTA_SECONDS = 1.0f / 20.0f;
    private static final float MAX_RENDER_DISTANCE_MULTIPLIER = 2.35f;
    private static final float MAX_RENDER_DISTANCE_PADDING = 0.18f;
    private static final float MAX_RENDER_DISPLACEMENT_PER_FRAME = 0.42f;
    private static final float ANCHOR_INERTIA_GAIN = 3.15f;
    /// > 碰撞探针半径（方块）：摆体最大边 × 系数，夹在上下限之间
    private static final float PROBE_RADIUS_FACTOR = 0.35f;
    private static final float MIN_PROBE_RADIUS = 0.03f;
    private static final float MAX_PROBE_RADIUS = 0.18f;

    private static long cachedCameraTick = Long.MIN_VALUE;
    private static int cachedCameraPartialTickBits;
    private static final Vector3f cachedCameraAcceleration = new Vector3f();
    private static boolean cameraInitialized;
    private static float lastYaw;
    private static float lastPitch;

    /* FIXME rendering
    /// > 在 {@code afterRender} 末尾（{@code model.resetPose()} 之前）调用，渲染枪上绑定的挂饰。
    /// > 此时 {@code poseStack} 已含完整第一人称定位变换。
    public static void render(ItemStack gunStack, BedrockGunModel gunModel,
                              PoseStack poseStack, MultiBufferSource buffer, int light) {
        List<GunCharmData> bindings = GunCharmDataHelper.getAll(gunStack);
        if (bindings.isEmpty()) {
            return;
        }
        float partialTick = Minecraft.getInstance().getPartialTick();
        for (GunCharmData binding : bindings) {
            renderOne(gunModel, poseStack, buffer, light, binding, partialTick);
        }
    }
    */

    /* FIXME rendering
    private static void renderOne(BedrockGunModel gunModel, PoseStack poseStack, MultiBufferSource buffer,
                                  int light, GunCharmData binding, float partialTick) {
        Identifier charmId = binding.charmId();
        CharmDisplayInstance display = TimelessHelperC.getCharmDisplay(charmId).orElse(null);
        if (display == null || display.getModel() == null || display.getModelTexture() == null) {
            return;
        }
        Matrix4f boneMat = gunModel.getGlobalTransform(binding.boneName());
        if (boneMat == null) {
            return;
        }

        BedrockCharmModel model = display.getModel();
        CharmData data = TimelessHelperS.getCharmData(charmId).orElse(null);
        CharmModelLayout layout = data == null ? CharmModelLayout.ROD_DOUBLE_RING : data.getLayout();

        Vector3f forwardAxis = new Vector3f(display.getForwardAxis());
        Vector3f localNormal = binding.localNormal();

        /// > 命中点（像素 → 方块），在锚点骨骼局部空间求挂饰模型根锚点
        Vector3f anchorBlocks = new Vector3f(binding.localHit()).mul(1.0f / 16.0f);
        Vector3f basePosition = CharmAnchorPlacement.initialPosition(
                anchorBlocks, binding.localOffset(), localNormal, display.getAnchorClearancePixels());

        Identifier texture = display.getModelTexture();
        RenderType quadType = RenderType.entityCutout(texture);
        RenderType triangleType = BedrockModelRenderTypes.polyMeshCutout(texture);

        poseStack.pushPose();
        /// > 进入锚点骨骼局部空间：localHit / localNormal 直接可用
        poseStack.mulPoseMatrix(boneMat);

        if (layout.isLayered()) {
            renderLayered(gunModel, model, layout, display, data, binding, basePosition, localNormal, forwardAxis,
                    poseStack, buffer, quadType, triangleType, light, partialTick);
        } else {
            renderWhole(model, display, basePosition, localNormal, forwardAxis,
                    poseStack, buffer, quadType, triangleType, light);
        }
        poseStack.popPose();
    }
    */

    /* FIXME rendering
    /// > 分层物理渲染。坐标全部在锚点骨骼局部空间。
    private static void renderLayered(BedrockGunModel gunModel, BedrockCharmModel model, CharmModelLayout layout,
                                      CharmDisplayInstance display, CharmData data, GunCharmData binding,
                                      Vector3f basePosition, Vector3f localNormal, Vector3f forwardAxis,
                                      PoseStack poseStack, MultiBufferSource buffer,
                                      RenderType quadType, RenderType triangleType, int light, float partialTick) {
        CharmLayoutGeometry geometry = CharmLayoutGeometry.get(binding.charmId(), model, layout, localNormal, forwardAxis);
        if (!geometry.isLayered()) {
            renderWhole(model, display, basePosition, localNormal, forwardAxis,
                    poseStack, buffer, quadType, triangleType, light);
            return;
        }

        CharmLayerPlacement placement = CharmLayerPlacement.fromAnchor(
                basePosition,
                geometry.anchorOffset(),
                geometry.fixedPivotOffset(),
                geometry.dynamicPivotOffset(),
                geometry.massOffset());

        Vector3f physicsAnchor = placement.fixedPivotPosition();
        Vector3f restMassPosition = placement.massRestPosition();

        /// > 物理推进
        CharmPhysicsSettings settings = effectiveSwingSettings(data == null ? CharmPhysicsSettings.DEFAULT : data.getPhysics(),
                physicsAnchor, restMassPosition);
        float deltaSeconds = Minecraft.getInstance().getDeltaFrameTime() / 20.0f;
        CharmPhysicsState state = CharmRenderStateCache.swing(binding);
        /// > 锚点骨骼的渲染空间 pose（此处 poseStack 已 mulPoseMatrix(boneMat)，含完整动画 + 第一人称定位）。
        Matrix4f anchorPose = new Matrix4f(poseStack.last().pose());
        /// > 锚点惯性：把物理锚点变到渲染空间做二阶差分，得到锚点加速度（动画甩动挂饰的来源）。
        Vector3f renderAnchor = CharmFrameForces.localPositionToRender(anchorPose, physicsAnchor);
        float inertiaScale = massInertiaScale(settings.mass());
        Vector3f renderAnchorInertia = state.anchorInertialAcceleration(renderAnchor, deltaSeconds)
                .mul(ANCHOR_INERTIA_GAIN * inertiaScale);
        /// > 三股外力（视角重力 + 相机转动 + 锚点惯性）统一换算回锚点骨骼局部空间。
        Vector3f totalAcceleration = CharmFrameForces.capturedTotalAcceleration(
                anchorPose,
                viewGravity(partialTick, settings.gravityScale()),
                cameraAcceleration(partialTick, deltaSeconds).mul(inertiaScale),
                renderAnchorInertia);
        /// > 防穿模：取枪体碰撞盒（锚点骨骼局部空间，缓存，按到命中锚点距离保留最近的）+ 摆体球探针。
        /// > focus 用命中锚点 basePosition（贴枪面），代表挂饰活动区，收集到的盒更贴合摆动范围。
        boolean collisionEnabled = ClientConfig.CHARM_COLLISION_ENABLED.get();
        List<CharmCollisionBox> collisionBoxes = collisionEnabled
                ? CharmCollisionExtractor.get(binding.displayId(), gunModel, binding.boneName(), basePosition)
                : List.of();
        float probeRadius = bodyProbeRadius(model, geometry.bodyBoneName());
        CharmCollisionProbe probe = CharmCollisionProbe.centered(probeRadius);
        state.stepWithCollisionProbes(
                physicsAnchor, restMassPosition, totalAcceleration, settings, deltaSeconds,
                basePosition, localNormal, 0.0f, List.of(probe), collisionBoxes);
        Vector3f massPosition = state.position();
        if (!renderPositionInSafeRange(physicsAnchor, restMassPosition, massPosition, settings)) {
            state.reset(physicsAnchor, restMassPosition);
            massPosition = new Vector3f(restMassPosition);
        }

        /// > 各层姿态
        Vector3f dynamicPivotPosition = currentDynamicPivot(placement.fixedPivotPosition(), placement.dynamicPivotRestPosition(), massPosition);
        Vector3f restBodyMassPosition = restBodyMassPosition(dynamicPivotPosition, placement.dynamicPivotRestPosition(), placement.massRestPosition());
        Quaternionf ringSwing = CharmLayerPlacement.swingRotation(placement.fixedPivotPosition(), placement.dynamicPivotRestPosition(), dynamicPivotPosition);
        Quaternionf bodySwing = CharmLayerPlacement.swingRotation(dynamicPivotPosition, restBodyMassPosition, massPosition);
        Quaternionf ringHinge = CharmRenderStateCache.ringHinge(binding)
                .rotation(placement.fixedPivotPosition(), placement.dynamicPivotRestPosition(), dynamicPivotPosition, localNormal, deltaSeconds, 0.0f);
        Quaternionf bodyRoll = CharmRenderStateCache.bodyRoll(binding)
                .rotation(dynamicPivotPosition, restBodyMassPosition, massPosition, localNormal, deltaSeconds, 0.0f,
                        geometry.bodyEccentricOffset(), totalAcceleration);

        float[] tint = display.getTint();
        float[] scale = display.getScale();
        float[] rotation = display.getRotation();
        float[] dynamicRotation = display.getDynamicRotation();
        SegmentArgs args = new SegmentArgs(model, buffer, quadType, triangleType, light, tint, scale, rotation, forwardAxis, localNormal);

        /// > 固定段：贴模型根，不摆（attachment + gun_ring）
        renderFixedSegment(args, poseStack, placement.modelRootPosition(),
                layout.fixedParts());
        /// > 动态环段：绕固定枢轴主摆 + 吊环扭转（charm_ring）
        renderSwingSegment(args, poseStack, dynamicRotation,
                placement.fixedPivotPosition(), geometry.rawFixedPivotOffset(), geometry.rawDynamicPivotOffset(),
                ringSwing, ringHinge, List.of(layout.dynamicPivotPart()));
        /// > 摆体段：绕动态枢轴主摆 + 轴向滚转（body 子树）
        renderSwingSegment(args, poseStack, dynamicRotation,
                dynamicPivotPosition, geometry.rawDynamicPivotOffset(), geometry.rawBodyPivotOffset(),
                bodySwing, bodyRoll, List.of(geometry.bodyBoneName()));

        /// > 调试：在锚点骨骼局部空间画碰撞盒线框 + 质点探针十字（poseStack 仍处于锚点局部空间）
        if (collisionEnabled && ClientConfig.CHARM_COLLISION_DEBUG.get()) {
            CharmCollisionDebugRenderer.render(poseStack, buffer, collisionBoxes, massPosition, probeRadius);
        }
    }
    */

    /* FIXME rendering
    private static void renderFixedSegment(SegmentArgs args, PoseStack poseStack, Vector3f position,
                                           List<String> boneNames) {
        poseStack.pushPose();
        poseStack.translate(position.x, position.y, position.z);
        poseStack.mulPose(CharmAnchorPlacement.surfaceOrientation(args.forwardAxis(), args.localNormal()));
        applyRenderRotation(poseStack, args.rotation());
        poseStack.scale(args.scale()[0], args.scale()[1], args.scale()[2]);
        renderBones(args, poseStack, boneNames);
        poseStack.popPose();
    }
    */

    /* FIXME rendering
    private static void renderSwingSegment(SegmentArgs args, PoseStack poseStack, float[] dynamicRotation,
                                           Vector3f pivotPosition, Vector3f rawFixedPivotOffset, Vector3f rawDynamicPivotOffset,
                                           Quaternionf swing, Quaternionf hinge, List<String> boneNames) {
        poseStack.pushPose();
        poseStack.translate(pivotPosition.x, pivotPosition.y, pivotPosition.z);
        poseStack.mulPose(swing);
        poseStack.mulPose(hinge);
        poseStack.mulPose(CharmAnchorPlacement.surfaceOrientation(args.forwardAxis(), args.localNormal()));
        applyRenderRotation(poseStack, args.rotation());
        Vector3f dynamicDelta = new Vector3f(rawDynamicPivotOffset).sub(rawFixedPivotOffset);
        poseStack.translate(dynamicDelta.x, dynamicDelta.y, dynamicDelta.z);
        applyRenderRotation(poseStack, dynamicRotation);
        poseStack.translate(-rawDynamicPivotOffset.x, -rawDynamicPivotOffset.y, -rawDynamicPivotOffset.z);
        poseStack.scale(args.scale()[0], args.scale()[1], args.scale()[2]);
        renderBones(args, poseStack, boneNames);
        poseStack.popPose();
    }
    */

    /* FIXME rendering
    private static void renderBones(SegmentArgs args, PoseStack poseStack, List<String> boneNames) {
        for (String name : boneNames) {
            int index = args.model().boneIndex(name);
            if (index < 0) {
                continue;
            }
            args.model().renderBone(index, poseStack, args.buffer(), args.quadType(), args.triangleType(),
                    args.light(), OverlayTexture.NO_OVERLAY, args.tint()[0], args.tint()[1], args.tint()[2], 1.0f);
        }
    }
    */

    /* FIXME rendering
    /// > 非分层挂饰：整体刚性贴在锚点（兜底）。
    private static void renderWhole(BedrockCharmModel model, CharmDisplayInstance display, Vector3f position,
                                    Vector3f localNormal, Vector3f forwardAxis, PoseStack poseStack,
                                    MultiBufferSource buffer, RenderType quadType, RenderType triangleType, int light) {
        float[] tint = display.getTint();
        float[] scale = display.getScale();
        poseStack.pushPose();
        poseStack.translate(position.x, position.y, position.z);
        poseStack.mulPose(CharmAnchorPlacement.surfaceOrientation(forwardAxis, localNormal));
        applyRenderRotation(poseStack, display.getRotation());
        poseStack.scale(scale[0], scale[1], scale[2]);
        model.renderToBuffer(poseStack, buffer, quadType, triangleType, light, OverlayTexture.NO_OVERLAY,
                tint[0], tint[1], tint[2], 1.0f);
        poseStack.popPose();
    }
    */

    private static Vector3f currentDynamicPivot(Vector3f fixedPivot, Vector3f restDynamicPivot, Vector3f massPosition) {
        Vector3f restDelta = new Vector3f(restDynamicPivot).sub(fixedPivot);
        float length = restDelta.length();
        if (!Float.isFinite(length) || length < 1.0E-6f || !finite(massPosition)) {
            return new Vector3f(restDynamicPivot);
        }
        Vector3f direction = new Vector3f(massPosition).sub(fixedPivot);
        if (!finite(direction) || direction.lengthSquared() < 1.0E-8f) {
            direction.set(restDelta);
        }
        return new Vector3f(fixedPivot).fma(length, direction.normalize());
    }

    private static Vector3f restBodyMassPosition(Vector3f currentDynamicPivot, Vector3f restDynamicPivot, Vector3f restMassPosition) {
        return new Vector3f(currentDynamicPivot).add(new Vector3f(restMassPosition).sub(restDynamicPivot));
    }

    private static CharmPhysicsSettings effectiveSwingSettings(CharmPhysicsSettings settings, Vector3f anchor, Vector3f restMassPosition) {
        float restLength = finite(anchor) && finite(restMassPosition) ? anchor.distance(restMassPosition) : 0.0f;
        float ropeLength = Math.max(settings.ropeLength(), restLength);
        return new CharmPhysicsSettings(settings.mass(), ropeLength, settings.damping(), settings.stiffness(),
                settings.solverIterations(), settings.gravityScale());
    }

    private static boolean renderPositionInSafeRange(Vector3f anchor, Vector3f restPosition, Vector3f renderPosition, CharmPhysicsSettings settings) {
        if (!finite(anchor) || !finite(restPosition) || !finite(renderPosition)) {
            return false;
        }
        float restDistance = Math.max(anchor.distance(restPosition), settings.ropeLength());
        float maxDistance = Math.max(0.08f, restDistance * MAX_RENDER_DISTANCE_MULTIPLIER + MAX_RENDER_DISTANCE_PADDING);
        if (anchor.distance(renderPosition) > maxDistance) {
            return false;
        }
        return renderPosition.distance(restPosition) <= maxDistance + MAX_RENDER_DISPLACEMENT_PER_FRAME;
    }

    /// > 质量惯性缩放：轻挂饰甩得更明显、重挂饰更稳，避免不同质量观感失衡。照搬源挂饰模组取值。
    private static float massInertiaScale(float mass) {
        float normalized = Math.max(0.001f, mass) / 0.08f;
        return Math.max(0.42f, Math.min(1.18f, (float) (1.0 / Math.sqrt(normalized))));
    }

    /// > 估算摆体碰撞探针半径（方块）：取摆体子树包围盒最大边 × 系数，夹在上下限之间。
    /// > 用最大边而非最小边——薄片状挂饰（吊牌）最小边只有 1-2 像素，按最小边会退回下限导致球太小、
    /// > 几乎不挡；用最大边代表挂饰的整体尺度，碰撞球能更早把挂饰挡在枪面外。无几何时退回最小半径。
    private static float bodyProbeRadius(BedrockCharmModel model, String bodyBoneName) {
        if (model == null || bodyBoneName == null || bodyBoneName.isBlank()) {
            return MIN_PROBE_RADIUS;
        }
        Vector3f size = model.subtreeSize(bodyBoneName).orElse(null);
        if (size == null) {
            return MIN_PROBE_RADIUS;
        }
        float maxEdge = Math.max(Math.max(0.0f, size.x), Math.max(Math.max(0.0f, size.y), Math.max(0.0f, size.z)));
        if (!Float.isFinite(maxEdge) || maxEdge <= 0.0f) {
            return MIN_PROBE_RADIUS;
        }
        return Math.max(MIN_PROBE_RADIUS, Math.min(maxEdge * PROBE_RADIUS_FACTOR, MAX_PROBE_RADIUS));
    }

    private static Vector3f viewGravity(float partialTick, float gravityScale) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return CharmFrameForces.viewPitchGravity(0.0f, gravityScale);
        }
        return CharmFrameForces.viewPitchGravity(minecraft.player.getViewXRot(partialTick), gravityScale);
    }

    private static Vector3f cameraAcceleration(float partialTick, float deltaSeconds) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            cameraInitialized = false;
            return new Vector3f();
        }
        long tick = minecraft.level == null ? Long.MIN_VALUE : minecraft.level.getGameTime();
        int partialTickBits = Float.floatToIntBits(partialTick);
        if (tick == cachedCameraTick && partialTickBits == cachedCameraPartialTickBits) {
            return new Vector3f(cachedCameraAcceleration);
        }
        float yaw = minecraft.player.getViewYRot(partialTick);
        float pitch = minecraft.player.getViewXRot(partialTick);
        if (!cameraInitialized) {
            lastYaw = yaw;
            lastPitch = pitch;
            cachedCameraTick = tick;
            cachedCameraPartialTickBits = partialTickBits;
            cameraInitialized = true;
            cachedCameraAcceleration.zero();
            return new Vector3f();
        }
        float yawDelta = wrapDegrees(yaw - lastYaw);
        lastYaw = yaw;
        lastPitch = pitch;
        cachedCameraTick = tick;
        cachedCameraPartialTickBits = partialTickBits;
        cachedCameraAcceleration.set(cameraMotionAcceleration(yawDelta, deltaSeconds));
        return new Vector3f(cachedCameraAcceleration);
    }

    private static Vector3f cameraMotionAcceleration(float yawDelta, float deltaSeconds) {
        float frameScale = CAMERA_REFERENCE_DELTA_SECONDS / Math.max(1.0E-4f, deltaSeconds);
        float normalizedYawDelta = yawDelta * frameScale;
        float moderateAcceleration = -normalizedYawDelta * CAMERA_MOTION_ACCELERATION;
        float flickAcceleration = -CAMERA_FLICK_ACCELERATION * (float) Math.tanh(normalizedYawDelta / CAMERA_FLICK_DEGREES);
        return new Vector3f(
                Math.abs(moderateAcceleration) < Math.abs(flickAcceleration) ? moderateAcceleration : flickAcceleration,
                0.0f,
                0.0f
        );
    }

    private static float wrapDegrees(float value) {
        while (value > 180.0f) {
            value -= 360.0f;
        }
        while (value < -180.0f) {
            value += 360.0f;
        }
        return value;
    }

    /* FIXME rendering
    private static void applyRenderRotation(PoseStack poseStack, float[] rotation) {
        if (rotation == null || rotation.length < 3) {
            return;
        }
        if (rotation[2] != 0.0f) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation[2]));
        }
        if (rotation[1] != 0.0f) {
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation[1]));
        }
        if (rotation[0] != 0.0f) {
            poseStack.mulPose(Axis.XP.rotationDegrees(rotation[0]));
        }
    }
    */

    private static boolean finite(Vector3f value) {
        return value != null && Float.isFinite(value.x) && Float.isFinite(value.y) && Float.isFinite(value.z);
    }

    /* FIXME rendering
    /// > 三段渲染共享的不变参数打包，减少方法签名长度。
    private record SegmentArgs(BedrockCharmModel model, MultiBufferSource buffer, RenderType quadType,
                               RenderType triangleType, int light, float[] tint, float[] scale,
                               float[] rotation, Vector3f forwardAxis, Vector3f localNormal) {
    }
    */
}
