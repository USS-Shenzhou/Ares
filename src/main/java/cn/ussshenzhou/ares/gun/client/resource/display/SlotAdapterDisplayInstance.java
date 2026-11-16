package cn.ussshenzhou.ares.gun.client.resource.display;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.google.common.base.Preconditions;
import cn.ussshenzhou.ares.gun.client.model.BedrockAttachmentModel;
import cn.ussshenzhou.ares.gun.client.resource.ClientAssetsManager;
import cn.ussshenzhou.ares.gun.client.resource.display.SlotAdapterDisplay;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;
import org.joml.Vector3f;

/// > 槽位适配器的客户端显示实例（含烘焙后的 bedrock 模型）。
/// @author TACZ, USS_Shenzhou
public class SlotAdapterDisplayInstance {
    private BedrockAttachmentModel model;
    private Identifier modelTexture;
    @Nullable
    private Vector3f mountOffset;

    /// > 必须在资源包重载期间构建，否则所需的资源缓存不可用
    public static SlotAdapterDisplayInstance of(SlotAdapterDisplay pojo) {
        SlotAdapterDisplayInstance instance = new SlotAdapterDisplayInstance();
        instance.mountOffset = pojo.mountOffset;

        Preconditions.checkArgument(pojo.modelLocation != null, "slot adapter display missing model field");
        BedrockModelPOJO modelPojo = ClientAssetsManager.INSTANCE.getModelPojo(pojo.modelLocation);
        Preconditions.checkArgument(modelPojo != null, "there is no corresponding model file: " + pojo.modelLocation);
        instance.model = new BedrockAttachmentModel(modelPojo);
        Preconditions.checkArgument(pojo.modelTexture != null, "slot adapter display missing texture field");
        instance.modelTexture = pojo.modelTexture.withPrefix("textures/").withSuffix(".png");

        return instance;
    }

    public BedrockAttachmentModel getModel() {
        return model;
    }

    public Identifier getModelTexture() {
        return modelTexture;
    }

    /// > 适配器对其上子配件落点施加的额外平移；无则返回 null。
    @Nullable
    public Vector3f getMountOffset() {
        return mountOffset;
    }
}
