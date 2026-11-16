package cn.ussshenzhou.ares.gun.client.render.fx;

/* FIXME rendering
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import cn.ussshenzhou.ares.gun.client.model.BedrockShellModel;
import cn.ussshenzhou.ares.gun.client.resource.display.GunDisplayInstance.ShellEjectionConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

/// > 抛壳的第一人称表现渲染（视图空间冻结帧）。
/// >
/// > 弹壳生成（[#spawn]）时冻结「当前帧第一人称完整 pose × 抛壳窗骨骼」的视图矩阵，弹壳在该冻结
/// > 坐标系内做匀变速直线运动 + 三轴匀角速度旋转（运动学移植自 legacy `ShellRender`）。冻结矩阵
/// > 由抛壳骨骼的视图变换给出，故弹壳的初始位置/朝向随枪体动画与持枪姿态自然同步；冻结后弹壳脱离枪体，
/// > 跟随相机但不再随枪摆动。
/// >
/// > 本渲染器为全局单例（仅本机玩家第一人称抛壳）。第三人称留待后续：抛壳事件依赖第一人称动画运行时，
/// > 第三人称需另走世界空间实体/粒子方案（见 [#spawnWorldStub]）。
/// @author TACZ, USS_Shenzhou
public class ShellRenderer {
    private static final ShellRenderer INSTANCE = new ShellRenderer();
    /// > 队列上限，溢出时丢弃最旧的弹壳。
    private static final int MAX_SHELLS = 128;

    private final Deque<ShellInstance> queue = new ConcurrentLinkedDeque<>();

    public static ShellRenderer getInstance() {
        return INSTANCE;
    }

    /// > 生成一枚弹壳。
    /// >
    /// > @param frozenViewTransform 冻结的视图矩阵（`第一人称完整 pose × 抛壳窗骨骼变换`）
    /// >
    /// > @param config              抛壳运动配置
    public void spawn(Matrix4f frozenViewTransform, ShellEjectionConfig config) {
        if (config == null || frozenViewTransform == null) {
            return;
        }
        if (queue.size() >= MAX_SHELLS) {
            queue.pollFirst();
        }
        Vector3f rv = config.randomVelocity();
        Vector3f randomOffset = new Vector3f(
                (float) (Math.random() * rv.x()),
                (float) (Math.random() * rv.y()),
                (float) (Math.random() * rv.z())
        );
        queue.offerLast(new ShellInstance(System.currentTimeMillis(), randomOffset, new Matrix4f(frozenViewTransform)));
    }

    /// > 渲染所有存活弹壳并剔除过期者。`config`/`model`/`texture` 任一为空则清空队列。
    /// >
    /// > @param packedLight 渲染光照（取枪体渲染光照）
    public void render(MultiBufferSource bufferSource, @Nullable BedrockShellModel model,
                       @Nullable Identifier texture, @Nullable ShellEjectionConfig config, int packedLight) {
        if (config == null || model == null || texture == null) {
            queue.clear();
            return;
        }
        long lifeMs = (long) (config.livingTime() * 1000);
        long now = System.currentTimeMillis();

        /// > 剔除过期弹壳（队列按生成时间升序，从队首开始清）。
        Iterator<ShellInstance> it = queue.iterator();
        while (it.hasNext()) {
            ShellInstance shell = it.next();
            if (now - shell.timestamp > lifeMs) {
                it.remove();
            }
        }

        RenderType quadType = RenderType.entityCutout(texture);
        RenderType triangleType = BedrockModelRenderTypes.polyMeshCutout(texture);
        for (ShellInstance shell : queue) {
            renderSingle(bufferSource, model, quadType, triangleType, config, shell, now, packedLight);
        }
    }

    private void renderSingle(MultiBufferSource bufferSource, BedrockShellModel model, RenderType quadType,
                              RenderType triangleType, ShellEjectionConfig config, ShellInstance shell,
                              long now, int packedLight) {
        double t = (now - shell.timestamp) / 1000.0;
        Vector3f iv = config.initialVelocity();
        Vector3f acc = config.acceleration();
        Vector3f av = config.angularVelocity();
        Vector3f off = shell.randomOffset;

        /// > 匀变速直线运动：s = (v0 + 随机偏移) * t + 0.5 * a * t²（在冻结的骨骼局部坐标系内，单位为模型单位）。
        double x = (iv.x() + off.x()) * t + 0.5 * acc.x() * t * t;
        double y = (iv.y() + off.y()) * t + 0.5 * acc.y() * t * t;
        double z = (iv.z() + off.z()) * t + 0.5 * acc.z() * t * t;

        PoseStack poseStack = new PoseStack();
        /// > 初始化到冻结的骨骼视图位置与朝向。
        poseStack.mulPoseMatrix(shell.transform);
        /// > 平移：这里的冻结矩阵已经处于新渲染管线的视图空间，需要反向映射回 display 参数的 legacy 语义。
        poseStack.translate(x, y, -z);
        /// > 三轴匀角速度旋转。
        poseStack.mulPose(Axis.XN.rotationDegrees((float) (av.x() * t)));
        poseStack.mulPose(Axis.YN.rotationDegrees((float) (av.y() * t)));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (av.z() * t)));

        model.renderToBuffer(poseStack, bufferSource, quadType, triangleType, packedLight, OverlayTexture.NO_OVERLAY);
    }

    public void clear() {
        queue.clear();
    }

    /// > 第三人称抛壳入口占位：依赖第一人称动画运行时的事件链路在此不可用，留待后续以世界空间方案实现。
    @SuppressWarnings("unused")
    private void spawnWorldStub() {
        /// > TODO: 第三人称/世界空间抛壳（独立于第一人称动画事件）。
    }

    /// > 单枚弹壳的运行时数据：生成时间戳、随机速度偏移、冻结的视图矩阵。
    private static class ShellInstance {
        final long timestamp;
        final Vector3f randomOffset;
        final Matrix4f transform;

        ShellInstance(long timestamp, Vector3f randomOffset, Matrix4f transform) {
            this.timestamp = timestamp;
            this.randomOffset = randomOffset;
            this.transform = transform;
        }
    }
}
*/
