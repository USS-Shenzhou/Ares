package cn.ussshenzhou.ares.gun.gameplay.timeline;

/// > 轻量毫秒计时原语。
/// > <p>
/// > 收编散落在双端 operator 里的裸时间戳（{@code System.currentTimeMillis()} 直接比较）。
/// > 计时单位统一为毫秒，但推进由游戏 tick 驱动（每 tick 累加 {@code deltaMs}），
/// > 因此实际粒度仍是 tick，不受墙上时钟双端漂移影响。
/// > <p>
/// > 典型用法：
/// > <pre>
/// >   timer.start(500);            // 起一个 500ms 的窗口
/// >   boolean done = timer.tick(deltaMs);  // 每 tick 推进，窗口刚结束时返回 true
/// >   if (timer.isRunning()) ...   // 窗口期内
/// >   float p = timer.progress();  // 0~1 进度
/// > </pre>
/// @author TACZ, USS_Shenzhou
public class Timer {
    private long durationMs = 0;
    private long elapsedMs = 0;
    private boolean running = false;
    private boolean justFinished = false;

    /// > 启动计时。durationMs <= 0 时视为「瞬时完成」，下一次 tick 即结束。
    public void start(long durationMs) {
        this.durationMs = Math.max(durationMs, 0);
        this.elapsedMs = 0;
        this.running = true;
        this.justFinished = false;
    }

    /// > 推进计时。
    /// >
    /// > @param deltaMs 本 tick 经过的毫秒数
    /// > @return true 表示本次推进后计时刚好结束（仅在结束的那一拍返回 true）
    public boolean tick(long deltaMs) {
        this.justFinished = false;
        if (!running) {
            return false;
        }
        elapsedMs += Math.max(deltaMs, 0);
        if (elapsedMs >= durationMs) {
            running = false;
            justFinished = true;
            return true;
        }
        return false;
    }

    /// > 是否正在计时（已 start 且未结束）。
    public boolean isRunning() {
        return running;
    }

    /// > 本 tick 是否刚刚结束（由 {@link #tick(long)} 设置，下一次 tick 复位）。
    public boolean justFinished() {
        return justFinished;
    }

    /// > 进度 0~1。未启动或时长为 0 时返回 1。
    public float progress() {
        if (durationMs <= 0) {
            return 1.0f;
        }
        return Math.min(elapsedMs / (float) durationMs, 1.0f);
    }

    /// > 已经过的毫秒数。
    public long elapsedMs() {
        return elapsedMs;
    }

    /// > 计时总时长（毫秒）。
    public long durationMs() {
        return durationMs;
    }

    /// > 剩余毫秒数（未启动时为 0）。
    public long remainingMs() {
        if (!running) {
            return 0;
        }
        return Math.max(durationMs - elapsedMs, 0);
    }

    /// > 立即停止计时（不触发 justFinished）。
    public void stop() {
        this.running = false;
        this.justFinished = false;
    }

    /// > 在不重置已经过时间的前提下，把总时长调整为 newDurationMs（用于「延长窗口」语义，
    /// > 保留进度不回跳）。若调整后已超过新时长则视为仍未结束（保持 running，由下次 tick 判定）。
    public void setDuration(long newDurationMs) {
        this.durationMs = Math.max(newDurationMs, 0);
        if (elapsedMs < durationMs) {
            this.running = true;
            this.justFinished = false;
        }
    }
}
