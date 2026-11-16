package cn.ussshenzhou.ares.client.model;

/* FIXME rendering
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
*/
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel;
import javax.annotation.Nullable;
import org.joml.Matrix4f;

/// > 弹药本体模型（v2）。弹药无动画与子骨骼逻辑，仅静态渲染整棵模型。
/// >
/// > 与 [BedrockShellModel] 同源（[TreeBedrockModel]），额外暴露定位组能力：
/// > 弹药模型含 `fixed` / `ground` / `thirdperson_hand` 定位骨骼，渲染器据此把对应
/// > 上下文的定位点对齐到渲染中心。模型无动画，instance 恒处于 bind 状态，定位骨骼的全局变换即 bind 值。
/// @author TACZ, USS_Shenzhou
public class BedrockAmmoModel {
    private final TreeBedrockModel baseModel;
    private final TreeModelInstance instance;

    public BedrockAmmoModel(BedrockModelPOJO pojo) {
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

    /// > 取定位骨骼在 bind pose 下相对模型原点的全局变换（模型空间）。骨骼不存在返回 `null`。
    @Nullable
    public Matrix4f getPositioningTransform(String boneName) {
        int index = baseModel.getIndex(boneName);
        return index < 0 ? null : new Matrix4f(instance.getGlobalTransform(index));
    }
}
