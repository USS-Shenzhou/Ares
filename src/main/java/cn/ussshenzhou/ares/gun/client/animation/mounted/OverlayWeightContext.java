package cn.ussshenzhou.ares.gun.client.animation.mounted;

/// > host_overlay 权重求值上下文。生产侧（provider）据此计算当前权重。
/// >
/// > M2 仅承载 section 进度（恒为 0，权重退化为常数）；M3 接入关键帧曲线采样、
/// > M4 接入 Molang query 上下文时在此扩展字段，不影响消费链路。
/// @author TACZ, USS_Shenzhou
public class OverlayWeightContext {
    /// > 当前 overlay section 播放进度，归一化到 [0,1]。M2 恒为 0。
    private float sectionProgress;

    public float sectionProgress() {
        return sectionProgress;
    }

    public void setSectionProgress(float sectionProgress) {
        this.sectionProgress = sectionProgress;
    }
}
