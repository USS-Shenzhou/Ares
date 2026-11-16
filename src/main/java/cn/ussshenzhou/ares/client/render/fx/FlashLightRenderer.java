package cn.ussshenzhou.ares.client.render.fx;

/* FIXME rendering
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.gameplay.MuzzleReference;
import cn.ussshenzhou.ares.client.renderer.entity.FirstPersonPoseCache;
import cn.ussshenzhou.ares.gun.mixin.client.IAccessorPostChain;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.List;

/// > 枪口火光/手电环境照明后处理。
/// >
/// > 纯屏幕空间实现：shader 用主帧缓冲深度重建视图空间坐标，按 From/To/Angle/Range 计算锥形照明，
/// > 并沿光源到片元的视图空间线段采样主深度图，近似处理屏幕内遮挡。
/// @author TACZ, USS_Shenzhou
public class FlashLightRenderer {
    public static final boolean DEBUG = false;

    private static final Identifier POST_CHAIN_LOCATION = Identifier.fromNamespaceAndPath(Ares.MOD_ID, "shaders/post/flashlight.json");

    private static final long FLASH_DURATION_MS = 120L;
    private static final float FLASH_PEAK_LUMINANCE = 2.5f;
    private static final float FLASH_ANGLE = (float) Math.toRadians(40.0);
    private static final float FLASH_RANGE = 12.0f;

    public static final float DEFAULT_FLASHLIGHT_ANGLE = (float) Math.toRadians(18.0);
    public static final float DEFAULT_FLASHLIGHT_RANGE = 32.0f;
    public static final float DEFAULT_FLASHLIGHT_LUMINANCE = 0.58f;
    /// > 第一人称模型的视图空间深度与场景世界尺度存在少量观感差异，近距离时把光源向相机侧收一点。
    private static final float FLASHLIGHT_VIEW_DEPTH_BIAS = 0.28f;

    private static final int RAY_MARCH_STEPS = 24;
    private static final float RAY_MARCH_DEPTH_BIAS = 0.10f;
    private static final float RAY_MARCH_START = 0.04f;
    private static final float RAY_MARCH_END = 0.90f;
    private static final float RAY_MARCH_SOFTNESS = 0.55f;

    private static final SpotlightProfile MUZZLE_FLASH_PROFILE = new SpotlightProfile(
            new Vector3f(1.0f, 0.78f, 0.48f),
            0.40f, 1.45f,
            0.65f, 1.10f, 1.00f
    );
    private static final SpotlightProfile FLASHLIGHT_PROFILE = new SpotlightProfile(
            new Vector3f(1.0f, 0.88f, 0.68f),
            0.45f, 1.70f,
            0.38f, 1.35f, 0.42f
    );

    private static PostChain postChain;
    private static int lastWidth = -1;
    private static int lastHeight = -1;
    private static boolean failed = false;

    /// > 主深度备份，避免后处理 process 破坏主帧缓冲深度而影响后续手部/粒子渲染。
    private static RenderTarget depthBackup;

    /// > 计算当前开火爆闪强度（0 表示无需渲染）。
    public static float computeFlashLuminance(long lastFireTimeMs) {
        if (lastFireTimeMs <= 0) {
            return 0f;
        }
        long elapsed = System.currentTimeMillis() - lastFireTimeMs;
        if (elapsed < 0 || elapsed >= FLASH_DURATION_MS) {
            return 0f;
        }
        float t = 1.0f - (float) elapsed / FLASH_DURATION_MS;
        return FLASH_PEAK_LUMINANCE * t;
    }

/// > 执行一次持续手电后处理（参数化）。
/// >
/// > 纯屏幕空间聚光灯：from/to 为视图空间光源点与朝向，color/angle/range/luminance 为灯光参数。
/// >
/// > @param proj       当前帧场景投影矩阵
/// >
/// > @param from       视图空间光源起点
/// >
/// > @param to         视图空间光源指向点
/// >
/// > @param lightColor 灯光颜色（RGB 0~1）
/// >
/// > @param angle      锥角（弧度）
/// >
/// > @param range      照射范围（米）
/// >
/// > @param luminance  亮度
    public static void renderFlashlight(Matrix4f proj, Vector3f from, Vector3f to,
                                        Vector3f lightColor, float angle, float range, float luminance) {
        SpotlightProfile profile = new SpotlightProfile(
                lightColor,
                FLASHLIGHT_PROFILE.innerAngleScale(),
                FLASHLIGHT_PROFILE.outerAngleScale(),
                FLASHLIGHT_PROFILE.alphaMax(),
                FLASHLIGHT_PROFILE.lightRoomPower(),
                FLASHLIGHT_PROFILE.maxChannelLift()
        );
        Vector3f biasedFrom = new Vector3f(from);
        biasedFrom.z = Math.min(0.0f, biasedFrom.z + FLASHLIGHT_VIEW_DEPTH_BIAS);
        Vector3f biasedTo = new Vector3f(to);
        renderSpotlight(profile, luminance, biasedFrom, biasedTo, angle, range, proj);
    }

    /// > 执行一次枪口火光后处理。
    /// >
    /// > @param luminance 爆闪强度（>0），由 [#computeFlashLuminance] 计算
    /// >
    /// > @param envDim    环境调光系数 [0,1]，由玩家眼位真实光照算出，亮处趋近 0
    /// >
    /// > @param proj      当前帧场景投影矩阵
    /// >
    /// > @param view      当前帧场景 modelView 矩阵（保留签名兼容当前调用；纯屏幕空间路径不使用）
    public static void render(float luminance, float envDim, Matrix4f proj, Matrix4f view, Camera camera) {
        luminance *= envDim;
        if (luminance <= 0.01f) {
            return;
        }

        Vector3f muzzleView = FirstPersonPoseCache.getMuzzleOffset(InteractionHand.MAIN_HAND, MuzzleReference.primary());
        Vector3f from = (muzzleView != null) ? new Vector3f(muzzleView) : new Vector3f(0, 0, 0);
        Vector3f to = new Vector3f(from).add(0, 0, -1);

        renderSpotlight(MUZZLE_FLASH_PROFILE, luminance, from, to, FLASH_ANGLE, FLASH_RANGE, proj);
    }

    private static void renderSpotlight(SpotlightProfile profile, float luminance, Vector3f from, Vector3f to, float angle, float range, Matrix4f proj) {
        if (failed || luminance <= 0.01f) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();

        if (!ensureLoaded(mc)) {
            return;
        }

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        if (width != lastWidth || height != lastHeight) {
            postChain.resize(width, height);
            ensureDepthBackup(width, height);
            lastWidth = width;
            lastHeight = height;
        }

        Matrix4f invProj = new Matrix4f(proj).invert();

        List<PostPass> passes = ((IAccessorPostChain) postChain).getPasses();
        for (PostPass pass : passes) {
            var effect = pass.getEffect();
            effect.safeGetUniform("InvProjMat").set(invProj);
            effect.safeGetUniform("SceneProjMat").set(proj);
            effect.safeGetUniform("From").set(from);
            effect.safeGetUniform("To").set(to);
            effect.safeGetUniform("Angle").set(angle);
            effect.safeGetUniform("Range").set(range);
            effect.safeGetUniform("Luminance").set(luminance);
            effect.safeGetUniform("MinZ").set(0.0f);
            effect.safeGetUniform("LightColor").set(profile.lightColor());
            effect.safeGetUniform("RayMarchSteps").set(RAY_MARCH_STEPS);
            effect.safeGetUniform("RayMarchDepthBias").set(RAY_MARCH_DEPTH_BIAS);
            effect.safeGetUniform("RayMarchStart").set(RAY_MARCH_START);
            effect.safeGetUniform("RayMarchEnd").set(RAY_MARCH_END);
            effect.safeGetUniform("RayMarchSoftness").set(RAY_MARCH_SOFTNESS);
            effect.safeGetUniform("InnerAngleScale").set(profile.innerAngleScale());
            effect.safeGetUniform("OuterAngleScale").set(profile.outerAngleScale());
            effect.safeGetUniform("AlphaMax").set(profile.alphaMax());
            effect.safeGetUniform("LightRoomPower").set(profile.lightRoomPower());
            effect.safeGetUniform("MaxChannelLift").set(profile.maxChannelLift());
        }

        RenderTarget main = mc.getMainRenderTarget();
        ensureDepthBackup(width, height);
        depthBackup.copyDepthFrom(main);

        if (DEBUG) {
            LogUtils.getLogger().info("[TACZ][flashlight] 执行后处理 lum={} from={} to={} angle={} range={} passes={}",
                    luminance, from, to, angle, range, passes.size());
        }

        postChain.process(mc.getFrameTime());

        mc.getMainRenderTarget().copyDepthFrom(depthBackup);
        mc.getMainRenderTarget().bindWrite(false);
    }

    private static boolean ensureLoaded(Minecraft mc) {
        if (postChain != null) {
            return true;
        }
        try {
            postChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), POST_CHAIN_LOCATION);
            int width = mc.getWindow().getWidth();
            int height = mc.getWindow().getHeight();
            postChain.resize(width, height);
            ensureDepthBackup(width, height);
            lastWidth = width;
            lastHeight = height;
            if (DEBUG) {
                LogUtils.getLogger().info("[TACZ][flashlight] PostChain 加载成功 {}x{}", width, height);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            failed = true;
            LogUtils.getLogger().error("[TACZ] 加载枪口火光/手电后处理 shader 失败，已禁用该效果", e);
            return false;
        }
    }

    private static void ensureDepthBackup(int width, int height) {
        if (depthBackup == null) {
            depthBackup = new TextureTarget(width, height, true, Minecraft.ON_OSX);
        } else if (depthBackup.width != width || depthBackup.height != height) {
            depthBackup.resize(width, height, Minecraft.ON_OSX);
        }
    }

    private record SpotlightProfile(
            Vector3f lightColor,
            float innerAngleScale,
            float outerAngleScale,
            float alphaMax,
            float lightRoomPower,
            float maxChannelLift
    ) {}

    /// > 资源重载时丢弃已加载的后处理链，下次使用时重建。
    public static void reset() {
        if (postChain != null) {
            postChain.close();
            postChain = null;
        }
        failed = false;
        lastWidth = -1;
        lastHeight = -1;
    }
}
*/
