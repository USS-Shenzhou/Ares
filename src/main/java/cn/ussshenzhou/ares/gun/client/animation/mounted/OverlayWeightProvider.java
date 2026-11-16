package cn.ussshenzhou.ares.gun.client.animation.mounted;

import com.mojang.logging.LogUtils;

/// > host_overlay 权重生产侧抽象。消费侧（{@link HostOverlayRuntime}）每 tick 只问当前权重是多少，
/// > 不关心背后是常数、关键帧曲线还是 Molang 表达式。
/// > <p>
/// > 通过 {@link #from(OverlayWeightSpec)} 由 profile 声明构建对应实现，迁移成本集中在新增 provider。
/// @author TACZ, USS_Shenzhou
@FunctionalInterface
public interface OverlayWeightProvider {
    /// > 返回当前权重，结果 clamp 到 [0,1] 由调用方保证。
    float getWeight(OverlayWeightContext context);

    /// > 由多态权重声明构建 provider。
    /// > <p>
    /// > EXPRESSION（Molang）形态在 M4 接入，M2/M3 暂以常数 1.0 退化并告警，避免链路阻塞。
    static OverlayWeightProvider from(OverlayWeightSpec spec) {
        if (spec == null) {
            return ctx -> 1.0f;
        }
        switch (spec.kind()) {
            case CONSTANT:
            case KEYFRAMES:
                return new KeyframeWeightProvider(spec);
            case EXPRESSION:
            default:
                LogUtils.getLogger().warn("[TACZ] host_overlay Molang weight not supported yet, fallback to 1.0: {}", spec.expression());
                return ctx -> 1.0f;
        }
    }
}
