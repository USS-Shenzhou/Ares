package cn.ussshenzhou.ares.client.handler;

/*
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.dimension.DimensionType;

/// > 镭射（激光指示器）的渲染辅助工具。
/// >
/// > 第一人称镭射由 `BedrockGunItemRenderer` 在枪模型渲染阶段直绘（与枪体 bob/sway/动画
/// > 同步，受配件/枪开关状态门控）；本类仅保留世界光照下光束可见度的计算入口
/// > （[#computeBeamAlpha]）。
/// @author TACZ, USS_Shenzhou
public class LaserHandler {
    /// > 最大射程（米）。
    private static final float MAX_RANGE = 64.0f;

    /// > 由玩家眼位真实世界光照算出光束可见度 [0,1]：亮处趋近 0、暗处趋近 1。
    /// > 算法对齐 `FlashLightHandler.computeEnvDim`（原版 LightTexture 公式）。
    public static float computeBeamAlpha(LocalPlayer player, float partialTick) {
        BlockPos eyePos = BlockPos.containing(player.getEyePosition());
        ClientLevel level = (ClientLevel) player.level();
        DimensionType dim = level.dimensionType();

        int skyLevel = level.getBrightness(LightLayer.SKY, eyePos);
        int blockLevel = level.getBrightness(LightLayer.BLOCK, eyePos);

        float f = level.getSkyDarken(partialTick);
        float skyFactor = level.getSkyFlashTime() > 0 ? 1.0f : f * 0.95f + 0.05f;

        float skyBrightness = LightTexture.getBrightness(dim, skyLevel) * skyFactor;
        float blockBrightness = LightTexture.getBrightness(dim, blockLevel);
        float brightness = Mth.clamp(Math.max(skyBrightness, blockBrightness), 0.0f, 1.0f);

        /// > 亮处束几乎不可见，暗处全量；留少量底值使白天也能隐约看到一线。
        float t = Mth.clamp(brightness / 0.85f, 0.0f, 1.0f);
        return 0.12f + 0.88f * (1.0f - t);
    }
}
*/
