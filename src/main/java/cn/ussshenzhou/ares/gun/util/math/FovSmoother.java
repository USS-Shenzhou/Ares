package cn.ussshenzhou.ares.gun.util.math;

/// > 轻量 FOV 平滑器：指数逼近（一阶低通），主线程按帧推进（无后台线程 / 无跨线程竞争）。
/// >
/// > 相比 [SecondOrderDynamics]：单调无过冲、目标连续小变化时零延迟感（逼近速率恒定），
/// > 目标突变时快速收敛（1 - e^(-rate·dt)），适合「开镜曲线本来就平滑、切档/滚轮突变才需要过滤」的场景。
/// @author TACZ, USS_Shenzhou
public class FovSmoother {
    private float value;
    private final float rate;

    /// > @param initial 初始值
    /// >
    /// > @param rate    每秒逼近速率（约 25 时时间常数 40ms）
    public FovSmoother(float initial, float rate) {
        this.value = initial;
        this.rate = rate;
    }

    /// > 按帧时长逼近目标并返回新值。
    /// >
    /// > @param target 目标 FOV
    /// >
    /// > @param dt     上一帧时长（秒）
    public float get(float target, float dt) {
        float diff = target - value;
        if (Math.abs(diff) < 0.01F) {
            value = target;
        } else {
            value += diff * (float) (1.0 - Math.exp(-rate * dt));
        }
        return value;
    }

    /// > 当前平滑值。
    public float get() {
        return value;
    }

    /// > 直接置位（切离持枪 / 复位场景，避免残留滞后）。
    public void reset(float v) {
        value = v;
    }
}