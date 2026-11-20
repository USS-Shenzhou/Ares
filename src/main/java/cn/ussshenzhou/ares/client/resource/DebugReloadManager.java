package cn.ussshenzhou.ares.client.resource;

import com.mojang.logging.LogUtils;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockAnimationFile;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import cn.ussshenzhou.ares.gun.resource.GunGsonHelper;
import cn.ussshenzhou.ares.client.resource.display.GunDisplay;
import cn.ussshenzhou.ares.client.resource.display.GunDisplayCfg;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/// @author TACZ, USS_Shenzhou
public class DebugReloadManager {

    /// > 尝试热重载指定显示配置 ID 及其所有依赖
    /// >
    /// > @param displayId 显示配置 ID (e.g. "tacz:ak47_display")
    /// >
    /// > @return 是否成功
    public static boolean reloadGunDisplay(Identifier displayId) {
        LogUtils.getLogger().info("Hot reloading gun display for: {}", displayId);
        
        try {
            /// > 1. 读取 Display JSON
            Identifier displayLoc = Identifier.fromNamespaceAndPath(displayId.getNamespace(), "display/guns/" + displayId.getPath() + ".json");
            GunDisplayCfg cfg = loadJson(displayLoc, GunDisplayCfg.class);
            
            if (cfg == null) {
                LogUtils.getLogger().error("Could not find display file: {}", displayLoc);
                return false;
            }
            
            /// > 2. 加载依赖
            
            /// > 2.1 加载 Model
            if (cfg.modelLocation != null) {
                Identifier fileLoc = FileToIdConverter.json("geo_models").idToFile(cfg.modelLocation);
                BedrockModelPOJO modelPojo = loadJson(fileLoc, BedrockModelPOJO.class);
                if (modelPojo != null) {
                    ClientAssetsManager.INSTANCE.putModel(cfg.modelLocation, modelPojo);
                }
            }
            
            /// > 2.2 加载 Animation
            if (cfg.animationLocation != null) {
                Identifier fileLoc = new FileToIdConverter("animations", ".animation.json").idToFile(cfg.animationLocation);
                BedrockAnimationFile animPojo = loadJson(fileLoc, BedrockAnimationFile.class);
                if (animPojo != null) {
                    ClientAssetsManager.INSTANCE.putAnimation(cfg.animationLocation, animPojo);
                }
            }
            if (cfg.defaultAnimation != null) {
                Identifier fileLoc = new FileToIdConverter("animations", ".animation.json").idToFile(cfg.defaultAnimation);
                BedrockAnimationFile defAnimPojo = loadJson(fileLoc, BedrockAnimationFile.class);
                if (defAnimPojo != null) {
                    ClientAssetsManager.INSTANCE.putAnimation(cfg.defaultAnimation, defAnimPojo);
                }
            }
            
            /// > 3. 重建 GunDisplay
            GunDisplay newInstance = GunDisplay.of(cfg);
            
            /// > 4. 热替换
            ClientAssetsManager.INSTANCE.putGunDisplay(displayId, newInstance);

            LogUtils.getLogger().info("Successfully hot-swap: {}", displayId);
            return true;
            
        } catch (Exception e) {
            LogUtils.getLogger().error("Failed to hot reload display: {}", displayId, e);
            return false;
        }
    }
    
    private static <T> T loadJson(Identifier loc, Class<T> clazz) {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(loc);
            if (resource.isPresent()) {
                try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                    /// > 处理 Minecraft 可能的封装
                    if (loc.getPath().endsWith(".json")) {
                        return GunGsonHelper.GSON.fromJson(reader, clazz);
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.getLogger().warn("Failed to load dependency: {}", loc);
        }
        return null;
    }
}
