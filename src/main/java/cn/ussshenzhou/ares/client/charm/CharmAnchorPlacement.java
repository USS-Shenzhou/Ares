package cn.ussshenzhou.ares.client.charm;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/// > 挂饰锚点放置与朝向数学（仅依赖 JOML，端无关算法）。
/// >
/// > 自源模组移植并裁剪：去掉 legacy 偏移兼容分支，仅保留刚性渲染所需的「离面间隙锚点」与
/// > 「forward 轴对齐命中面法线」两类计算。坐标均在锚点骨骼的局部空间。
/// @author TACZ, USS_Shenzhou
public class CharmAnchorPlacement {
    private static final float DEFAULT_SURFACE_CLEARANCE_PIXELS = 0.32f;
    private static final float PIXELS_TO_BLOCKS = 1.0f / 16.0f;

    /// > 计算挂饰在锚点骨骼局部空间的初始位置（方块单位）。
    /// >
    /// > @param anchorBlocks            命中点（已 /16 转方块）
    /// >
    /// > @param storedLocalOffsetPixels 存储的局部附加偏移（像素），为空则按法线离面间隙
    /// >
    /// > @param localNormal             命中面法线
    /// >
    /// > @param surfaceClearancePixels  离面间隙（像素）
    public static Vector3f initialPosition(Vector3f anchorBlocks, Vector3f storedLocalOffsetPixels,
                                           Vector3f localNormal, float surfaceClearancePixels) {
        if (!finite(anchorBlocks)) {
            return new Vector3f();
        }
        if (finite(storedLocalOffsetPixels) && storedLocalOffsetPixels.lengthSquared() > 1.0E-6f) {
            return new Vector3f(anchorBlocks).add(new Vector3f(storedLocalOffsetPixels).mul(PIXELS_TO_BLOCKS));
        }
        if (!finite(localNormal) || localNormal.lengthSquared() < 1.0E-6f) {
            return new Vector3f(anchorBlocks);
        }
        return new Vector3f(anchorBlocks).add(clearanceOffsetPixels(localNormal, surfaceClearancePixels).mul(PIXELS_TO_BLOCKS));
    }

    private static Vector3f clearanceOffsetPixels(Vector3f localNormal, float surfaceClearancePixels) {
        float clearance = Float.isFinite(surfaceClearancePixels) ? Math.max(0.0f, surfaceClearancePixels) : DEFAULT_SURFACE_CLEARANCE_PIXELS;
        return new Vector3f(localNormal).normalize().mul(clearance);
    }

    /// > 把模型 forward 轴旋到命中面法线方向的四元数。
    public static Quaternionf surfaceOrientation(Vector3f sourceAxis, Vector3f localNormal) {
        if (!finite(localNormal) || localNormal.lengthSquared() < 1.0E-6f) {
            return new Quaternionf();
        }
        Vector3f source = finite(sourceAxis) && sourceAxis.lengthSquared() >= 1.0E-6f
                ? new Vector3f(sourceAxis).normalize()
                : new Vector3f(0.0f, 0.0f, 1.0f);
        Vector3f target = new Vector3f(localNormal).normalize();
        if (target.distanceSquared(source) < 1.0E-6f) {
            return new Quaternionf();
        }
        return new Quaternionf().rotationTo(source, target);
    }

    /// > 把模型空间的向量按「forward 轴对齐命中面法线」的朝向旋转到锚点骨骼局部空间。
    public static Vector3f orientVectorToSurface(Vector3f vector, Vector3f localNormal, Vector3f sourceAxis) {
        if (!finite(vector)) {
            return new Vector3f();
        }
        return new Vector3f(vector).rotate(surfaceOrientation(sourceAxis, localNormal));
    }

    private static boolean finite(Vector3f value) {
        return value != null && Float.isFinite(value.x) && Float.isFinite(value.y) && Float.isFinite(value.z);
    }
}
