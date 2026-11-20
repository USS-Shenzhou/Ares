package cn.ussshenzhou.ares.client.animation.mounted;

import com.mojang.logging.LogUtils;

/// > host_overlay 权重生产侧抽象。消费侧（[HostOverlayRuntime]）每 tick 只问当前权重是多少，
/// > 不关心背后是常数、关键帧曲线还是 Molang 表达式。
/// >
/// > 通过 [#from(OverlayWeightCfg)] 由 profile 声明构建对应实现，迁移成本集中在新增 provider。
/// @author TACZ, USS_Shenzhou
@FunctionalInterface
public interface OverlayWeightProvider {
    /// > 返回当前权重，结果 clamp 到 [0,1] 由调用方保证。
    float getWeight(OverlayWeightContext context);

    /// > 由多态权重声明构建 provider。
    /// >
    /// > EXPRESSION（Molang）形态在 M4 接入，M2/M3 暂以常数 1.0 退化并告警，避免链路阻塞。
    static OverlayWeightProvider from(OverlayWeightCfg cfg) {
        if (cfg == null) {
            return ctx -> 1.0f;
        }
        return switch (cfg.kind()) {
            case CONSTANT, KEYFRAMES -> new KeyframeWeightProvider(cfg);
            default -> {
                LogUtils.getLogger().warn("[TACZ] host_overlay Molang weight not supported yet, fallback to 1.0: {}", cfg.expression());
                yield ctx -> 1.0f;
            }
        };
    }
}
