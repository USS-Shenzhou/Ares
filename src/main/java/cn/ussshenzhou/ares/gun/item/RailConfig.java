package cn.ussshenzhou.ares.gun.item;

import org.joml.Vector3f;

/// > 导轨微调配置。
/// > <p>
/// > 声明某个 {@link AttachmentSlot} 上已安装配件可沿导轨方向滑动的范围。
/// > 偏移是沿 {@link #axis} 单位方向的一个标量（单位：模型空间，1.0 = 16 像素 = 1 格）。
/// > <p>
/// > 第一阶段仅支持单轴平移；旋转 / 多轴留作后续扩展。
/// >
/// > @param axis 导轨方向（已归一化的单位向量）
/// > @param min  允许的最小偏移（沿 axis）
/// > @param max  允许的最大偏移（沿 axis）
/// > @param step 界面步进；&lt;= 0 表示连续可调
/// @author TACZ, USS_Shenzhou
public record RailConfig(Vector3f axis, float min, float max, float step) {
    public RailConfig {
        /// > 归一化方向；非法（零向量）时回退到 Z 轴
        Vector3f normalized = new Vector3f(axis);
        if (normalized.lengthSquared() < 1.0e-8f) {
            normalized.set(0, 0, 1);
        } else {
            normalized.normalize();
        }
        axis = normalized;
        /// > 保证 min <= max
        if (min > max) {
            float tmp = min;
            min = max;
            max = tmp;
        }
    }

    /// > 将给定偏移夹取到 [min, max] 范围内。
    public float clamp(float offset) {
        return Math.max(min, Math.min(max, offset));
    }

    /// > 偏移是否落在合法范围内（含端点，带一点浮点容差）。
    public boolean inRange(float offset) {
        return offset >= min - 1.0e-4f && offset <= max + 1.0e-4f;
    }

    /// > 沿导轨方向计算偏移向量（offset * axis）。
    public Vector3f offsetVector(float offset, Vector3f dest) {
        return dest.set(axis).mul(offset);
    }
}
