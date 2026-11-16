package cn.ussshenzhou.ares.client.renderer;

/* FIXME rendering
import com.mojang.blaze3d.vertex.PoseStack;
*/
import org.joml.Matrix3f;

/// > 将完整仿射变换应用到 [PoseStack]，同时更新法线矩阵。
/// @author TACZ, USS_Shenzhou
public class PoseStackTransformHelper {
    /* FIXME rendering
    /// > 乘入位置矩阵及其逆转置法线矩阵。不可逆变换保留当前法线矩阵，避免写入非有限值。
    public static void mulPoseAndNormal(PoseStack poseStack, Matrix4f transform) {
        poseStack.mulPoseMatrix(transform);

        Matrix3f normal = new Matrix3f(transform);
        float determinant = normal.determinant();
        if (!Float.isFinite(determinant) || determinant == 0.0F) {
            return;
        }
        normal.invert().transpose();
        if (isFinite(normal)) {
            poseStack.last().normal().mul(normal);
        }
    }
    */

    private static boolean isFinite(Matrix3f matrix) {
        return Float.isFinite(matrix.m00()) && Float.isFinite(matrix.m01()) && Float.isFinite(matrix.m02())
                && Float.isFinite(matrix.m10()) && Float.isFinite(matrix.m11()) && Float.isFinite(matrix.m12())
                && Float.isFinite(matrix.m20()) && Float.isFinite(matrix.m21()) && Float.isFinite(matrix.m22());
    }
}
