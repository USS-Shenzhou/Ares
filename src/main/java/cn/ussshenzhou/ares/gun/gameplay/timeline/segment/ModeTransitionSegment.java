package cn.ussshenzhou.ares.gun.gameplay.timeline.segment;

import cn.ussshenzhou.ares.util.GunHolderState;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionGate;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.SegmentToken;
import cn.ussshenzhou.ares.gun.gameplay.timeline.Timer;

/// > 模式切换过渡轨（仅客户端表现锁）：收编旧的 `modeTransitionStartMs/EndMs` +
/// > `extendModeTransition`。
/// >
/// > 过渡期间禁止开火 / 换弹 / 检视 / 再切换（PRIMARY ↔ UNDER_BARREL）。过渡是纯客户端动画 + 输入锁，
/// > 服务端 moduleMode 立即翻转、不动网络协议。
/// >
/// > 时长由动画侧回填（[#extend(long)] 仅允许延长，不缩短）。过渡结束后轨道自然消失。
/// > 不投影 [GunHolderState]（过渡不是持枪者状态，纯门禁与进度）。
/// @author TACZ, USS_Shenzhou
public class ModeTransitionSegment extends ActionSegment {

    public static final String ID = "mode_transition";
    public static final SegmentToken<ModeTransitionSegment> TOKEN = SegmentToken.of(ID, ModeTransitionSegment.class);

    private final Timer timer = new Timer();

    private final long startWallMs = System.currentTimeMillis();
    private long progressDurationMs;

    public ModeTransitionSegment(long durationMs) {
        long d = Math.max(durationMs, 1);
        this.timer.start(d);
        this.progressDurationMs = d;
    }

    @Override
    public SegmentToken<ModeTransitionSegment> token() {
        return TOKEN;
    }

    @Override
    public boolean tick(long deltaMs) {
        if (timer.tick(deltaMs)) {
            finish();
            return false;
        }
        return true;
    }

    @Override
    public void constrain(ActionGate.Builder gate) {
        gate.denyShoot().denyReload().denyInspect().denySwitchMode();
    }

    /// > 动画侧拿到过渡动画实际时长后回填，延长过渡锁定窗口（仅允许延长，不缩短）。
    /// > `durationMs` 是过渡动画从起点算的完整时长；保留已经过的进度，不回跳。
    public void extend(long durationMs) {
        if (durationMs <= 0) {
            return;
        }
        if (durationMs > timer.durationMs()) {
            timer.setDuration(durationMs);
        }
        if (durationMs > progressDurationMs) {
            progressDurationMs = durationMs;
        }
    }

    /// > 过渡进度 0~1。动画侧据此驱动持枪姿态权重淡入淡出。
    /// >
    /// > 按墙上时钟计算而非 tick 推进的 [Timer#progress()]，避免 tick 粒度（~50ms）
    /// > 导致渲染每帧采样到同一量化值、姿态权重淡入淡出卡顿。
    public float progress() {
        if (progressDurationMs <= 0) {
            return 1.0f;
        }
        long elapsed = System.currentTimeMillis() - startWallMs;
        if (elapsed <= 0) {
            return 0.0f;
        }
        return Math.min(elapsed / (float) progressDurationMs, 1.0f);
    }

    @Override
    public String debugInfo() {
        return String.format("%.0f%% (%dms left)", progress() * 100, timer.remainingMs());
    }

    @Override
    public void cancel() {
        super.cancel();
        timer.stop();
    }
}
