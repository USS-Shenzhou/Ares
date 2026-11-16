package cn.ussshenzhou.ares.client.charm.gui;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/// > 改装界面挂饰拾取：单个枪体 cube 的屏幕投影 + 射线相交记录。
/// >
/// > 算法对接 SimpleBedrockModel v2：cube 几何在 bake 后已是「骨骼局部空间 + 方块单位」
/// > （除 16 并减去 bone pivot）。本类把 cube 局部 AABB 连同
/// > `mvp = proj × viewModel × boneGlobal × cubeRotation` 一并持有，鼠标点击时逆 mvp
/// > 反投影出 cube 局部空间的 3D 射线，用 slab 法做射线-AABB 相交，命中后把命中点 / 法线经
/// > `cubeToBone` 变换回锚点骨骼局部空间（命中点再 ×16 转像素），产出与几何无关的
/// > `boneName + localHit + localNormal` 绑定契约。
/// @author TACZ, USS_Shenzhou
public class ProjectedGunCube {
    private static final float PIXELS_PER_BLOCK = 16.0f;

    private final String boneName;
    private final Matrix4f mvp;
    private final Matrix4f boneMvp;
    private final Matrix4f viewBone;
    private final Matrix4f cubeToBone;
    /// > 视图空间 → cube 局部空间（cube 局部 → 视图空间的逆），用于在视图空间构造射线后变回 cube 空间求交
    private final Matrix4f viewCubeInv;
    /// > 投影矩阵的 FOV / 离轴项（仅 x/y 相关，与 z 约定无关）：用于由鼠标 NDC 解析视图空间射线方向
    private final float projM00;
    private final float projM11;
    private final float projM20;
    private final float projM21;
    private final Vector3f localMin;
    private final Vector3f localMax;
    /// > 8 个角投影到 GUI 像素坐标（用于画线框）
    private final float[] cornerScreenX = new float[8];
    private final float[] cornerScreenY = new float[8];
    private final boolean[] cornerVisible = new boolean[8];

    /// > @param boneName   cube 所属骨骼名
    /// >
    /// > @param viewModel  poseStack.last().pose()（含相机 + 第一人称定位）
    /// >
    /// > @param boneGlobal 该骨骼当前全局变换（受动画影响）
    /// >
    /// > @param cubeToBone cube 自身旋转（translate pivot → rotate → translate -pivot）；无旋转为单位阵
    /// >
    /// > @param proj       投影矩阵
    /// >
    /// > @param localMin   cube 局部 AABB 最小角（方块）
    /// >
    /// > @param localMax   cube 局部 AABB 最大角（方块）
    /// >
    /// > @param guiW       GUI 宽（像素）
    /// >
    /// > @param guiH       GUI 高（像素）
    public ProjectedGunCube(String boneName, Matrix4f viewModel, Matrix4f boneGlobal, Matrix4f cubeToBone,
                            Matrix4f proj, Vector3f localMin, Vector3f localMax, float guiW, float guiH) {
        this.boneName = boneName;
        this.cubeToBone = new Matrix4f(cubeToBone);
        this.localMin = new Vector3f(localMin);
        this.localMax = new Vector3f(localMax);
        /// > mvp：cube 局部空间 → 裁剪空间
        this.mvp = new Matrix4f(proj).mul(viewModel).mul(boneGlobal).mul(cubeToBone);
        /// > boneMvp：锚点骨骼局部空间 → 裁剪空间（渲染端 localHit/localNormal 所在空间，用于一致的预览）
        this.boneMvp = new Matrix4f(proj).mul(viewModel).mul(boneGlobal);
        /// > viewBone：锚点骨骼局部空间 → 视图空间（相机在原点朝 -Z，z 约定确定，用于法线朝向判定）
        this.viewBone = new Matrix4f(viewModel).mul(boneGlobal);
        /// > viewCube：cube 局部空间 → 视图空间；其逆用于把视图空间射线变回 cube 局部空间求交
        this.viewCubeInv = new Matrix4f(viewModel).mul(boneGlobal).mul(cubeToBone).invert();
        /// > 仅取投影矩阵影响 x/y 的项（FOV + 离轴）。这样射线方向不依赖近/远平面 z 约定，
        /// > 兼容光影（Iris/Oculus）改用的 reversed-Z 投影——它仅改 z，前向投影线框因此仍正常，
        /// > 但原先用 z=-1/+1 反投影构造射线会整体朝相机背后，导致命中判定恒失败。
        this.projM00 = proj.m00();
        this.projM11 = proj.m11();
        this.projM20 = proj.m20();
        this.projM21 = proj.m21();
        computeCornerScreen(guiW, guiH);
    }

    public String boneName() {
        return boneName;
    }

    private void computeCornerScreen(float guiW, float guiH) {
        Vector3f[] corners = corners();
        for (int i = 0; i < 8; i++) {
            Vector4f clip = mvp.transform(new Vector4f(corners[i].x, corners[i].y, corners[i].z, 1.0f));
            if (clip.w() <= 1.0e-4f) {
                cornerVisible[i] = false;
                continue;
            }
            float ndcX = clip.x() / clip.w();
            float ndcY = clip.y() / clip.w();
            cornerScreenX[i] = (ndcX * 0.5f + 0.5f) * guiW;
            cornerScreenY[i] = (1.0f - (ndcY * 0.5f + 0.5f)) * guiH;
            cornerVisible[i] = true;
        }
    }

    /// > cube 局部 8 个角（方块单位），顺序与 `BedrockCube` 顶点常量一致。
    private Vector3f[] corners() {
        float x1 = localMin.x, y1 = localMin.y, z1 = localMin.z;
        float x2 = localMax.x, y2 = localMax.y, z2 = localMax.z;
        return new Vector3f[]{
                /// > 0 X1_Y1_Z1
                new Vector3f(x1, y1, z1),
                /// > 1 X2_Y1_Z1
                new Vector3f(x2, y1, z1),
                /// > 2 X2_Y2_Z1
                new Vector3f(x2, y2, z1),
                /// > 3 X1_Y2_Z1
                new Vector3f(x1, y2, z1),
                /// > 4 X1_Y1_Z2
                new Vector3f(x1, y1, z2),
                /// > 5 X2_Y1_Z2
                new Vector3f(x2, y1, z2),
                /// > 6 X2_Y2_Z2
                new Vector3f(x2, y2, z2),
                /// > 7 X1_Y2_Z2
                new Vector3f(x1, y2, z2)
        };
    }

    /// > 6 个面（按 `axis*2 + (sign>0?1:0)` 索引）的角点环路，顺序首尾相接构成 4 条边。
    /// > 角点编号与 [#corners()] 一致。
    private static final int[][] FACE_LOOPS = {
            /// > axis 0 (X) min 面 x=x1
            {0, 4, 7, 3},
            /// > axis 0 (X) max 面 x=x2
            {1, 5, 6, 2},
            /// > axis 1 (Y) min 面 y=y1
            {0, 1, 5, 4},
            /// > axis 1 (Y) max 面 y=y2
            {3, 2, 6, 7},
            /// > axis 2 (Z) min 面 z=z1
            {0, 1, 2, 3},
            /// > axis 2 (Z) max 面 z=z2
            {4, 5, 6, 7}
    };

    /// > 返回命中面 4 条边的屏幕端点数组：{x1,y1,x2,y2}，两端都可见才返回该边，否则该项为 null。
    /// >
    /// > @param axis 命中面所在轴（0=X / 1=Y / 2=Z）
    /// >
    /// > @param sign 命中面外法线在该轴上的符号（min 面 -1 / max 面 +1）
    public float[][] faceEdgeSegments(int axis, float sign) {
        int key = axis * 2 + (sign > 0.0f ? 1 : 0);
        int[] loop = FACE_LOOPS[key];
        float[][] result = new float[loop.length][];
        for (int i = 0; i < loop.length; i++) {
            int a = loop[i];
            int b = loop[(i + 1) % loop.length];
            if (cornerVisible[a] && cornerVisible[b]) {
                result[i] = new float[]{cornerScreenX[a], cornerScreenY[a], cornerScreenX[b], cornerScreenY[b]};
            }
        }
        return result;
    }

    /// > 把锚点骨骼局部空间的一点（方块单位）投影到 GUI 像素坐标；不可见返回 null。
    /// > 与挂饰渲染端 localHit/localNormal 所在空间一致，用于绘制与实际渲染朝向严格一致的预览。
    public float[] projectBoneToScreen(Vector3f bonePoint, float guiW, float guiH) {
        Vector4f clip = boneMvp.transform(new Vector4f(bonePoint.x, bonePoint.y, bonePoint.z, 1.0f));
        if (clip.w() <= 1.0e-4f) {
            return null;
        }
        float ndcX = clip.x() / clip.w();
        float ndcY = clip.y() / clip.w();
        return new float[]{(ndcX * 0.5f + 0.5f) * guiW, (1.0f - (ndcY * 0.5f + 0.5f)) * guiH};
    }

    /// > 鼠标射线-AABB 相交。命中返回 [Hit]（含锚点骨骼局部空间的命中点/法线），否则 null。
    /// >
    /// > 射线在视图空间构造：相机位于原点朝 -Z，由鼠标 NDC 经投影矩阵的 x/y 项（FOV + 离轴）
    /// > 解析出视图空间方向，再用 `viewCubeInv` 把原点与方向变换到 cube 局部空间求交。
    /// > 这样完全不依赖投影矩阵的近/远平面 z 约定，兼容光影（Iris/Oculus）的 reversed-Z 投影。
    public Hit hit(double mouseX, double mouseY, float guiW, float guiH) {
        float ndcX = (float) (mouseX / guiW) * 2.0f - 1.0f;
        float ndcY = 1.0f - (float) (mouseY / guiH) * 2.0f;
        /// > 透视投影：clip.x = projM00*vx + projM20*vz，clip.w = -vz；令 vz=-1 则 clip.w=1，
        /// > ndcX = projM00*vx - projM20 → vx = (ndcX + projM20) / projM00（vy 同理）。
        if (Math.abs(projM00) < 1.0e-9f || Math.abs(projM11) < 1.0e-9f) {
            return null;
        }
        float vx = (ndcX + projM20) / projM00;
        float vy = (ndcY + projM21) / projM11;
        /// > 视图空间：原点为相机，方向朝 -Z（指向场景）
        Vector3f viewOrigin = new Vector3f(0.0f, 0.0f, 0.0f);
        Vector3f viewDir = new Vector3f(vx, vy, -1.0f);
        /// > 变换到 cube 局部空间
        Vector3f origin = viewCubeInv.transformPosition(new Vector3f(viewOrigin));
        Vector3f dir = viewCubeInv.transformDirection(new Vector3f(viewDir));
        if (dir.lengthSquared() < 1.0e-12f) {
            return null;
        }
        return intersect(origin, dir);
    }

    /// > slab 法射线-AABB 相交（cube 局部空间）。法线取「进入面（tMin）的 AABB 外法线」，
    /// > 与源挂饰模组约定一致：每轴 min 面外法线为 -1、max 面外法线为 +1，进入面外法线天然朝外，
    /// > 不做任何朝相机/视图空间翻转。
    private Hit intersect(Vector3f origin, Vector3f dir) {
        float tMin = Float.NEGATIVE_INFINITY;
        float tMax = Float.POSITIVE_INFINITY;
        int hitAxis = -1;
        /// > 进入面外法线在该轴上的符号（min 面 -1 / max 面 +1）
        float hitSign = 0.0f;
        float[] o = {origin.x, origin.y, origin.z};
        float[] d = {dir.x, dir.y, dir.z};
        float[] min = {localMin.x, localMin.y, localMin.z};
        float[] max = {localMax.x, localMax.y, localMax.z};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(d[axis]) < 1.0e-9f) {
                /// > 射线与该轴 slab 平行：起点不在范围内即未命中
                if (o[axis] < min[axis] || o[axis] > max[axis]) {
                    return null;
                }
                continue;
            }
            float inv = 1.0f / d[axis];
            float tNear = (min[axis] - o[axis]) * inv;
            float tFar = (max[axis] - o[axis]) * inv;
            /// > 近面外法线默认是 min 面（-1）；若顺序翻转则近面变 max 面（+1）
            float nearSign = -1.0f;
            if (tNear > tFar) {
                float tmp = tNear;
                tNear = tFar;
                tFar = tmp;
                nearSign = 1.0f;
            }
            if (tNear > tMin) {
                tMin = tNear;
                hitAxis = axis;
                hitSign = nearSign;
            }
            if (tFar < tMax) {
                tMax = tFar;
            }
            if (tMin > tMax) {
                return null;
            }
        }
        /// > 要求进入面在相机前方：tMin < 0 表示相机已在 cube 内（或近面在背后），
        /// > 此时若 clamp 到 0 会把背向相机的「进入面」当成命中面，导致钉到远面，故丢弃。
        if (tMax < 0.0f || hitAxis < 0 || tMin < 0.0f) {
            return null;
        }
        float t = tMin;
        Vector3f hitLocal = new Vector3f(origin).add(new Vector3f(dir).mul(t));
        /// > 进入面的 AABB 外法线（cube 局部空间），符号取自 slab
        Vector3f normalLocal = new Vector3f();
        switch (hitAxis) {
            case 0 -> normalLocal.set(hitSign, 0.0f, 0.0f);
            case 1 -> normalLocal.set(0.0f, hitSign, 0.0f);
            default -> normalLocal.set(0.0f, 0.0f, hitSign);
        }
        /// > 统一在「锚点骨骼局部空间」处理：这是写进 NBT、渲染端真正消费的空间，
        /// > 预览/命中/法线全部在此空间求，杜绝 cube 局部与骨骼局部混用导致的方向不一致。
        Vector3f hitBone = cubeToBone.transformPosition(new Vector3f(hitLocal));
        Vector3f normalBone = cubeToBone.transformDirection(new Vector3f(normalLocal));
        if (normalBone.lengthSquared() > 1.0e-12f) {
            normalBone.normalize();
        }
        /// > 命中点转像素（骨骼空间）
        Vector3f localHitPixels = new Vector3f(hitBone).mul(PIXELS_PER_BLOCK);
        /// > 跨 cube 比较用视图空间深度：相机朝 -Z，视图 z 越大越近；取负使「越小越近」，配合 min 比较器
        float viewZ = viewBone.transformPosition(new Vector3f(hitBone)).z;
        float depth = -viewZ;
        return new Hit(boneName, localHitPixels, normalBone, new Vector3f(hitBone), depth, this, hitAxis, hitSign);
    }

    /// > 单次命中结果。坐标统一在锚点骨骼局部空间，与挂饰渲染端一致。
    /// >
    /// > @param boneName    命中骨骼名
    /// >
    /// > @param localHit    锚点骨骼局部命中点（像素，写入 NBT）
    /// >
    /// > @param localNormal 锚点骨骼局部命中面外法线（单位向量，写入 NBT）
    /// >
    /// > @param hitBone     锚点骨骼局部命中点（方块，用于 [#projectBoneToScreen] 绘制预览）
    /// >
    /// > @param distance    命中点的 NDC 深度（z/w，越小越近，用于跨 cube 排序）
    /// >
    /// > @param cube        命中的 cube（用于反投影绘制）
    /// >
    /// > @param hitAxis     命中面所在轴（0=X / 1=Y / 2=Z），用于 [#faceEdgeSegments] 只画命中面
    /// >
    /// > @param hitSign     命中面外法线在该轴上的符号（min 面 -1 / max 面 +1）
    public record Hit(String boneName, Vector3f localHit, Vector3f localNormal,
                      Vector3f hitBone, float distance, ProjectedGunCube cube,
                      int hitAxis, float hitSign) {
    }
}
