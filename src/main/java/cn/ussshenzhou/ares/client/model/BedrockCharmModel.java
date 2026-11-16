package cn.ussshenzhou.ares.client.model;

/* FIXME rendering
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
*/
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.ICube;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBoneDefinition;
import javax.annotation.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.Optional;

/// > 挂饰模型（v2）。
/// >
/// > 除整体静态渲染外，向挂饰物理渲染暴露分层能力：取 layout 骨骼在 bind pose 下的 pivot 偏移、
/// > 计算某子树几何的体积加权质心、按骨骼名渲染单根子树。挂饰模型无动画，instance 恒处于 bind 状态，
/// > 故几何查询直接读 instance 的全局变换即可。
/// @author TACZ, USS_Shenzhou
public class BedrockCharmModel {
    private final TreeBedrockModel baseModel;
    private final TreeModelInstance instance;

    public BedrockCharmModel(BedrockModelPOJO pojo) {
        this.baseModel = TreeBedrockModel.bake(pojo);
        this.instance = baseModel.createInstance();
    }

    /* FIXME rendering
    public void renderToBuffer(PoseStack poseStack, MultiBufferSource bufferSource, RenderType quadRenderType,
                               RenderType triangleRenderType, int packedLight, int packedOverlay) {
        renderToBuffer(poseStack, bufferSource, quadRenderType, triangleRenderType, packedLight, packedOverlay,
                1, 1, 1, 1);
    }
    */

    /* FIXME rendering
    public void renderToBuffer(PoseStack poseStack, MultiBufferSource bufferSource, RenderType quadRenderType,
                               RenderType triangleRenderType, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        baseModel.renderToBuffer(instance, poseStack, bufferSource, quadRenderType, triangleRenderType, packedLight, packedOverlay,
                red, green, blue, alpha, true);
    }
    */

    /* FIXME rendering
    /// > 渲染单根骨骼及其子树（用于分层物理渲染）。
    public void renderBone(int boneIndex, PoseStack poseStack, MultiBufferSource bufferSource, RenderType quadRenderType,
                           RenderType triangleRenderType, int packedLight, int packedOverlay,
                           float red, float green, float blue, float alpha) {
        if (boneIndex < 0) {
            return;
        }
        baseModel.renderBone(instance, boneIndex, poseStack, bufferSource.getBuffer(quadRenderType),
                packedLight, packedOverlay, red, green, blue, alpha, true, true);
        baseModel.renderBone(instance, boneIndex, poseStack, bufferSource.getBuffer(triangleRenderType),
                packedLight, packedOverlay, red, green, blue, alpha, false, true);
    }
    */

    /// > 骨骼名 → 索引；不存在返回 -1。
    public int boneIndex(String boneName) {
        return boneName == null ? -1 : baseModel.getIndex(boneName);
    }

    /// > 取某骨骼在 bind pose 下相对模型原点的 pivot 偏移（方块单位，模型空间）。不存在返回空。
    public Optional<Vector3f> pivotOffset(String boneName) {
        int index = boneIndex(boneName);
        if (index < 0) {
            return Optional.empty();
        }
        return Optional.of(instance.getGlobalTransform(index).transformPosition(new Vector3f()));
    }

    /// > 取某骨骼在 bind pose 下相对模型原点的全局变换；不存在返回空。
    @Nullable
    public Matrix4f bindGlobalTransform(String boneName) {
        int index = boneIndex(boneName);
        return index < 0 ? null : instance.getGlobalTransform(index);
    }

    /// > 计算某骨骼子树所有 cube 的体积加权几何中心（方块单位，模型空间）。
    /// > 用于摆体质心定位与偏心力矩。locator 类辅助骨骼不计入。
    public Optional<Vector3f> subtreeVolumeWeightedCenter(String rootBoneName) {
        int rootIndex = boneIndex(rootBoneName);
        if (rootIndex < 0) {
            return Optional.empty();
        }
        Vector3f weighted = new Vector3f();
        float[] total = new float[]{0.0f};
        accumulateSubtreeCenter(rootIndex, weighted, total);
        if (total[0] <= 0.0f) {
            return Optional.empty();
        }
        return Optional.of(weighted.div(total[0]));
    }

    /// > 计算某骨骼子树所有 cube 顶点的包围盒尺寸（方块单位，模型空间）。
    /// > 用于估算摆体碰撞探针半径。locator 类辅助骨骼不计入。无 cube 返回空。
    public Optional<Vector3f> subtreeSize(String rootBoneName) {
        int rootIndex = boneIndex(rootBoneName);
        if (rootIndex < 0) {
            return Optional.empty();
        }
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        boolean[] any = new boolean[]{false};
        accumulateSubtreeBounds(rootIndex, min, max, any);
        if (!any[0]) {
            return Optional.empty();
        }
        return Optional.of(new Vector3f(max).sub(min));
    }

    private void accumulateSubtreeBounds(int boneIndex, Vector3f min, Vector3f max, boolean[] any) {
        TreeBoneDefinition def = baseModel.bone(boneIndex);
        if (!isNonPhysicalBoneName(def.name())) {
            Matrix4f global = instance.getGlobalTransform(boneIndex);
            for (ICube cube : def.cubes()) {
                float volume = cube.width() * cube.height() * cube.depth();
                if (!(volume > 0.0f) || !Float.isFinite(volume)) {
                    continue;
                }
                float x1 = cube.x();
                float y1 = cube.y();
                float z1 = cube.z();
                float x2 = x1 + cube.width();
                float y2 = y1 + cube.height();
                float z2 = z1 + cube.depth();
                for (int i = 0; i < 8; i++) {
                    Vector3f corner = new Vector3f(
                            (i & 1) == 0 ? x1 : x2,
                            (i & 2) == 0 ? y1 : y2,
                            (i & 4) == 0 ? z1 : z2);
                    Vector3f world = global.transformPosition(corner);
                    if (!finite(world)) {
                        continue;
                    }
                    min.min(world);
                    max.max(world);
                    any[0] = true;
                }
            }
        }
        for (int child : def.children()) {
            accumulateSubtreeBounds(child, min, max, any);
        }
    }

    private void accumulateSubtreeCenter(int boneIndex, Vector3f weighted, float[] total) {
        TreeBoneDefinition def = baseModel.bone(boneIndex);
        if (!isNonPhysicalBoneName(def.name())) {
            Matrix4f global = instance.getGlobalTransform(boneIndex);
            for (ICube cube : def.cubes()) {
                float volume = cube.width() * cube.height() * cube.depth();
                if (!(volume > 0.0f) || !Float.isFinite(volume)) {
                    continue;
                }
                Vector3f localCenter = new Vector3f(
                        cube.x() + cube.width() * 0.5f,
                        cube.y() + cube.height() * 0.5f,
                        cube.z() + cube.depth() * 0.5f
                );
                Vector3f worldCenter = global.transformPosition(localCenter);
                if (!finite(worldCenter)) {
                    continue;
                }
                weighted.fma(volume, worldCenter);
                total[0] += volume;
            }
        }
        for (int child : def.children()) {
            accumulateSubtreeCenter(child, weighted, total);
        }
    }

    private static boolean isNonPhysicalBoneName(String boneName) {
        if (boneName == null) {
            return false;
        }
        String lower = boneName.toLowerCase(Locale.ROOT);
        return lower.endsWith("locator") || lower.contains("_locator");
    }

    private static boolean finite(Vector3f value) {
        return value != null && Float.isFinite(value.x) && Float.isFinite(value.y) && Float.isFinite(value.z);
    }

    public TreeBedrockModel getBaseModel() {
        return baseModel;
    }

    public TreeModelInstance getInstance() {
        return instance;
    }
}
