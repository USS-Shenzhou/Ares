package cn.ussshenzhou.ares.gun.client.renderer.item;

/* FIXME rendering
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.model.SlotModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import cn.ussshenzhou.ares.gun.TimelessHelperC;
import cn.ussshenzhou.ares.gun.item.ICustomItem;
import cn.ussshenzhou.ares.gun.client.model.BedrockAmmoModel;
import cn.ussshenzhou.ares.gun.client.resource.display.AmmoDisplayInstance;
import cn.ussshenzhou.ares.gun.util.CustomItemDataHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/// > 弹药物品渲染器（独立 BEWLR）。
/// > <p>
/// > 弹药是无动画的静态物品，不走 SBM 的第一人称动画驱动渲染（{@code AbstractGeoItemRendererV2} 依赖动画实例），
/// > 故采用独立的 {@link BlockEntityWithoutLevelRenderer}：模型层用 v2 的 {@link BedrockAmmoModel}（与弹壳 / 挂饰同源）。
/// > <p>
/// > GUI 上下文或模型 / 贴图缺失时渲染 2D 格子图标；其余上下文（展示框 / 地面 / 第三人称手持等）渲染 3D 模型，
/// > 通过弹药模型的定位组（{@code fixed} / {@code ground} / {@code thirdperson_hand}）把对应锚点对齐到方块中心。
/// @author TACZ, USS_Shenzhou
public class AmmoItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final SlotModel SLOT_MODEL = new SlotModel();

    /// > 定位组骨骼名（与 bedrock 模型约定一致）
    private static final String NODE_FIXED = "fixed";
    private static final String NODE_GROUND = "ground";
    private static final String NODE_THIRD_PERSON = "thirdperson_hand";

    public AmmoItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemDisplayContext ctx, @Nonnull PoseStack poseStack,
                             @Nonnull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Identifier ammoId = CustomItemDataHelper.getCustomId(stack);
        AmmoDisplayInstance display = TimelessHelperC.getAmmoDisplay(ammoId).orElse(null);

        BedrockAmmoModel model = display == null ? null : display.getAmmoModel();
        Identifier modelTexture = display == null ? null : display.getModelTexture();

        /// > GUI 上下文或缺少 3D 模型：渲染 2D 格子图标
        if (ctx == ItemDisplayContext.GUI || model == null || modelTexture == null) {
            renderSlot(display, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }

        poseStack.pushPose();
        /// > 基础居中：地面实体稍低，其余居中（与 SBM v2 基类 beforeRender 口径一致）
        if (ctx == ItemDisplayContext.GROUND) {
            poseStack.translate(0.5, 0.3125, 0.5);
        } else {
            poseStack.translate(0.5, 0.5, 0.5);
        }

        /// > display 缩放（绕定位锚点缩放）
        Vector3f scale = display.getScale(ctx);
        poseStack.scale(scale.x(), scale.y(), scale.z());
        /// > 用定位组把对应上下文的锚点对齐到方块中心
        applyPositioning(model, ctx, poseStack);

        RenderType quadType = RenderType.entityCutout(modelTexture);
        RenderType triangleType = BedrockModelRenderTypes.polyMeshCutout(modelTexture);
        model.renderToBuffer(poseStack, bufferSource, quadType, triangleType, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /// > 把对应上下文的定位骨骼移动到当前坐标系原点（乘以其全局变换的逆）。骨骼缺失时不做处理。
    private void applyPositioning(BedrockAmmoModel model, ItemDisplayContext ctx, PoseStack poseStack) {
        String node = switch (ctx) {
            case FIXED -> NODE_FIXED;
            case GROUND -> NODE_GROUND;
            default -> NODE_THIRD_PERSON;
        };
        Matrix4f transform = model.getPositioningTransform(node);
        if (transform == null) {
            return;
        }
        poseStack.mulPoseMatrix(transform.invert());
    }

    /// > 渲染 GUI 2D 格子图标。图标贴图缺失时回退 {@link MissingTextureAtlasSprite}。
    private void renderSlot(@Nullable AmmoDisplayInstance display, PoseStack poseStack, MultiBufferSource bufferSource,
                            int packedLight, int packedOverlay) {
        Identifier slotTexture = display == null ? null : display.getSlotTexture();
        if (slotTexture == null) {
            slotTexture = MissingTextureAtlasSprite.getLocation();
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0);
        SLOT_MODEL.renderToBuffer(poseStack, bufferSource.getBuffer(RenderType.entityTranslucent(slotTexture)),
                packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}
*/
