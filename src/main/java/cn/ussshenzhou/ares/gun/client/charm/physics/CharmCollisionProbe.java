package cn.ussshenzhou.ares.gun.client.charm.physics;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/// > 碰撞探针：挂饰球探针在摆体局部的偏移与半径。
/// > <p>
/// > 自源挂饰模组移植（仅依赖 JOML）。本期物理只做自由摆动，探针主要用于阶段 4 的碰撞防穿模，
/// > 这里先建好类型，{@code stepWithCollisionProbes} 传空 box list 时不会产生穿模修正。
/// @author TACZ, USS_Shenzhou
public record CharmCollisionProbe(Vector3f offset, float radius) {
    public CharmCollisionProbe {
        offset = safeOffset(offset);
        radius = Math.max(0.0f, Float.isFinite(radius) ? radius : 0.0f);
    }

    public static CharmCollisionProbe centered(float radius) {
        return new CharmCollisionProbe(new Vector3f(), radius);
    }

    public CharmCollisionProbe rotated(Quaternionf rotation) {
        Vector3f rotatedOffset = new Vector3f(offset);
        if (rotation != null
                && Float.isFinite(rotation.x)
                && Float.isFinite(rotation.y)
                && Float.isFinite(rotation.z)
                && Float.isFinite(rotation.w)) {
            rotatedOffset.rotate(rotation);
        }
        return new CharmCollisionProbe(rotatedOffset, radius);
    }

    private static Vector3f safeOffset(Vector3f offset) {
        if (offset == null || !Float.isFinite(offset.x) || !Float.isFinite(offset.y) || !Float.isFinite(offset.z)) {
            return new Vector3f();
        }
        return new Vector3f(offset);
    }
}
