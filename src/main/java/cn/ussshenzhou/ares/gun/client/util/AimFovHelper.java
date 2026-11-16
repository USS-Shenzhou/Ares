package cn.ussshenzhou.ares.gun.client.util;

import cn.ussshenzhou.ares.gun.TimelessHelperC;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.attachment.entity.LocalGunOperator;
import cn.ussshenzhou.ares.attachment.entity.gun.ClientHandGunSession.ZoomPrediction;
import cn.ussshenzhou.ares.gun.client.resource.display.AttachmentDisplayInstance;
import cn.ussshenzhou.ares.gun.client.resource.display.GunDisplayInstance;
import cn.ussshenzhou.ares.gun.util.math.MathUtil;
import cn.ussshenzhou.ares.gun.util.ZoomHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

import java.util.List;

/// @author TACZ, USS_Shenzhou
public class AimFovHelper {
    public static final double DEFAULT_ZOOM_SENSITIVITY_BASE_MULTIPLIER = 1.0;
    public static final double DEFAULT_SCREEN_DISTANCE_COEFFICIENT = 1.33;

    public static float getAimingZoom(ItemStack stack) {
        if (!(stack.getItem() instanceof IGun)) {
            return 1.0F;
        }
        return TimelessHelperC.getGunDisplay(stack)
                .map(GunDisplayInstance::getIronZoom)
                .filter(zoom -> zoom > 0.0F)
                .orElse(1.0F);
    }

    public static float getModelFov(ItemStack stack, double fallbackFov) {
        return TimelessHelperC.getGunDisplay(stack)
                .map(GunDisplayInstance::getModelFov)
                .filter(fov -> fov > 0.0F)
                .orElse((float) fallbackFov);
    }

    /// > 开镜模型 FOV（模型透视）：当前激活瞄具声明的 views_fov **优先于模型本身**——
    /// > 镜子的倍率（zoom）与模型 fov 一一配套：档位模式下 views_fov[zoomIndex] 与档位倍率对应，
    /// > 无级模式下由无级位置同比例映射（[#resolveScopeZoomFov] 输出的就是该配套值）。
    /// > 未装瞄具（机瞄）回退枪械 zoom_model_fov。
    public static float getZoomModelFov(ItemStack stack, float fallbackFov) {
        ViewTransition views = getViewTransition(stack);
        if (views.active() == null) {
            return TimelessHelperC.getGunDisplay(stack)
                    .map(GunDisplayInstance::getZoomModelFov)
                    .filter(fov -> fov > 0.0F)
                    .orElse(fallbackFov);
        }
        float activeFov = viewZoomModelFov(views.active(), fallbackFov);
        if (views.pending() == null) {
            return activeFov;
        }
        float pendingFov = viewZoomModelFov(views.pending(), fallbackFov);
        return Mth.lerp(views.progress(), activeFov, pendingFov);
    }

    /// > 单个视图位的开镜模型 FOV：瞄具按其声明，本体视位取与倍率配套的 `zoom_model_fov`。
    private static float viewZoomModelFov(AimView view, float fallbackFov) {
        if (view instanceof AimView.ScopeView scope) {
            return resolveScopeZoomFov(scope.stack(), currentPrediction(), fallbackFov);
        }
        float fov = ((AimView.BodyView) view).zoomModelFov();
        return fov > 0.0F ? fov : fallbackFov;
    }

    /// > 解析指定瞄具的生效模型 FOV：无级进度（预测 → NBT，t=0 低倍 / t=1 高倍，在 zoom_fov_range 上
    /// > 从 maxFov 向 minFov 插值）→ 档位 views_fov[index % n] → fallback。无级范围的档位互斥：
    /// > 声明了无级范围时档位索引不再参与循环（初始进度由档位映射，见 [#resolveScopeZoomProgress]）。
    public static float resolveScopeZoomFov(ItemStack scope, @Nullable ZoomPrediction prediction, float fallbackFov) {
        AttachmentDisplayInstance display = scope.isEmpty() ? null : TimelessHelperC.getAttachmentDisplay(scope).orElse(null);
        if (display == null) {
            return fallbackFov;
        }
        float[] fovs = display.getViewsFov();
        float[] range = display.getZoomFovRange();
        float fov = fallbackFov;
        float progress = currentProgress(scope, prediction);
        if (progress >= 0.0F && range != null) {
            /// > t=0 低倍（fov 最大）→ t=1 高倍（fov 最小）
            fov = Mth.lerp(progress, range[1], range[0]);
        } else if (fovs != null && fovs.length > 0) {
            fov = fovs[currentIndex(scope, prediction) % fovs.length];
        }
        if (range != null) {
            fov = Mth.clamp(fov, range[0], range[1]);
        } else {
            fov = Mth.clamp(fov, 1.0F, 70.0F);
        }
        return fov;
    }

    /// > 解析指定瞄具的生效倍率（zoom，世界画面缩放用）：无级进度（预测 → NBT，t=0 低倍 / t=1 高倍，
    /// > 在 zoom_range 上从 minZoom 向 maxZoom 插值）→ 档位 zoom[index]（未声明按 70/views_fov 反推）
    /// > → fallback。与 [#resolveScopeZoomFov]（模型 FOV）是两个独立数据字段，共用同一无级进度。
    public static float resolveScopeZoom(ItemStack scope, @Nullable ZoomPrediction prediction, float fallbackZoom) {
        AttachmentDisplayInstance display = scope.isEmpty() ? null : TimelessHelperC.getAttachmentDisplay(scope).orElse(null);
        if (display == null) {
            return fallbackZoom;
        }
        float[] zooms = display.getZoom();
        float[] fovs = display.getViewsFov();
        float[] range = display.getZoomRange();
        float zoom = fallbackZoom;
        float progress = currentProgress(scope, prediction);
        if (progress >= 0.0F && range != null) {
            zoom = Mth.lerp(progress, range[0], range[1]);
        } else {
            int index = currentIndex(scope, prediction);
            if (zooms != null && zooms.length > 0) {
                zoom = zooms[index % zooms.length];
            } else if (fovs != null && fovs.length > 0) {
                zoom = (float) MathUtil.fovToMagnification(fovs[index % fovs.length], 70.0);
            }
        }
        if (range != null) {
            zoom = Mth.clamp(zoom, range[0], range[1]);
        } else {
            zoom = Math.max(0.1F, zoom);
        }
        return zoom;
    }

    /// > 当前生效的无级进度（预测 → NBT）；未启用无级返回 -1。
    public static float resolveScopeZoomProgress(ItemStack scope, @Nullable ZoomPrediction prediction) {
        return currentProgress(scope, prediction);
    }

    /// > 无级模型 FOV 范围；缺省由无级倍率范围反推（70/zoom）。返回 null 表示两个范围都未声明。
    @Nullable
    public static float[] resolveSmoothFovRange(AttachmentDisplayInstance display) {
        float[] fovRange = display.getZoomFovRange();
        if (fovRange != null) {
            return fovRange;
        }
        float[] zoomRange = display.getZoomRange();
        if (zoomRange == null) {
            return null;
        }
        float min = (float) MathUtil.magnificationToFov(zoomRange[1], 70.0);
        float max = (float) MathUtil.magnificationToFov(zoomRange[0], 70.0);
        return min < max ? new float[]{min, max} : null;
    }

    /// > 无级倍率范围；缺省由无级模型 FOV 范围反推（70/fov）。返回 null 表示两个范围都未声明。
    @Nullable
    public static float[] resolveSmoothZoomRange(AttachmentDisplayInstance display) {
        float[] zoomRange = display.getZoomRange();
        if (zoomRange != null) {
            return zoomRange;
        }
        float[] fovRange = display.getZoomFovRange();
        if (fovRange == null) {
            return null;
        }
        float min = (float) MathUtil.fovToMagnification(fovRange[1], 70.0);
        float max = (float) MathUtil.fovToMagnification(fovRange[0], 70.0);
        return min < max ? new float[]{min, max} : null;
    }

    /// > 开镜世界目标 FOV（世界视野缩放 + 灵敏度基准）：
    /// > 有瞄具时按其生效倍率（档位 zoom / 无级进度映射，独立于模型 FOV 的配套字段）从世界 FOV 换算；
    /// > 机瞄时按 iron_zoom 从世界 FOV 换算。
    public static float getAimingWorldTargetFov(ItemStack stack, double baseWorldFov) {
        ViewTransition views = getViewTransition(stack);
        if (views.active() == null) {
            return (float) MathUtil.magnificationToFov(getAimingZoom(stack), baseWorldFov);
        }
        float fallbackZoom = getAimingZoom(stack);
        float activeZoom = viewZoom(views.active(), fallbackZoom);
        float activeFov = (float) MathUtil.magnificationToFov(activeZoom, baseWorldFov);
        if (views.pending() == null) {
            return activeFov;
        }
        float pendingZoom = viewZoom(views.pending(), fallbackZoom);
        float pendingFov = (float) MathUtil.magnificationToFov(pendingZoom, baseWorldFov);
        return Mth.lerp(views.progress(), activeFov, pendingFov);
    }

    /// > 单个视图位的世界倍率：瞄具按其声明（含档位 / 无级），本体视位恒为单档。
    private static float viewZoom(AimView view, float fallbackZoom) {
        if (view instanceof AimView.ScopeView scope) {
            return resolveScopeZoom(scope.stack(), currentPrediction(), fallbackZoom);
        }
        float zoom = ((AimView.BodyView) view).zoom();
        return zoom > 0.0F ? zoom : fallbackZoom;
    }

    /// > 无级进度（预测优先，其次配件 NBT）；未启用返回 -1。预测持续生效直到 operator 对账清除。
    private static float currentProgress(ItemStack scope, @Nullable ZoomPrediction prediction) {
        if (prediction != null && prediction.zoomProgress() >= 0.0F) {
            return Mth.clamp(prediction.zoomProgress(), 0.0F, 1.0F);
        }
        if (ZoomHelper.hasZoomProgress(scope)) {
            return ZoomHelper.getZoomProgress(scope);
        }
        return -1.0F;
    }

    /// > 档位索引（预测优先，其次配件 NBT）。
    private static int currentIndex(ItemStack scope, @Nullable ZoomPrediction prediction) {
        if (prediction != null) {
            return Math.max(0, prediction.zoomIndex());
        }
        return ZoomHelper.getZoomIndex(scope);
    }

    @Nullable
    private static ZoomPrediction currentPrediction() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        return IGunOperator.asGunHolder(player)
                .filter(op -> op instanceof LocalGunOperator)
                .map(op -> ((LocalGunOperator) op).zoomPrediction(InteractionHand.MAIN_HAND))
                .orElse(null);
    }

    public static float easeInExpo(float x) {
        if (x <= 0.0F) {
            return 0.0F;
        }
        if (x >= 1.0F) {
            return 1.0F;
        }
        return (float) Math.pow(2.0, 10.0F * x - 10.0F);
    }

    private record ViewTransition(@Nullable AimView active, @Nullable AimView pending, float progress) {
        private static final ViewTransition NONE = new ViewTransition(null, null, 1.0F);
    }

    /// > 按视图位列表解析旧 / 新视图位，共用模型视点切换进度，而不是等激活索引落定才跳变 FOV。
    /// > 变倍预测只属于当前激活瞄具，目标瞄具始终读取自己的档位 / 无级进度；本体视位没有档位概念，
    /// > 恒取 display 声明的单档值。
    private static ViewTransition getViewTransition(ItemStack stack) {
        List<AimView> views = AimViewHelper.collect(stack);
        if (views.isEmpty()) {
            return ViewTransition.NONE;
        }
        LocalGunOperator.AimViewState state = LocalGunOperator.AimViewState.NONE;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            state = IGunOperator.asGunHolder(player)
                    .filter(op -> op instanceof LocalGunOperator)
                    .map(op -> ((LocalGunOperator) op).aimViewState(InteractionHand.MAIN_HAND))
                    .orElse(LocalGunOperator.AimViewState.NONE);
        }
        AimView active = views.get(Mth.clamp(state.activeIndex(), 0, views.size() - 1));
        AimView pending = state.pendingIndex() < 0 ? null
                : views.get(Mth.clamp(state.pendingIndex(), 0, views.size() - 1));
        return new ViewTransition(active, pending, Mth.clamp(state.progress(), 0.0F, 1.0F));
    }
}
