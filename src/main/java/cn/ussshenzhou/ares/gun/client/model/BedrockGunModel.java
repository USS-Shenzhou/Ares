package cn.ussshenzhou.ares.gun.client.model;

/* FIXME rendering
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
*/
import com.mojang.logging.LogUtils;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.BoneIndexProvider;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBoneDefinition;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.basic.BoneTransform;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import cn.ussshenzhou.ares.gun.client.resource.display.AttachmentSlotDisplay;
import javax.annotation.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/// > 枪械模型（v2）。
/// > <p>
/// > 静态几何由共享的 {@link TreeBedrockModel} 提供；当前姿态保存在每个显示实例独立持有的
/// > {@link TreeModelInstance} 上。本类实现 {@link BoneIndexProvider} 以便动画系统按骨骼名绑定索引。
/// @author TACZ, USS_Shenzhou
public class BedrockGunModel implements BoneIndexProvider {
    private static final String LEFT_HAND_BONE = "lefthand_pos";
    private static final String RIGHT_HAND_BONE = "righthand_pos";
    private static final String ROOT_BONE = "root";

    private final TreeBedrockModel baseModel;
    private final TreeModelInstance instance;
    private final Map<Identifier, Matrix4f> attachmentTransforms = new HashMap<>();
    private final int rootBoneIndex;
    private final int leftHandBoneIndex;
    private final int rightHandBoneIndex;
    private final Map<Integer, BoneTransform> bindBoneTransforms = new HashMap<>();
    /// > 改装视角等静态锚点骨骼的 bind pose 全局变换缓存（按骨骼名，可缓存 null 表示不存在）
    private final Map<String, Matrix4f> bindGlobalTransformCache = new HashMap<>();
    /// > 模型几何中心缓存（模型空间），懒计算
    @Nullable
    private Vector3f modelCenter = null;

    private boolean renderHand = true;

    public BedrockGunModel(BedrockModelPOJO pojo, @Nullable Map<Identifier, AttachmentSlotDisplay> attachmentSlots) {
        this.baseModel = TreeBedrockModel.bake(pojo);
        this.instance = baseModel.createInstance();
        this.rootBoneIndex = baseModel.getIndex(ROOT_BONE);
        this.leftHandBoneIndex = baseModel.getIndex(LEFT_HAND_BONE);
        this.rightHandBoneIndex = baseModel.getIndex(RIGHT_HAND_BONE);
        for (BoneTransform transform : baseModel.getBindPose().getBoneTransforms()) {
            bindBoneTransforms.put(transform.boneIndex(), transform);
        }
        if (attachmentSlots != null) {
            initAttachmentTransforms(attachmentSlots);
        }
    }

    /// > 在 bind pose 下采集各配件槽位定位组的全局变换（不受动画影响）。
    /// > 实例新建后即处于 bind 状态，可直接采集。
    private void initAttachmentTransforms(Map<Identifier, AttachmentSlotDisplay> attachmentSlots) {
        instance.resetPose();
        for (var entry : attachmentSlots.entrySet()) {
            String posGroup = entry.getValue().posGroup();
            int index = baseModel.getIndex(posGroup);
            if (index >= 0) {
                this.attachmentTransforms.put(entry.getKey(), new Matrix4f(instance.getGlobalTransform(index)));
            } else {
                LogUtils.getLogger().warn("Attachment slot pos group '{}' not found in model", posGroup);
            }
        }
    }

    public TreeBedrockModel getBaseModel() {
        return baseModel;
    }

    public TreeModelInstance getInstance() {
        return instance;
    }

    public Pose getBindPose() {
        return baseModel.getBindPose();
    }

    public void applyPose(Pose pose) {
        instance.applyPose(pose);
    }

    public void resetPose() {
        instance.resetPose();
    }

    @Override
    public int getIndex(String boneName) {
        return baseModel.getIndex(boneName);
    }

    @Nullable
    public BoneTransform getBindTransform(int boneIndex) {
        return bindBoneTransforms.get(boneIndex);
    }

    @Nullable
    public BoneState getBone(String boneName) {
        return instance.getBone(boneName);
    }

    @Nullable
    public BoneState getBone(int boneIndex) {
        return instance.getBone(boneIndex);
    }

    /// > 返回从根骨骼到指定骨骼（含）的索引路径。
    public int[] getConstraintPath(int boneIndex) {
        if (boneIndex < 0) {
            return new int[0];
        }
        java.util.List<Integer> path = new java.util.ArrayList<>();
        int current = boneIndex;
        while (current >= 0) {
            path.add(0, current);
            current = baseModel.bone(current).parentIndex();
        }
        int[] result = new int[path.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = path.get(i);
        }
        return result;
    }

    @Nullable
    public BoneState getGunRootBone() {
        return instance.getBone(rootBoneIndex);
    }

    /// > 获取骨骼当前（受动画影响）的全局变换矩阵。
    @Nullable
    public Matrix4f getGlobalTransform(String boneName) {
        int index = baseModel.getIndex(boneName);
        return index >= 0 ? instance.getGlobalTransform(index) : null;
    }

    public Matrix4f getGlobalTransform(int boneIndex) {
        return instance.getGlobalTransform(boneIndex);
    }

    /// > 获取配件的不受动画影响的安装点 transform。
    /// >
    /// > @param slot 配件槽位
    @Nullable
    public Matrix4f getAttachmentTransform(Identifier slot) {
        return this.attachmentTransforms.get(slot);
    }

    /// > 获取指定骨骼在 bind pose 下的全局变换（不受动画影响），结果按骨骼名缓存。
    /// > <p>
    /// > 用于改装视角等「静态锚点」骨骼：直接读 {@link #getGlobalTransform(String)} 会带上
    /// > 待机呼吸等动画位移导致镜头持续抖动，故沿父链累乘 bind 局部变换得到稳定值。
    @Nullable
    public Matrix4f getBindGlobalTransform(String boneName) {
        if (bindGlobalTransformCache.containsKey(boneName)) {
            return bindGlobalTransformCache.get(boneName);
        }
        int index = baseModel.getIndex(boneName);
        Matrix4f result = index < 0 ? null : computeBindGlobalTransform(index);
        bindGlobalTransformCache.put(boneName, result);
        return result;
    }

    @Nullable
    private Matrix4f computeBindGlobalTransform(int boneIndex) {
        int[] path = getConstraintPath(boneIndex);
        if (path.length == 0) {
            return null;
        }
        /// > 沿父链累乘各骨骼的 bind 局部变换（不受动画影响）。
        /// > tree 模型的 BoneDefinition.bindLocalTransform() 恒为 null 且 rotateAroundPivot=false，
        /// > 故这里按 BoneState.applyCurrentSelfTransform 的非 pivot 分支重建：
        /// >   translate(bind/16) → rotate(bindRotation)，bind scale 恒为 1。
        Matrix4f matrix = new Matrix4f();
        for (int idx : path) {
            TreeBoneDefinition def = baseModel.bone(idx);
            if (def == null) {
                continue;
            }
            matrix.translate(def.bindX() / 16.0F, def.bindY() / 16.0F, def.bindZ() / 16.0F);
            matrix.rotate(def.bindRotation());
        }
        return matrix;
    }

    public boolean isRenderHand() {
        return renderHand;
    }

    public void setRenderHand(boolean renderHand) {
        this.renderHand = renderHand;
    }

    /// > 模型几何中心（模型空间，不受动画影响），按需计算并缓存。
    /// > <p>
    /// > 优先取渲染包围盒中心；包围盒缺失时回退到所有可见几何骨骼 bind 全局位置的包围盒中心，
    /// > 排除 view / camera / hand 等纯锚点骨骼以免把中心拉偏。用于改装拖拽旋转的旋转中心。
    public Vector3f getModelCenter() {
        if (modelCenter != null) {
            return modelCenter;
        }
        AABB box = baseModel.getRenderBoundingBox();
        if (box != null) {
            modelCenter = new Vector3f((float) box.getCenter().x, (float) box.getCenter().y, (float) box.getCenter().z);
            return modelCenter;
        }
        /// > 回退：统计骨骼 bind 全局位置包围盒
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        boolean any = false;
        for (TreeBoneDefinition def : baseModel.bones()) {
            String name = def.name() == null ? "" : def.name().toLowerCase(java.util.Locale.ENGLISH);
            if (name.contains("view") || name.contains("camera") || name.contains("hand")) {
                continue;
            }
            Matrix4f g = computeBindGlobalTransform(def.index());
            if (g == null) {
                continue;
            }
            Vector3f p = g.getTranslation(new Vector3f());
            minX = Math.min(minX, p.x); minY = Math.min(minY, p.y); minZ = Math.min(minZ, p.z);
            maxX = Math.max(maxX, p.x); maxY = Math.max(maxY, p.y); maxZ = Math.max(maxZ, p.z);
            any = true;
        }
        modelCenter = any
                ? new Vector3f((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f)
                : new Vector3f();
        return modelCenter;
    }

    /* FIXME rendering
    /// > 渲染枪械几何（cube + poly mesh 双 pass），并按需渲染双臂。
    public void renderToBuffer(PoseStack poseStack, MultiBufferSource bufferSource, RenderType quadRenderType,
                               RenderType triangleRenderType, int packedLight, int packedOverlay) {
        /// > 手部定位骨骼本身不渲染几何
        hideBone(leftHandBoneIndex);
        hideBone(rightHandBoneIndex);

        baseModel.renderToBuffer(instance, poseStack, bufferSource, quadRenderType, triangleRenderType, packedLight, packedOverlay,
                1, 1, 1, 1, true);

        if (renderHand) {
            renderHands(poseStack, packedLight, bufferSource);
        }
    }
    */

    private void hideBone(int boneIndex) {
        BoneState bone = instance.getBone(boneIndex);
        if (bone != null) {
            bone.visible = false;
        }
    }

    /* FIXME rendering
    private void renderHands(PoseStack poseStack, int packedLight, MultiBufferSource bufferSource) {
        if (leftHandBoneIndex >= 0) {
            poseStack.pushPose();
            MathUtil.mulMatrix(poseStack, instance.getGlobalTransform(leftHandBoneIndex));
            renderFirstPersonArm(Minecraft.getInstance().player, bufferSource, HumanoidArm.LEFT, poseStack, packedLight);
            poseStack.popPose();
        }
        if (rightHandBoneIndex >= 0) {
            poseStack.pushPose();
            MathUtil.mulMatrix(poseStack, instance.getGlobalTransform(rightHandBoneIndex));
            renderFirstPersonArm(Minecraft.getInstance().player, bufferSource, HumanoidArm.RIGHT, poseStack, packedLight);
            poseStack.popPose();
        }
    }
    */

    /* FIXME rendering
    public static void renderFirstPersonArm(LocalPlayer player, MultiBufferSource bufferSource, HumanoidArm hand, PoseStack matrixStack, int combinedLight) {
        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher renderManager = mc.getEntityRenderDispatcher();
        PlayerRenderer renderer = (PlayerRenderer) renderManager.getRenderer(player);

        if (hand == HumanoidArm.RIGHT) {
            renderer.renderRightHand(matrixStack, bufferSource, combinedLight, player);
        } else {
            renderer.renderLeftHand(matrixStack, bufferSource, combinedLight, player);
        }
    }
    */
}
