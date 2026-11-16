package cn.ussshenzhou.ares.gun.client.animation.mounted;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/// > 关键帧权重 provider：从 {@link OverlayWeightSpec} 的关键帧表按 section 进度线性插值。
/// > <p>
/// > 常数形态退化为单关键帧 {@code {0: c}}，任意进度恒返回该常数（M2 即用此路径）。
/// > 曲线形态按 {@link OverlayWeightContext#sectionProgress()} 在相邻关键帧间线性插值（M3 启用进度采样）。
/// @author TACZ, USS_Shenzhou
public class KeyframeWeightProvider implements OverlayWeightProvider {
    private final NavigableMap<Float, Float> points;

    public KeyframeWeightProvider(OverlayWeightSpec spec) {
        NavigableMap<Float, Float> map = new TreeMap<>();
        if (spec != null && spec.kind() == OverlayWeightSpec.Kind.KEYFRAMES && spec.keyframes() != null && !spec.keyframes().isEmpty()) {
            map.putAll(spec.keyframes());
        } else {
            /// > 常数（或缺省）退化为单关键帧。
            float value = spec != null ? spec.constant() : 1.0f;
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
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
