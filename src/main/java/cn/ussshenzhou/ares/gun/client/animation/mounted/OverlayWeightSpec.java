package cn.ussshenzhou.ares.gun.client.animation.mounted;

import java.util.NavigableMap;
import java.util.TreeMap;

/// > host_overlay 权重声明（多态）。同一字段在 JSON 中可写成三种形态，由
/// > {@link cn.ussshenzhou.ares.gun.client.animation.mounted.serialize.OverlayWeightSpecSerializer} 判别解析：
/// > <pre>
/// >   "weight": 1.0                       -> {@link Kind#CONSTANT}
/// >   "weight": { "0.0": 0, "0.3": 1 }    -> {@link Kind#KEYFRAMES}（M3 曲线）
/// >   "weight": "query.reload_progress"   -> {@link Kind#EXPRESSION}（M4 Molang）
/// > </pre>
/// > 消费侧统一通过 {@link OverlayWeightProvider} 取值，不直接感知形态。
/// @author TACZ, USS_Shenzhou
public class OverlayWeightSpec {
    public enum Kind {
        CONSTANT,
        KEYFRAMES,
        EXPRESSION
    }

    private final Kind kind;
    private final float constant;
    private final NavigableMap<Float, Float> keyframes;
    private final String expression;

    private OverlayWeightSpec(Kind kind, float constant, NavigableMap<Float, Float> keyframes, String expression) {
        this.kind = kind;
        this.constant = constant;
        this.keyframes = keyframes;
        this.expression = expression;
    }

    public static OverlayWeightSpec constant(float value) {
        return new OverlayWeightSpec(Kind.CONSTANT, value, null, null);
    }

    public static OverlayWeightSpec keyframes(NavigableMap<Float, Float> points) {
        TreeMap<Float, Float> copy = new TreeMap<>(points);
        return new OverlayWeightSpec(Kind.KEYFRAMES, 0.0f, copy, null);
    }

    public static OverlayWeightSpec expression(String expression) {
        return new OverlayWeightSpec(Kind.EXPRESSION, 0.0f, null, expression);
    }

    public Kind kind() {
        return kind;
    }

    public float constant() {
        return constant;
    }

    public NavigableMap<Float, Float> keyframes() {
        return keyframes;
    }

    public String expression() {
        return expression;
    }
}
