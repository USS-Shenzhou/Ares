package cn.ussshenzhou.ares.gun.client.animation.statemachine.source;

/// > 轨道进度控制句柄。仅常规 runner 后端（{@link AnimationRunnerSource}）真实支持，
/// > 用于 manual / locomotion 轨道每 tick 强设相位。
/// > <p>
/// > montage 后端不支持外部任意强设进度（进度由 section 内部管理），其 {@link PoseSource#progress()}
/// > 返回 {@link #NONE}，调用 {@link #setProgress} 将被忽略。
/// @author TACZ, USS_Shenzhou
public interface IProgressControl {
    /// > 不支持外部强设进度的空实现。montage 后端返回此实例。
    IProgressControl NONE = new IProgressControl() {
        @Override
        public boolean isSupported() {
            return false;
        }

        @Override
        public void setProgress(long progress) {
        }

        @Override
        public long getMaxProgress() {
            return 0L;
        }
    };

    /// > 是否支持外部强设进度。
    boolean isSupported();

    /// > 设置当前进度（纳秒）。不支持时无副作用。
    void setProgress(long progress);

    /// > 动画总时长（纳秒）。不支持时返回 0。
    long getMaxProgress();
}
