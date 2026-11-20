package cn.ussshenzhou.ares.client.resource.display;

import com.mojang.logging.LogUtils;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import cn.ussshenzhou.ares.client.model.BedrockAmmoModel;
import cn.ussshenzhou.ares.client.model.BedrockShellModel;
import cn.ussshenzhou.ares.client.resource.ClientAssetsManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import javax.annotation.Nullable;
import org.joml.Vector3f;

/// > 弹药 display 运行时实例。
/// >
/// > 必须在资源包重载期间构建，否则所需的模型缓存（[ClientAssetsManager#getModelPojo]）不可用。
/// > 承载弹药本体模型 / 贴图 / GUI 图标 / 各上下文缩放，以及抛壳弹壳模型与贴图。模型或贴图缺失则退化
/// > 为 GUI 2D 图标渲染；弹壳缺失则该弹药不抛壳。
/// @author TACZ, USS_Shenzhou
public class AmmoDisplay {
    /// > 各上下文默认缩放（沿用 legacy：手持/地面 0.4，展示框 0.75）
    private static final Vector3f DEFAULT_THIRD_PERSON = new Vector3f(0.4f, 0.4f, 0.4f);
    private static final Vector3f DEFAULT_GROUND = new Vector3f(0.4f, 0.4f, 0.4f);
    private static final Vector3f DEFAULT_FIXED = new Vector3f(0.75f, 0.75f, 0.75f);

    @Nullable
    private BedrockAmmoModel ammoModel;
    @Nullable
    private Identifier modelTexture;
    @Nullable
    private Identifier slotTexture;

    private Vector3f thirdPersonScale = new Vector3f(DEFAULT_THIRD_PERSON);
    private Vector3f groundScale = new Vector3f(DEFAULT_GROUND);
    private Vector3f fixedScale = new Vector3f(DEFAULT_FIXED);

    @Nullable
    private BedrockShellModel shellModel;
    @Nullable
    private Identifier shellTexture;

    public static AmmoDisplay of(AmmoDisplayCfg cfg) {
        AmmoDisplay instance = new AmmoDisplay();
        buildModel(instance, cfg);
        buildScale(instance, cfg);
        buildShell(instance, cfg);
        return instance;
    }

    private static void buildModel(AmmoDisplay instance, AmmoDisplayCfg cfg) {
        instance.ammoModel = null;
        instance.modelTexture = null;
        instance.slotTexture = null;
        /// > GUI 图标贴图（可独立于 3D 模型存在）
        if (cfg.slot != null) {
            instance.slotTexture = cfg.slot.withPrefix("textures/").withSuffix(".png");
        }
        /// > 3D 模型与贴图：缺一则退化 GUI 渲染
        if (cfg.model == null || cfg.texture == null) {
            return;
        }
        BedrockModelPOJO modelPojo = ClientAssetsManager.INSTANCE.getModelPojo(cfg.model);
        if (modelPojo == null) {
            LogUtils.getLogger().warn("Ammo model not found: {}", cfg.model);
            return;
        }
        instance.ammoModel = new BedrockAmmoModel(modelPojo);
        instance.modelTexture = cfg.texture.withPrefix("textures/").withSuffix(".png");
    }

    private static void buildScale(AmmoDisplay instance, AmmoDisplayCfg ammoDisplayCfg) {
        if (ammoDisplayCfg.transform == null || ammoDisplayCfg.transform.scale == null) {
            return;
        }
        AmmoTransformCfg.ScaleCfg scale = ammoDisplayCfg.transform.scale;
        instance.thirdPersonScale = toVector(scale.thirdperson, DEFAULT_THIRD_PERSON);
        instance.groundScale = toVector(scale.ground, DEFAULT_GROUND);
        instance.fixedScale = toVector(scale.fixed, DEFAULT_FIXED);
    }

    private static Vector3f toVector(@Nullable float[] values, Vector3f fallback) {
        if (values == null || values.length < 3) {
            return new Vector3f(fallback);
        }
        return new Vector3f(values[0], values[1], values[2]);
    }

    private static void buildShell(AmmoDisplay instance, AmmoDisplayCfg ammoDisplayCfg) {
        instance.shellModel = null;
        instance.shellTexture = null;
        ShellCfg shell = ammoDisplayCfg.shell;
        if (shell == null || shell.model == null || shell.texture == null) {
            return;
        }
        BedrockModelPOJO modelPojo = ClientAssetsManager.INSTANCE.getModelPojo(shell.model);
        if (modelPojo == null) {
            LogUtils.getLogger().warn("Shell model not found: {}", shell.model);
            return;
        }
        instance.shellModel = new BedrockShellModel(modelPojo);
        instance.shellTexture = shell.texture.withPrefix("textures/").withSuffix(".png");
    }

    @Nullable
    public BedrockAmmoModel getAmmoModel() {
        return ammoModel;
    }

    @Nullable
    public Identifier getModelTexture() {
        return modelTexture;
    }

    @Nullable
    public Identifier getSlotTexture() {
        return slotTexture;
    }

    /// > 取某渲染上下文的缩放向量。第一人称沿用第三人称缩放，其余上下文按各自值返回。
    public Vector3f getScale(ItemDisplayContext ctx) {
        return switch (ctx) {
            case FIXED -> fixedScale;
            case GROUND -> groundScale;
            default -> thirdPersonScale;
        };
    }

    @Nullable
    public BedrockShellModel getShellModel() {
        return shellModel;
    }

    @Nullable
    public Identifier getShellTexture() {
        return shellTexture;
    }
}
