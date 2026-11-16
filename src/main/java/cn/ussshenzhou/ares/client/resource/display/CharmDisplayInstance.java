package cn.ussshenzhou.ares.client.resource.display;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.google.common.base.Preconditions;
import cn.ussshenzhou.ares.client.model.BedrockCharmModel;
import cn.ussshenzhou.ares.client.resource.ClientAssetsManager;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

/// > 挂饰 display 运行时实例。
/// >
/// > 必须在资源包重载期间构建，否则所需的模型缓存（[ClientAssetsManager#getModelPojo]）不可用。
/// > 承载挂饰模型 + 贴图 + 渲染变换；本期先打通加载链路，渲染留到后续阶段。
/// @author TACZ, USS_Shenzhou
public class CharmDisplayInstance {
    private BedrockCharmModel model;
    private Identifier modelTexture;
    private float[] scale = {1.0f, 1.0f, 1.0f};
    private float[] rotation = {0.0f, 0.0f, 0.0f};
    private float[] dynamicRotation = {0.0f, 0.0f, 0.0f};
    private float[] tint = {1.0f, 1.0f, 1.0f};
    private float[] forwardAxis = {0.0f, 0.0f, 1.0f};
    private float anchorClearancePixels = 0.32f;

    public static CharmDisplayInstance of(CharmDisplay pojo) {
        CharmDisplayInstance instance = new CharmDisplayInstance();
        buildModelAndTexture(instance, pojo);
        applyRender(instance, pojo.render);
        return instance;
    }

    private static void buildModelAndTexture(CharmDisplayInstance instance, CharmDisplay pojo) {
        Preconditions.checkArgument(pojo.modelLocation != null, "charm display object missing model field");
        BedrockModelPOJO modelPojo = ClientAssetsManager.INSTANCE.getModelPojo(pojo.modelLocation);
        Preconditions.checkArgument(modelPojo != null, "there is no corresponding model file: " + pojo.modelLocation);
        instance.model = new BedrockCharmModel(modelPojo);
        Preconditions.checkArgument(pojo.modelTexture != null, "charm display object missing texture field");
        instance.modelTexture = pojo.modelTexture.withPrefix("textures/").withSuffix(".png");
    }

    private static void applyRender(CharmDisplayInstance instance, @Nullable CharmDisplay.Render render) {
        if (render == null) {
            return;
        }
        instance.scale = normalizedTriple(render.scale, 1.0f);
        instance.rotation = normalizedTriple(render.rotation, 0.0f);
        instance.dynamicRotation = normalizedTriple(render.dynamicRotation, 0.0f);
        instance.tint = normalizedTriple(render.tint, 1.0f);
        instance.forwardAxis = normalizedAxis(render.forwardAxis);
        if (render.anchorClearancePixels != null && Float.isFinite(render.anchorClearancePixels)) {
            instance.anchorClearancePixels = Math.max(0.0f, render.anchorClearancePixels);
        }
    }

    private static float[] normalizedTriple(@Nullable float[] values, float fallback) {
        float[] triple = {fallback, fallback, fallback};
        if (values != null) {
            for (int i = 0; i < Math.min(values.length, 3); i++) {
                triple[i] = Float.isFinite(values[i]) ? values[i] : fallback;
            }
        }
        return triple;
    }

    private static float[] normalizedAxis(@Nullable float[] values) {
        float[] axis = normalizedTriple(values, 0.0f);
        float lengthSquared = axis[0] * axis[0] + axis[1] * axis[1] + axis[2] * axis[2];
        if (!Float.isFinite(lengthSquared) || lengthSquared < 1.0E-6f) {
            return new float[]{0.0f, 0.0f, 1.0f};
        }
        float inverseLength = (float) (1.0 / Math.sqrt(lengthSquared));
        return new float[]{axis[0] * inverseLength, axis[1] * inverseLength, axis[2] * inverseLength};
    }

    public BedrockCharmModel getModel() {
        return model;
    }

    public Identifier getModelTexture() {
        return modelTexture;
    }

    public float[] getScale() {
        return scale;
    }

    public float[] getRotation() {
        return rotation;
    }

    public float[] getDynamicRotation() {
        return dynamicRotation;
    }

    public float[] getTint() {
        return tint;
    }

    public float[] getForwardAxis() {
        return forwardAxis;
    }

    public float getAnchorClearancePixels() {
        return anchorClearancePixels;
    }
}
