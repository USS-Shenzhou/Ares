package cn.ussshenzhou.ares.client.charm.physics;

import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/// > 挂饰碰撞盒：带局部↔世界变换的 OBB（自源挂饰模组原样移植，仅依赖 JOML）。
/// >
/// > 阶段 4 碰撞防穿模会把枪体附近 cube 转成本类型；阶段 3 自由摆动暂不构造碰撞盒，
/// > 这里先建好类型保证物理层 API 完整。
/// @author TACZ, USS_Shenzhou
public class CharmCollisionBox {
    private final Vector3f min;
    private final Vector3f max;
    private final Matrix4f localToWorld;
    private final Matrix4f worldToLocal;
    private final int contactId;

    public CharmCollisionBox(Vector3f min, Vector3f max) {
        this(min, max, new Matrix4f(), 0);
    }

    public static CharmCollisionBox transformed(Vector3f min, Vector3f max, Matrix4f localToWorld) {
        return new CharmCollisionBox(min, max, localToWorld, 0);
    }

    public static CharmCollisionBox transformed(Vector3f min, Vector3f max, Matrix4f localToWorld, int contactId) {
        return new CharmCollisionBox(min, max, localToWorld, contactId);
    }

    private CharmCollisionBox(Vector3f min, Vector3f max, Matrix4f localToWorld, int contactId) {
        Vector3f a = min == null ? new Vector3f() : new Vector3f(min);
        Vector3f b = max == null ? new Vector3f() : new Vector3f(max);
        this.min = new Vector3f(
                Math.min(a.x, b.x),
                Math.min(a.y, b.y),
                Math.min(a.z, b.z)
        );
        this.max = new Vector3f(
                Math.max(a.x, b.x),
                Math.max(a.y, b.y),
                Math.max(a.z, b.z)
        );
        this.localToWorld = localToWorld == null ? new Matrix4f() : new Matrix4f(localToWorld);
        this.worldToLocal = new Matrix4f(this.localToWorld);
        if (finiteMatrix(this.worldToLocal) && Math.abs(this.worldToLocal.determinant()) > 1.0E-6f) {
            this.worldToLocal.invert();
        } else {
            this.worldToLocal.identity();
        }
        this.contactId = contactId;
    }

    public Vector3f min() {
        return new Vector3f(min);
    }

    public Vector3f max() {
        return new Vector3f(max);
    }

    public float minX() {
        return min.x;
    }

    public float minY() {
        return min.y;
    }

    public float minZ() {
        return min.z;
    }

    public float maxX() {
        return max.x;
    }

    public float maxY() {
        return max.y;
    }

    public float maxZ() {
        return max.z;
    }

    public boolean finite() {
        return finite(min) && finite(max) && finiteMatrix(localToWorld) && finiteMatrix(worldToLocal);
    }

    public int contactId() {
        return contactId;
    }

    public Vector3f closestPoint(Vector3f point) {
        Vector3f local = localPosition(point);
        return worldPosition(new Vector3f(
                Mth.clamp(local.x, min.x, max.x),
                Mth.clamp(local.y, min.y, max.y),
                Mth.clamp(local.z, min.z, max.z)
        ));
    }

    public boolean contains(Vector3f point) {
        return containsLocal(localPosition(point));
    }

    public boolean containsLocal(Vector3f point) {
        return point != null
                && point.x >= min.x && point.x <= max.x
                && point.y >= min.y && point.y <= max.y
                && point.z >= min.z && point.z <= max.z;
    }

    public float distanceSquaredTo(Vector3f point) {
        Vector3f closest = closestPoint(point);
        return closest.distanceSquared(point);
    }

    public Vector3f localPosition(Vector3f point) {
        return transformPosition(worldToLocal, point);
    }

    public Vector3f worldPosition(Vector3f localPosition) {
        return transformPosition(localToWorld, localPosition);
    }

    public Vector3f localDirection(Vector3f direction) {
        return transformDirection(worldToLocal, direction);
    }

    public Vector3f worldDirection(Vector3f direction) {
        return transformDirection(localToWorld, direction);
    }

    public void localPosition(Vector3f point, Vector3f destination) {
        transformPosition(worldToLocal, point, destination);
    }

    public void worldPosition(Vector3f localPosition, Vector3f destination) {
        transformPosition(localToWorld, localPosition, destination);
    }

    public void localDirection(Vector3f direction, Vector3f destination) {
        transformDirection(worldToLocal, direction, destination);
    }

    public void worldDirection(Vector3f direction, Vector3f destination) {
        transformDirection(localToWorld, direction, destination);
    }

    public Vector3f corner(int index) {
        return worldPosition(new Vector3f(
                (index & 1) == 0 ? min.x : max.x,
                (index & 2) == 0 ? min.y : max.y,
                (index & 4) == 0 ? min.z : max.z
        ));
    }

    public Matrix4f localToWorldMatrix() {
        return new Matrix4f(localToWorld);
    }

    private static Vector3f transformPosition(Matrix4f matrix, Vector3f point) {
        Vector3f result = new Vector3f();
        transformPosition(matrix, point, result);
        return result;
    }

    private static void transformPosition(Matrix4f matrix, Vector3f point, Vector3f destination) {
        destination.set(
                matrix.m00() * point.x + matrix.m10() * point.y + matrix.m20() * point.z + matrix.m30(),
                matrix.m01() * point.x + matrix.m11() * point.y + matrix.m21() * point.z + matrix.m31(),
                matrix.m02() * point.x + matrix.m12() * point.y + matrix.m22() * point.z + matrix.m32()
        );
    }

    private static Vector3f transformDirection(Matrix4f matrix, Vector3f direction) {
        Vector3f result = new Vector3f();
        transformDirection(matrix, direction, result);
        return result;
    }

    private static void transformDirection(Matrix4f matrix, Vector3f direction, Vector3f destination) {
        destination.set(
                matrix.m00() * direction.x + matrix.m10() * direction.y + matrix.m20() * direction.z,
                matrix.m01() * direction.x + matrix.m11() * direction.y + matrix.m21() * direction.z,
                matrix.m02() * direction.x + matrix.m12() * direction.y + matrix.m22() * direction.z
        );
    }

    private static boolean finite(Vector3f value) {
        return value != null && Float.isFinite(value.x) && Float.isFinite(value.y) && Float.isFinite(value.z);
    }

    private static boolean finiteMatrix(Matrix4f matrix) {
        return matrix != null
                && Float.isFinite(matrix.m00()) && Float.isFinite(matrix.m01()) && Float.isFinite(matrix.m02()) && Float.isFinite(matrix.m03())
                && Float.isFinite(matrix.m10()) && Float.isFinite(matrix.m11()) && Float.isFinite(matrix.m12()) && Float.isFinite(matrix.m13())
                && Float.isFinite(matrix.m20()) && Float.isFinite(matrix.m21()) && Float.isFinite(matrix.m22()) && Float.isFinite(matrix.m23())
                && Float.isFinite(matrix.m30()) && Float.isFinite(matrix.m31()) && Float.isFinite(matrix.m32()) && Float.isFinite(matrix.m33());
    }
}
