package cn.ussshenzhou.ares.gun.client.refit;

import cn.ussshenzhou.ares.gun.item.AttachmentPath;
import cn.ussshenzhou.ares.gun.util.math.SecondOrderDynamics;
import net.minecraft.util.Mth;
import javax.annotation.Nullable;

/// > 改装界面的客户端渲染状态（单例）。
/// > <p>
/// > 复用第一人称枪械渲染：改装时枪继续在世界里渲染，但定位变换插值到模型的
/// > {@code refit_view}（总览）或选中槽位的 {@code refit_<slot>_view} 骨骼，
/// > 使枪铺满屏幕并聚焦到对应部位。GUI（{@link cn.ussshenzhou.ares.gun.client.gui.GunRefitScreen}）
/// > 仅作透明覆盖层。
/// > <p>
/// > 开启进度由二阶动力学平滑驱动：打开界面 0→1（枪移入改装位），关闭 1→0（移回待机位）。
/// > 槽位聚焦的平滑由渲染端对目标矩阵做逐帧缓动完成（见 BedrockGunItemRenderer）。
/// @author TACZ, USS_Shenzhou
public class RefitTransform {
    /// > 与原渲染器中预留的 REFIT_OPENING_DYNAMICS 参数一致
    private static final SecondOrderDynamics OPENING = new SecondOrderDynamics(1f, 1.2f, 0.5f, new float[]{0});
    /// > 拖拽旋转回弹动力学：index 0 = yaw（度），index 1 = pitch（度）
    /// > f 越小越慢，z>=1 不过冲、更柔和
    private static final SecondOrderDynamics DRAG = new SecondOrderDynamics(0.7f, 1.1f, 0f, new float[]{0, 0});
    /// > 松手后延迟多久才开始回弹（毫秒）
    private static final long RELEASE_DELAY_MS = 400L;
    /// > 松手回弹的计划开始时刻；<0 表示无待回弹
    private static long springBackAt = -1L;

    /// > 拖拽时累积的目标旋转角（度）；松手后目标归零，由 DRAG 平滑回弹
    private static float dragYaw = 0f;
    private static float dragPitch = 0f;
    /// > pitch 限幅，避免翻过头
    private static final float PITCH_LIMIT = 60f;
    /// > 鼠标位移到角度的灵敏度（度/像素）
    private static final float DRAG_SENSITIVITY = 0.5f;

    private static boolean active = false;
    /// > 当前聚焦的槽位路径；null 或空表示总览（refit_view）。可指向嵌套子槽位。
    @Nullable
    private static AttachmentPath focusPath = null;

    /// > 进入改装态。
    public static void open() {
        active = true;
        focusPath = null;
        dragYaw = 0f;
        dragPitch = 0f;
        springBackAt = -1L;
        DRAG.target(0, 0f);
        DRAG.target(1, 0f);
        OPENING.target(0, 1f);
    }

    /// > 退出改装态（枪平滑移回待机位）。
    public static void close() {
        active = false;
        focusPath = null;
        /// > 关闭时立即复位拖拽（不延迟），随枪一起平滑移回待机位
        dragYaw = 0f;
        dragPitch = 0f;
        springBackAt = -1L;
        DRAG.target(0, 0f);
        DRAG.target(1, 0f);
        OPENING.target(0, 0f);
    }

    public static boolean isActive() {
        return active;
    }

    /// > 设置聚焦槽位路径；null 或空回到总览视角。可指向嵌套子槽位。
    public static void setFocusPath(@Nullable AttachmentPath path) {
        focusPath = (path == null || path.isEmpty()) ? null : path;
    }

    @Nullable
    public static AttachmentPath getFocusPath() {
        return focusPath;
    }

    /// > 当前开启进度（0=待机位，1=改装位），已做平滑与夹取。
    public static float getOpeningProgress() {
        return Mth.clamp(OPENING.get(0), 0f, 1f);
    }

    /// > 是否已完全关闭且稳定（用于渲染端重置缓动状态）。
    public static boolean isClosedAndStable() {
        return !active && getOpeningProgress() < 1.0e-3f;
    }

    /// > ============================ 拖拽旋转 ============================

    /// > 累加一次拖拽位移（鼠标拖动），更新目标旋转角并即时跟随。
    /// >
    /// > @param dx 鼠标 x 位移（像素，向右为正）→ yaw
    /// > @param dy 鼠标 y 位移（像素，向下为正）→ pitch
    public static void addDrag(double dx, double dy) {
        dragYaw += (float) dx * DRAG_SENSITIVITY;
        dragPitch = Mth.clamp(dragPitch + (float) dy * DRAG_SENSITIVITY, -PITCH_LIMIT, PITCH_LIMIT);
        /// > 拖拽期间目标 = 当前累积角，dynamics 平滑跟随手指
        springBackAt = -1L;
        DRAG.target(0, dragYaw);
        DRAG.target(1, dragPitch);
    }

    /// > 松手：保持当前角度，延迟 {@link #RELEASE_DELAY_MS} 后再开始平滑回弹。
    /// > 实际把 target 置零的动作由 {@link #pollSpringBack()} 在延迟到点时执行。
    public static void releaseDrag() {
        /// > 目标仍停在松手时的角度，先不回弹
        DRAG.target(0, dragYaw);
        DRAG.target(1, dragPitch);
        springBackAt = System.currentTimeMillis() + RELEASE_DELAY_MS;
    }

    /// > 保持当前拖拽角度，取消任何待回弹（不自动回正）。用于挂饰绑定态：让玩家自由转动枪体
    /// > 在各个面上拾取吊挂点，松手后视角不应自动复位。
    public static void holdDrag() {
        springBackAt = -1L;
        DRAG.target(0, dragYaw);
        DRAG.target(1, dragPitch);
    }

    /// > 检查松手延迟是否到点，到点则把回弹目标置零。每帧轮询（取角度时调用）。
    private static void pollSpringBack() {
        if (springBackAt >= 0 && System.currentTimeMillis() >= springBackAt) {
            springBackAt = -1L;
            dragYaw = 0f;
            dragPitch = 0f;
            DRAG.target(0, 0f);
            DRAG.target(1, 0f);
        }
    }

    /// > 当前平滑后的拖拽 yaw（度，绕竖直轴）。
    public static float getDragYaw() {
        pollSpringBack();
        return DRAG.get(0);
    }

    /// > 当前平滑后的拖拽 pitch（度，绕水平轴）。
    public static float getDragPitch() {
        pollSpringBack();
        return DRAG.get(1);
    }
}
