package cn.ussshenzhou.ares.client.refit;

/// > 改装视角聚焦的时间归一化缓动参数。
/// @author TACZ, USS_Shenzhou
public class RefitFocusSmoothing {
    public static final long TRANSITION_DURATION_NANOS = 250_000_000L;
    private static final double EASE_AT_60_FPS = 0.35d;
    private static final double REFERENCE_FPS = 60d;

    /// > 将经过时间转换为聚焦过渡进度，保持 60 FPS 下与旧版逐帧缓动相近的观感。
    public static float progressForElapsedNanos(long elapsedNanos) {
        long boundedElapsed = Math.max(0L, elapsedNanos);
        if (boundedElapsed >= TRANSITION_DURATION_NANOS) {
            return 1f;
        }
        double referenceFrames = boundedElapsed * REFERENCE_FPS / 1_000_000_000d;
        return (float) (1d - Math.pow(1d - EASE_AT_60_FPS, referenceFrames));
    }
}
