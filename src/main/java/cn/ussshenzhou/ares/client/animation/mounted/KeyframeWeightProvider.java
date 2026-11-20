package cn.ussshenzhou.ares.client.animation.mounted;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/// > 关键帧权重 provider：从 [OverlayWeightCfg] 的关键帧表按 section 进度线性插值。
/// >
/// > 常数形态退化为单关键帧 `{0: c}`，任意进度恒返回该常数（M2 即用此路径）。
/// > 曲线形态按 [OverlayWeightContext#sectionProgress()] 在相邻关键帧间线性插值（M3 启用进度采样）。
/// @author TACZ, USS_Shenzhou
public class KeyframeWeightProvider implements OverlayWeightProvider {
    private final NavigableMap<Float, Float> points;

    public KeyframeWeightProvider(OverlayWeightCfg cfg) {
        NavigableMap<Float, Float> map = new TreeMap<>();
        if (cfg != null && cfg.kind() == OverlayWeightCfg.Kind.KEYFRAMES && cfg.keyframes() != null && !cfg.keyframes().isEmpty()) {
            map.putAll(cfg.keyframes());
        } else {
            /// > 常数（或缺省）退化为单关键帧。
            float value = cfg != null ? cfg.constant() : 1.0f;
            map.put(0.0f, value);
        }
        this.points = map;
    }

    @Override
    public float getWeight(OverlayWeightContext context) {
        float progress = context != null ? context.sectionProgress() : 0.0f;
        Map.Entry<Float, Float> floor = points.floorEntry(progress);
        Map.Entry<Float, Float> ceil = points.ceilingEntry(progress);
        if (floor == null) {
            return clamp(ceil != null ? ceil.getValue() : 1.0f);
        }
        if (ceil == null || floor.getKey().equals(ceil.getKey())) {
            return clamp(floor.getValue());
        }
        float span = ceil.getKey() - floor.getKey();
        if (span <= 0.0f) {
            return clamp(floor.getValue());
        }
        float t = (progress - floor.getKey()) / span;
        return clamp(floor.getValue() + (ceil.getValue() - floor.getValue()) * t);
    }

    private static float clamp(float value) {
        return Math.clamp(value, 0.0f, 1.0f);
    }
}
