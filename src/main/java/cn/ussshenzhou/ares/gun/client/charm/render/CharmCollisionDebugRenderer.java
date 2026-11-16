package cn.ussshenzhou.ares.gun.client.charm.render;

/* FIXME rendering
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import cn.ussshenzhou.ares.gun.client.charm.physics.CharmCollisionBox;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/// > 挂饰防穿模调试可视化：在锚点骨骼局部空间把枪体碰撞盒画成线框 OBB，并把质点探针球画成三轴十字。
/// > <p>
/// > 调用前 {@code poseStack} 须已进入锚点骨骼局部空间（与碰撞盒、质点位置同一空间，方块单位）。
/// > 仅在 {@link CharmRenderer#DEBUG_COLLISION} 打开时调用。
/// @author TACZ, USS_Shenzhou
public class CharmCollisionDebugRenderer {
    /// > OBB 12 条边的角点索引对；角点编号 (x?,y?,z?) 位掩码：bit0=x, bit1=y, bit2=z
    private static final int[][] EDGES = {
            /// > x 向边
            {0, 1}, {2, 3}, {4, 5}, {6, 7},
            /// > y 向边
            {0, 2}, {1, 3}, {4, 6}, {5, 7},
            /// > z 向边
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    /// > @param poseStack      已进入锚点骨骼局部空间
    /// > @param probeCenter    质点探针中心（锚点骨骼局部空间，方块）
    /// > @param probeRadius    探针半径（方块）
    public static void render(PoseStack poseStack, MultiBufferSource buffer,
                              List<CharmCollisionBox> boxes, Vector3f probeCenter, float probeRadius) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
        PoseStack.Pose pose = poseStack.last();

        /// > 碰撞盒线框（青色）
        for (CharmCollisionBox box : boxes) {
            Vector3f[] corners = new Vector3f[8];
            for (int i = 0; i < 8; i++) {
                corners[i] = box.corner(i);
            }
            for (int[] edge : EDGES) {
                line(consumer, pose, corners[edge[0]], corners[edge[1]], 0.2f, 0.9f, 1.0f);
            }
        }

        /// > 探针球：用三轴十字 + 对角线近似（黄色），长度 = 半径
        if (probeCenter != null && Float.isFinite(probeRadius) && probeRadius > 0.0f) {
            float r = probeRadius;
            crossLine(consumer, pose, probeCenter, new Vector3f(r, 0, 0), 1.0f, 0.85f, 0.1f);
            crossLine(consumer, pose, probeCenter, new Vector3f(0, r, 0), 1.0f, 0.85f, 0.1f);
            crossLine(consumer, pose, probeCenter, new Vector3f(0, 0, r), 1.0f, 0.85f, 0.1f);
        }
    }

    private static void crossLine(VertexConsumer consumer, PoseStack.Pose pose, Vector3f center, Vector3f half,
                                  float r, float g, float b) {
        Vector3f a = new Vector3f(center).sub(half);
        Vector3f c = new Vector3f(center).add(half);
        line(consumer, pose, a, c, r, g, b);
    }

    private static void line(VertexConsumer consumer, PoseStack.Pose pose, Vector3f from, Vector3f to,
                             float r, float g, float b) {
        Matrix4f mat = pose.pose();
        Matrix3f norm = pose.normal();
        Vector3f dir = new Vector3f(to).sub(from);
        if (dir.lengthSquared() < 1.0e-12f) {
            return;
        }
        dir.normalize();
        consumer.vertex(mat, from.x, from.y, from.z).color(r, g, b, 1.0f)
                .normal(norm, dir.x, dir.y, dir.z).endVertex();
        consumer.vertex(mat, to.x, to.y, to.z).color(r, g, b, 1.0f)
                .normal(norm, dir.x, dir.y, dir.z).endVertex();
    }
}
*/
