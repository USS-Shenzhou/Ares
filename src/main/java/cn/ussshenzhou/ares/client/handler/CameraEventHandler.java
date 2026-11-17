package cn.ussshenzhou.ares.client.handler;

import net.neoforged.fml.common.EventBusSubscriber;
import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.client.util.AimFovHelper;
import cn.ussshenzhou.ares.gun.custom.GunRecoil;
import cn.ussshenzhou.ares.gun.custom.GunRecoilKeyFrame;
import cn.ussshenzhou.ares.gun.handler.LivingEntityHandler;
import cn.ussshenzhou.ares.gun.util.math.FovSmoother;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.joml.*;

/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class CameraEventHandler {
    /// > FOV 平滑器（主线程按帧推进的指数逼近，替代旧二阶动力学：无后台线程滞后 / 无跟踪停顿）。
    /// > 开镜曲线由 easeInExpo 自平滑，平滑器只过滤切档 / 滚轮的目标突变，故速率取大值（低滞后）。
    public static final FovSmoother WORLD_FOV_DYNAMICS = new FovSmoother(70.0F, 25.0F);
    public static final FovSmoother ITEM_MODEL_FOV_DYNAMICS = new FovSmoother(70.0F, 25.0F);

    private static PolynomialSplineFunction pitchSplineFunction;
    private static PolynomialSplineFunction yawSplineFunction;
    private static long shootTimeStamp = -1L;
    private static double xRotO = 0;
    private static double yRotO = 0;

    /// > 测试用
    public static Vector3f extractEulerAnglesYXZ(Matrix3fc matrix) {
        Vector3f euler = new Vector3f();

        float r32 = matrix.m21();
        float r12 = matrix.m01();
        float r22 = matrix.m11();
        float r31 = matrix.m20();
        float r33 = matrix.m22();

        final float EPSILON = 1e-4f;

        /// > Clamp r32 to the range [-1, 1] to avoid NaN from asin due to floating point inaccuracies
        r32 = org.joml.Math.clamp(r32, -1.0f, 1.0f);

        if (org.joml.Math.abs(r32) < 1.0f - EPSILON) {
            euler.x = org.joml.Math.asin(-r32);
            euler.y = org.joml.Math.atan2(r31, r33);
            euler.z = org.joml.Math.atan2(r12, r22);
        } else {
            if (org.joml.Math.abs(r31) < EPSILON && org.joml.Math.abs(r33) < EPSILON &&
                    org.joml.Math.abs(r12) < EPSILON && org.joml.Math.abs(r22) < EPSILON) {
                euler.x = (float) (r32 > 0 ? -org.joml.Math.PI / 2 : org.joml.Math.PI / 2);
                /// > when x ≈ -90°
                /// > r21 ≈ -(sin(y)cos(z) + cos(y)sin(z))
                /// > r11 ≈ cos(y)cos(z) - sin(y)sin(z)
                /// > when x ≈ 90°
                /// > r21 ≈ sin(y)cos(z) - cos(y)sin(z)
                /// > r11 ≈ cos(y)cos(z) + sin(y)sin(z)
                float r21 = matrix.m10();
                float r11 = matrix.m00();
                euler.z = (float) java.lang.Math.atan2(-r21, r11);
                euler.y = 0;
            } else {
                euler.x = (float) java.lang.Math.asin(-r32);
                euler.y = (float) java.lang.Math.atan2(r31, r33);
                euler.z = (float) java.lang.Math.atan2(r12, r22);
            }
        }

        return  euler;
    }

    public static Vector3fc asEulerAngle(Quaternionf quaternion) {
        Matrix3f m = new Matrix3f().set(quaternion);
        return extractEulerAnglesYXZ(m);
    }

    public static void initialCameraRecoil() {
        pitchSplineFunction = GUN_RECOIL.genPitchSplineFunction(1.0f);
        yawSplineFunction = GUN_RECOIL.genYawSplineFunction(1.0f);
        shootTimeStamp = System.currentTimeMillis();
        xRotO = 0;
        yRotO = 0;
    }

    // FIXME wait until TACZ update
    public static final GunRecoil GUN_RECOIL = new GunRecoil(
            new GunRecoilKeyFrame[]{
                    new GunRecoilKeyFrame(0, 0.66f, 0.66f),
                    new GunRecoilKeyFrame(0.1f, 0.66f, 0.66f),
                    new GunRecoilKeyFrame(0.45f, -0.175f, -0.175f),
                    new GunRecoilKeyFrame(0.6f, 0f, 0f)
            },
            new GunRecoilKeyFrame[]{
                    new GunRecoilKeyFrame(0, -0.23f, 0.18f),
                    new GunRecoilKeyFrame(0.35f, 0f, 0f)
            }
    );

    @SubscribeEvent
    public static void excludeGunSlowdownFov(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        float walkingSpeed = player.getAbilities().getWalkingSpeed();
        if (speed == null || walkingSpeed <= 0.0F
                || speed.getModifier(LivingEntityHandler.GUN_SPEED_MODIFIER_ID) == null) {
            return;
        }

        /// > 仅从视野计算中剔除枪械减速，避免服务端同步的瞄准 / 换弹减速再次驱动世界 FOV。
        /// > 用独立属性重算，保留疾跑、药水等修饰器，也正确处理减速为零和属性范围钳制。
        AttributeInstance visualSpeed = new AttributeInstance(speed.getAttribute(), ignored -> {});
        visualSpeed.setBaseValue(speed.getBaseValue());
        for (AttributeModifier modifier : speed.getModifiers()) {
            if (!modifier.id().equals(LivingEntityHandler.GUN_SPEED_MODIFIER_ID)) {
                visualSpeed.addTransientModifier(modifier);
            }
        }
        float speedFactor = ((float) speed.getValue() / walkingSpeed + 1.0F) / 2.0F;
        float visualSpeedFactor = ((float) visualSpeed.getValue() / walkingSpeed + 1.0F) / 2.0F;
        float correctedFov = event.getFovModifier() * visualSpeedFactor / speedFactor;
        if (Float.isFinite(correctedFov)) {
            double scale = Minecraft.getInstance().options.fovEffectScale().get();
            event.setNewFovModifier(event.getNewFovModifier()
                    + (float) ((correctedFov - event.getFovModifier()) * scale));
        }
    }

    @SubscribeEvent
    public static void applyAimingFov(ViewportEvent.ComputeFov event) {
        Entity entity = event.getCamera().entity();
        if (!(entity instanceof LivingEntity livingEntity)) {
            updateFovDynamics(event, (float) event.getFOV());
            return;
        }
        ItemStack stack = livingEntity.getMainHandItem();
        if (!(stack.getItem() instanceof IGun)) {
            updateFovDynamics(event, (float) event.getFOV());
            return;
        }

        float aimingProgress = IGunOperator.asGunHolder(livingEntity)
                .map(cap -> cap.aimingProgress(event.getPartialTick()))
                .orElse(0.0F);
        float fovProgress = AimFovHelper.easeInExpo(aimingProgress);
        /// > 原版帧时长单位是 tick，平滑器需要秒；暂停时不继续追赶目标。
        Minecraft minecraft = Minecraft.getInstance();
        float dt = minecraft.isPaused() ? 0.0F
                : Mth.clamp(minecraft.getDeltaTracker().getGameTimeDeltaTicks() / 20.0F, 0.0F, 0.1F);
        if (isComputingWorldFov()) {
            /// > 世界画面（含镜内画面）缩放：开镜目标 = 当前瞄具倍率 FOV（views_fov 档位 / 无级位置 /
            /// > 机瞄 iron_zoom 换算），随开镜进度插值。倍率变化只改世界 FOV，模型 FOV 走另一组
            /// > 配套值（zoom_model_fov，见 else 分支），二者一一对应互不混用。
            /// > 插值曲线本身平滑（easeInExpo），平滑器只过滤切档 / 滚轮的目标突变。
            double baseFov = event.getFOV();
            float targetFov = AimFovHelper.getAimingWorldTargetFov(stack, baseFov);
            event.setFOV(WORLD_FOV_DYNAMICS.get((float) Mth.lerp(fovProgress, baseFov, targetFov), dt));
        } else {
            /// > 模型 FOV 组（与倍率一一配套）：常驻 model_fov，开镜过渡到当前瞄具 views_fov
            /// > （档位配套 / 无级同源映射，优先于模型本身）；机瞄回退枪械 zoom_model_fov。
            float modelFov = AimFovHelper.getModelFov(stack, event.getFOV());
            float zoomModelFov = AimFovHelper.getZoomModelFov(stack, modelFov);
            float fov = Mth.lerp(fovProgress, modelFov, zoomModelFov);
            event.setFOV(ITEM_MODEL_FOV_DYNAMICS.get(fov, dt));
        }
    }

    private static void updateFovDynamics(ViewportEvent.ComputeFov event, float fov) {
        if (isComputingWorldFov()) {
            WORLD_FOV_DYNAMICS.reset(fov);
        } else {
            ITEM_MODEL_FOV_DYNAMICS.reset(fov);
        }
    }

    // FIXME distinguish caller: since the NeoForge 26.2-pre-2 port ComputeFov no longer has usedConfiguredFov(), and Camera posts it from both calculateFov (world FOV) and calculateHudFov (hand FOV), so the call stack is walked instead.
    private static boolean isComputingWorldFov() {
        String cameraClassName = Camera.class.getName();
        return StackWalker.getInstance().walk(frames -> frames
                .filter(frame -> frame.getClassName().equals(cameraClassName))
                .map(StackWalker.StackFrame::getMethodName)
                .filter(methodName -> methodName.equals("calculateFov") || methodName.equals("calculateHudFov"))
                .findFirst()
                .map(methodName -> methodName.equals("calculateFov"))
                .orElse(true));
    }

    @SubscribeEvent
    public static void applyCameraRecoil(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        long timeTotal = System.currentTimeMillis() - shootTimeStamp;
        if (pitchSplineFunction != null && pitchSplineFunction.isValidPoint(timeTotal)) {
            double value = pitchSplineFunction.value(timeTotal);
            player.setXRot(player.getXRot() - (float) (value - xRotO));
            xRotO = value;
        }
        if (yawSplineFunction != null && yawSplineFunction.isValidPoint(timeTotal)) {
            double value = yawSplineFunction.value(timeTotal);
            player.setYRot(player.getYRot() - (float) (value - yRotO));
            yRotO = value;
        }
    }
}
