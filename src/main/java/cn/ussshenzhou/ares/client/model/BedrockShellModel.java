package cn.ussshenzhou.ares.client.model;

/* FIXME rendering
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
*/
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel;

/// > 弹壳模型（v2）。弹壳无动画与子骨骼逻辑，仅静态渲染整棵模型。
/// >
/// > 与 [BedrockAttachmentModel] 同源（[TreeBedrockModel]），但不涉及瞄具/目镜/分划板等
/// > 特殊骨骼处理，是最精简的渲染包装。
/// @author TACZ, USS_Shenzhou
public class BedrockShellModel {
    private final TreeBedrockModel baseModel;
    private final TreeModelInstance instance;

    public BedrockShellModel(BedrockModelPOJO pojo) {
        this.baseModel = TreeBedrockModel.bake(pojo);
        this.instance = baseModel.createInstance();
    }

    /* FIXME rendering
    public void renderToBuffer(PoseStack poseStack, MultiBufferSource bufferSource, RenderType quadRenderType,
                               RenderType triangleRenderType, int packedLight, int packedOverlay) {
        baseModel.renderToBuffer(instance, poseStack, bufferSource, quadRenderType, triangleRenderType, packedLight, packedOverlay,
                1, 1, 1, 1, true);
    }
    */
}
