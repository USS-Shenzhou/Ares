package cn.ussshenzhou.ares.client.render.fx;

/* FIXME rendering
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import cn.ussshenzhou.ares.gun.gameplay.LaserHitType;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/// > 镭射（激光指示器）世界空间表现渲染。
/// >
/// > 与曳光弹同类，属于世界空间自发光几何，走 [RenderType#entityTranslucentEmissive]，
/// > 不涉及任何后处理（手电那套 PostChain/深度重建仅用于「照亮已有表面」，镭射只是自发光）。
/// >
/// > 表现要素：
/// > - 从起点沿方向做 raycast（方块 + 实体），命中处画一个朝向相机的发光落点光斑；
/// > - 从起点到落点画一条细发光束，被墙/实体挡住的部分通过 raycast 直接裁掉，
/// >   束的可见度由环境光照决定（暗处更明显）。
/// >
/// > 渲染坐标：顶点位置取「世界坐标 - 相机坐标」，配合 `poseStack`（已含相机旋转），
/// > 这是 `RenderLevelStageEvent` 阶段绘制世界几何的标准做法。
/// @author TACZ, USS_Shenzhou
public class LaserRenderer {
    /// > 光束基础半宽（米），按到相机距离轻微放大避免远处消失。
    private static final float BEAM_HALF_WIDTH = 0.004f;
    /// > 立体光柱：内层白热芯半宽（米）。
    public static final float BEAM_CORE_HALF_WIDTH = 0.010f;
    /// > 立体光柱：外层辉光壳半宽（米）。
    public static final float BEAM_GLOW_HALF_WIDTH = 0.028f;
    /// > 落点光斑基础半径（米），按距离放大保持屏幕尺寸大致稳定。
    private static final float DOT_HALF_SIZE = 0.035f;
    /// > 落点法线偏移，避免与命中面 z-fighting。
    private static final float DOT_SURFACE_BIAS = 0.01f;
    /// > 第一人称落点 raycast 限频：约 30Hz。
    private static final long FIRST_PERSON_RAYCAST_INTERVAL_MS = 33L;

    private static long lastFirstPersonRaycastTimeMs = 0L;
    private static int lastFirstPersonShooterId = Integer.MIN_VALUE;
    private static float lastFirstPersonRange = -1.0f;
    private static HitInfo cachedFirstPersonHit = null;
    private static Vec3 smoothedFirstPersonDotView = null;

    /// > 第一人称镭射视图几何（计算一次，供光束 / 光斑分 pass 绘制）。
    public record FirstPersonLaserView(Vec3 beamStart, Vec3 beamEnd, Vec3 dotView, Vec3 normal, float beamAlpha,
                                       boolean hitSurface) {
    }

    /// > 计算第一人称镭射视图几何（raycast 缓存 + 落点平滑）。
    public static FirstPersonLaserView computeFirstPerson(Matrix4f providerViewTransform, Camera camera, Entity shooter,
                                                          float maxRange, float beamAlpha) {
        Vector3f viewStartVec = providerViewTransform.getTranslation(new Vector3f());
        Vector3f viewForwardVec = providerViewTransform.transformDirection(new Vector3f(0.0f, 0.0f, -1.0f));
        if (viewForwardVec.lengthSquared() < 1.0e-8f) {
            return null;
        }
        Vec3 viewStart = new Vec3(viewStartVec.x(), viewStartVec.y(), viewStartVec.z());
        Vec3 viewDir = new Vec3(viewForwardVec.x(), viewForwardVec.y(), viewForwardVec.z()).normalize();

        Vec3 worldStart = camera.getPosition().add(viewToWorld(viewStart, camera));
        Vec3 worldDir = viewToWorld(viewDir, camera).normalize();
        HitInfo hit = getCachedFirstPersonHit(shooter, worldStart, worldDir, maxRange);
        /// > raycast 只估算距离；光束终点始终沿当前帧 provider 原方向，避免 30Hz 命中点更新导致方向跳变。
        double distance = Math.min(maxRange, Math.max(0.05, worldStart.distanceTo(hit.pos())));
        Vec3 targetView = viewStart.add(viewDir.scale(distance));
        smoothedFirstPersonDotView = smoothDot(smoothedFirstPersonDotView, targetView, 0.35);
        /// > 未命中方块（normal 为空）时无落点光斑：不渲染悬空辉光，只保留光束。
        Vec3 normalView = hit.normal() == null ? null : worldVectorToView(hit.normal(), camera);
        return new FirstPersonLaserView(viewStart, targetView,
                hit.normal() == null ? null : smoothedFirstPersonDotView, normalView, beamAlpha,
                hit.normal() != null);
    }

    /// > 渲染镭射光束部分（视图空间直绘，加法混合）。
    public static void renderFirstPersonBeamPart(MultiBufferSource bufferSource, FirstPersonLaserView view, float[] color) {
        if (view == null || view.beamAlpha() <= 0.01f) {
            return;
        }
        renderBeamView(new Matrix4f(), bufferSource, view.beamStart(), view.beamEnd(), color, view.beamAlpha());
    }

    /// > 渲染镭射落点光斑部分（视图空间）。仅在命中方块表面（`hitSurface`）时渲染——未命中不画悬空辉光。
    public static void renderFirstPersonDotPart(MultiBufferSource bufferSource, FirstPersonLaserView view, float[] color) {
        if (view == null || !view.hitSurface()) {
            return;
        }
        renderDotOnPlane(new Matrix4f(), bufferSource, view.dotView(), view.normal(), color);
    }

    /// > 在两个给定世界点之间直接绘制激光束（不再内部 raycast）。
    /// >
    /// > 用于光束类射弹的脉冲表现：起点由客户端按射击者实时求出，终点是服务端权威命中点。
    /// > 激光束是强自发光能量光柱，以满亮度渲染（不像镭射指示器那样按环境光衰减）。
    /// >
    /// > @param poseStack    `RenderLevelStageEvent` 的 poseStack（已含相机旋转，未含相机平移）
    /// >
    /// > @param camera       主相机，用于取相机世界坐标与 billboard 朝向
    /// >
    /// > @param bufferSource 顶点缓冲来源
    /// >
    /// > @param worldStart   束流起点（世界坐标）
    /// >
    /// > @param worldEnd     束流终点 = 命中点（世界坐标）
    /// >
    /// > @param color        RGB 颜色（每分量 0~1）
    /// >
    /// > @param drawDot      是否在终点绘制落点光斑（本机视角绘制，其他玩家仅束）
    public static void renderBeamBetween(PoseStack poseStack, Camera camera, MultiBufferSource bufferSource,
                                         Vec3 worldStart, Vec3 worldEnd, float[] color, boolean drawDot) {
        renderBeamBetween(poseStack, camera, bufferSource, worldStart, worldEnd, color, drawDot,
                BEAM_CORE_HALF_WIDTH, BEAM_GLOW_HALF_WIDTH);
    }

    /// > 在两个给定世界点之间绘制激光束，芯/辉光半宽 + 截面滚转由调用方指定（display 视觉表达式驱动）。
    /// >
    /// > @param coreHalfWidth 内层白热芯半宽（米）
    /// >
    /// > @param glowHalfWidth 外层辉光壳半宽（米）
    public static void renderBeamBetween(PoseStack poseStack, Camera camera, MultiBufferSource bufferSource,
                                         Vec3 worldStart, Vec3 worldEnd, float[] color, boolean drawDot,
                                         float coreHalfWidth, float glowHalfWidth) {
        renderBeamBetween(poseStack, camera, bufferSource, worldStart, worldEnd, color, drawDot,
                coreHalfWidth, glowHalfWidth, 0.0f);
    }

    /// > 在两个给定世界点之间绘制激光束，芯/辉光半宽 + 截面滚转由调用方指定（display 视觉表达式驱动）。
    /// >
    /// > @param coreHalfWidth 内层白热芯半宽（米）
    /// >
    /// > @param glowHalfWidth 外层辉光壳半宽（米）
    /// >
    /// > @param rollDegrees   方柱截面绕束轴滚转角（度）
    public static void renderBeamBetween(PoseStack poseStack, Camera camera, MultiBufferSource bufferSource,
                                         Vec3 worldStart, Vec3 worldEnd, float[] color, boolean drawDot,
                                         float coreHalfWidth, float glowHalfWidth, float rollDegrees) {
        Vec3 axis = worldEnd.subtract(worldStart);
        if (axis.lengthSqr() < 1.0e-8) {
            return;
        }
        Vec3 cameraPos = camera.getPosition();
        Matrix4f pose = poseStack.last().pose();

        /// > 立体光柱：坐标先减相机位置（世界几何标准做法），再交给方柱发射器。
        Vec3 s = worldStart.subtract(cameraPos);
        Vec3 e = worldEnd.subtract(cameraPos);
        renderVolumetricBeam(pose, bufferSource, s, e, color, coreHalfWidth, glowHalfWidth, rollDegrees);

        if (drawDot) {
            Vec3 normal = axis.normalize().scale(-1.0);
            renderDotOnPlaneWorld(pose, bufferSource, cameraPos, worldEnd, normal, color);
        }
    }

    /// > 第一人称束流方向：枪口视觉方向混入权重（其余跟相机视线）。小权重给真实感又不飘。
    private static final double FP_MUZZLE_DIR_WEIGHT = 0.15;

    /// > 第一人称能量束视图几何（计算一次，供光束 / 光斑分 pass 绘制）。
    public record FirstPersonBeamView(Vec3 start, Vec3 end, boolean hitSurface) {
    }

    /// > 计算第一人称能量束视图几何：终点方向在「相机视线」与「枪口视觉方向」之间按小权重融合
    /// > （主体跟相机故无拖拽、不乱飘，混入少量枪口方向保留真实感），长度锁定真实命中距离
    /// > （命中点经相机转视图空间后与起点取距，避免跨坐标系混算）。
    /// >
    /// > `hitType`（见 [LaserHitType]）为服务端权威判定结果：落点光斑只在命中方块
    /// > 表面（`HIT_BLOCK`）时渲染，命中实体 / 射空不渲染悬空辉光。
    public static FirstPersonBeamView computeFirstPersonBeam(Camera camera, Matrix4f providerViewTransform, Vec3 worldHit,
                                                             byte hitType) {
        Vector3f viewStartVec = providerViewTransform.getTranslation(new Vector3f());
        Vec3 viewStart = new Vec3(viewStartVec.x(), viewStartVec.y(), viewStartVec.z());
        Vec3 viewHit = worldToView(worldHit, camera);
        if (viewStart.distanceTo(viewHit) < 1.0e-4) {
            return null;
        }

        /// > 相机视线方向（视图空间恒 -Z）与枪口视觉方向的加权融合。
        Vec3 cameraDir = new Vec3(0.0, 0.0, -1.0);
        Vector3f muzzleFwd = providerViewTransform.transformDirection(new Vector3f(0.0f, 0.0f, -1.0f));
        Vec3 muzzleDir = new Vec3(muzzleFwd.x(), muzzleFwd.y(), muzzleFwd.z());
        if (muzzleDir.lengthSqr() < 1.0e-8) {
            muzzleDir = cameraDir;
        } else {
            muzzleDir = muzzleDir.normalize();
        }
        Vec3 dir = cameraDir.lerp(muzzleDir, FP_MUZZLE_DIR_WEIGHT);
        if (dir.lengthSqr() < 1.0e-8) {
            dir = cameraDir;
        } else {
            dir = dir.normalize();
        }
        return new FirstPersonBeamView(viewStart, viewStart.add(dir.scale(viewStart.distanceTo(viewHit))),
                hitType == LaserHitType.HIT_BLOCK);
    }

    /// > 渲染能量束光束部分（视图空间直绘，立体方柱）。
    public static void renderFirstPersonBeamPart(MultiBufferSource bufferSource, FirstPersonBeamView view, float[] color,
                                                 float coreHalfWidth, float glowHalfWidth, float rollDegrees) {
        if (view == null) {
            return;
        }
        renderVolumetricBeam(new Matrix4f(), bufferSource, view.start(), view.end(), color,
                coreHalfWidth, glowHalfWidth, rollDegrees);
    }

    /// > 渲染能量束落点光斑部分（视图空间）。仅在终点贴住方块表面（`hitSurface`）时渲染。
    public static void renderFirstPersonDotPart(MultiBufferSource bufferSource, FirstPersonBeamView view, float[] color) {
        if (view == null || !view.hitSurface()) {
            return;
        }
        Vec3 normal = view.end().subtract(view.start()).normalize().scale(-1.0);
        renderDotOnPlane(new Matrix4f(), bufferSource, view.end(), normal, color);
    }

    /// > 立体激光光柱：双层方柱。
    /// >
    /// > 与镭射指示器的 billboard 细带不同，能量束需要体积感——用绕束轴的方形截面 4 面侧墙组成方柱，
    /// > 加法混合下侧墙相互叠加自然出中心更亮的体积效果。叠两层：
    /// > - 外层辉光壳：较粗、本色、低 alpha，做柔和辉光包边；
    /// > - 内层白热芯：较细、向白色提亮、高 alpha，做白热核心。
    /// >
    /// > 坐标空间由调用方决定（世界几何传「世界坐标 - 相机坐标」，视图几何传视图坐标）。
    private static void renderVolumetricBeam(Matrix4f pose, MultiBufferSource bufferSource,
                                             Vec3 start, Vec3 end, float[] color,
                                             float coreHalfWidth, float glowHalfWidth, float rollDegrees) {
        Vec3 axis = end.subtract(start);
        double length = axis.length();
        if (length < 1.0e-4) {
            return;
        }
        axis = axis.scale(1.0 / length);

        /// > 构造两个垂直于束轴的正交基向量，作为方形截面的两个边方向。
        Vec3 ref = Math.abs(axis.y) < 0.95 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u = axis.cross(ref).normalize();
        Vec3 v = axis.cross(u).normalize();

        /// > 滚转：让方形截面绕束轴旋转 rollDegrees（Rodrigues 旋转，axis 已归一）。
        if (rollDegrees != 0.0f) {
            double rad = Math.toRadians(rollDegrees);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            Vec3 ru = rotateAboutAxis(u, axis, cos, sin);
            Vec3 rv = rotateAboutAxis(v, axis, cos, sin);
            u = ru;
            v = rv;
        }

        VertexConsumer consumer = bufferSource.getBuffer(LaserRenderTypes.laserAdditive());

        /// > 外层辉光壳：本色、低 alpha。
        emitBoxColumn(consumer, pose, start, end, u, v, glowHalfWidth,
                color[0], color[1], color[2], 90);
        /// > 内层白热芯：向白色提亮、高 alpha。
        float cr = color[0] + (1.0f - color[0]) * 0.7f;
        float cg = color[1] + (1.0f - color[1]) * 0.7f;
        float cb = color[2] + (1.0f - color[2]) * 0.7f;
        emitBoxColumn(consumer, pose, start, end, u, v, coreHalfWidth,
                cr, cg, cb, 230);
    }

    /// > 绕单位轴 `axis` 旋转向量 `vec`（Rodrigues 公式）。cos/sin 由调用方预算。
    private static Vec3 rotateAboutAxis(Vec3 vec, Vec3 axis, double cos, double sin) {
        Vec3 cross = axis.cross(vec);
        double dot = axis.dot(vec);
        return vec.scale(cos)
                .add(cross.scale(sin))
                .add(axis.scale(dot * (1.0 - cos)));
    }

    /// > 沿 start→end 发射一根方形截面光柱（4 面侧墙）。截面由正交基 u/v 张成，半宽 halfWidth。
    /// > NO_CULL 下双面均可见，无需担心朝向。
    private static void emitBoxColumn(VertexConsumer consumer, Matrix4f pose, Vec3 start, Vec3 end,
                                      Vec3 u, Vec3 v, float halfWidth, float r, float g, float b, int alpha) {
        int ir = (int) (r * 255);
        int ig = (int) (g * 255);
        int ib = (int) (b * 255);
        Vec3 hu = u.scale(halfWidth);
        Vec3 hv = v.scale(halfWidth);

        /// > 方形截面四角（相对束轴中心）：(+u+v) (+u-v) (-u-v) (-u+v)
        Vec3 c0 = hu.add(hv);
        Vec3 c1 = hu.subtract(hv);
        Vec3 c2 = hu.scale(-1).subtract(hv);
        Vec3 c3 = hu.scale(-1).add(hv);

        /// > 4 面侧墙：相邻两角在 start / end 端各拉一条边围成矩形。
        emitQuad(consumer, pose, start, end, c0, c1, ir, ig, ib, alpha);
        emitQuad(consumer, pose, start, end, c1, c2, ir, ig, ib, alpha);
        emitQuad(consumer, pose, start, end, c2, c3, ir, ig, ib, alpha);
        emitQuad(consumer, pose, start, end, c3, c0, ir, ig, ib, alpha);
    }

    /// > 发射一面侧墙：start+ca → end+ca → end+cb → start+cb。
    private static void emitQuad(VertexConsumer consumer, Matrix4f pose, Vec3 start, Vec3 end,
                                 Vec3 ca, Vec3 cb, int r, int g, int b, int alpha) {
        Vec3 sa = start.add(ca);
        Vec3 ea = end.add(ca);
        Vec3 eb = end.add(cb);
        Vec3 sb = start.add(cb);
        emitVertex(consumer, pose, (float) sa.x, (float) sa.y, (float) sa.z, r, g, b, alpha, 0, 0);
        emitVertex(consumer, pose, (float) ea.x, (float) ea.y, (float) ea.z, r, g, b, alpha, 1, 0);
        emitVertex(consumer, pose, (float) eb.x, (float) eb.y, (float) eb.z, r, g, b, alpha, 1, 1);
        emitVertex(consumer, pose, (float) sb.x, (float) sb.y, (float) sb.z, r, g, b, alpha, 0, 1);
    }

    /// > 渲染一次镭射。
    /// >
    /// > @param poseStack  `RenderLevelStageEvent` 的 poseStack（已含相机旋转，未含相机平移）
    /// >
    /// > @param camera     主相机，用于取相机世界坐标与 billboard 朝向
    /// >
    /// > @param bufferSource 顶点缓冲来源
    /// >
    /// > @param worldStart 激光起点世界坐标
    /// >
    /// > @param worldDir   激光方向（世界空间，需已归一化）
    /// >
    /// > @param color      RGB 颜色（每分量 0~1）
    /// >
    /// > @param maxRange   最大射程（米）
    /// >
    /// > @param shooter    持枪者，raycast 时排除自身
    /// >
    /// > @param beamAlpha  光束可见度系数 [0,1]，由环境光照算出，亮处趋近 0
    /// >
    /// > @param drawDot    是否绘制落点光斑（本机视角绘制，其他玩家仅束）
    public static void render(PoseStack poseStack, Camera camera, MultiBufferSource bufferSource,
                              Vec3 worldStart, Vec3 worldDir, float[] color, float maxRange,
                              Entity shooter, float beamAlpha, boolean drawDot) {
        if (worldDir.lengthSqr() < 1.0e-8) {
            return;
        }
        Vec3 dir = worldDir.normalize();
        HitInfo hit = raycast(shooter.level(), shooter, worldStart, dir, maxRange);
        Vec3 hitPos = hit.pos();
        Vec3 hitNormal = hit.normal();

        Vec3 cameraPos = camera.getPosition();
        Matrix4f pose = poseStack.last().pose();

        /// > 2. 光束：起点到落点的细发光带，正对相机。
        if (beamAlpha > 0.01f) {
            renderBeam(pose, bufferSource, cameraPos, worldStart, hitPos, color, beamAlpha);
        }

        /// > 3. 落点光斑：依附命中面（沿命中法线构造平面），而不是朝向相机的 billboard。
        if (drawDot) {
            Vec3 normal = hitNormal != null ? hitNormal : dir.scale(-1.0);
            renderDotOnPlaneWorld(pose, bufferSource, cameraPos, hitPos, normal, color);
        }
    }

    /// > 绘制起点到落点的发光光束：构造一条始终正对相机的细带。
    private static void renderBeam(Matrix4f pose, MultiBufferSource bufferSource, Vec3 cameraPos,
                                   Vec3 start, Vec3 end, float[] color, float alpha) {
        Vec3 axis = end.subtract(start);
        double length = axis.length();
        if (length < 1.0e-4) {
            return;
        }
        axis = axis.scale(1.0 / length);

        Vec3 mid = start.add(end).scale(0.5);
        Vec3 toCamera = cameraPos.subtract(mid);
        /// > 带的横向 = 束轴 × 视线方向，使带面正对相机。
        Vec3 side = axis.cross(toCamera);
        if (side.lengthSqr() < 1.0e-8) {
            return;
        }
        side = side.normalize();

        /// > 镭射束使用固定世界半宽；不要随相机距离放大，否则会出现越远越粗的视觉问题。
        Vec3 sideOffset = side.scale(BEAM_HALF_WIDTH);

        int r = (int) (color[0] * 255);
        int g = (int) (color[1] * 255);
        int b = (int) (color[2] * 255);
        /// > 起点端淡，避免第一人称贴脸突兀。
        int aStart = (int) (alpha * 60);
        int aEnd = (int) (alpha * 200);

        VertexConsumer consumer = bufferSource.getBuffer(LaserRenderTypes.laserAdditive());

        float sx0 = (float) (start.x - cameraPos.x);
        float sy0 = (float) (start.y - cameraPos.y);
        float sz0 = (float) (start.z - cameraPos.z);
        float ex0 = (float) (end.x - cameraPos.x);
        float ey0 = (float) (end.y - cameraPos.y);
        float ez0 = (float) (end.z - cameraPos.z);
        float ox = (float) sideOffset.x;
        float oy = (float) sideOffset.y;
        float oz = (float) sideOffset.z;

        emitVertex(consumer, pose, sx0 - ox, sy0 - oy, sz0 - oz, r, g, b, aStart, 0, 0);
        emitVertex(consumer, pose, ex0 - ox, ey0 - oy, ez0 - oz, r, g, b, aEnd, 1, 0);
        emitVertex(consumer, pose, ex0 + ox, ey0 + oy, ez0 + oz, r, g, b, aEnd, 1, 1);
        emitVertex(consumer, pose, sx0 + ox, sy0 + oy, sz0 + oz, r, g, b, aStart, 0, 1);
    }

    private static void renderBeamView(Matrix4f pose, MultiBufferSource bufferSource,
                                       Vec3 start, Vec3 end, float[] color, float alpha) {
        Vec3 axis = end.subtract(start);
        double length = axis.length();
        if (length < 1.0e-4) {
            return;
        }
        axis = axis.scale(1.0 / length);
        Vec3 mid = start.add(end).scale(0.5);
        Vec3 toCamera = mid.scale(-1.0);
        Vec3 side = axis.cross(toCamera);
        if (side.lengthSqr() < 1.0e-8) {
            side = axis.cross(new Vec3(0, 1, 0));
        }
        if (side.lengthSqr() < 1.0e-8) {
            return;
        }
        side = side.normalize();

        /// > 镭射束使用固定世界半宽；不要随相机距离放大，否则会出现越远越粗的视觉问题。
        Vec3 sideOffset = side.scale(BEAM_HALF_WIDTH);

        int r = (int) (color[0] * 255);
        int g = (int) (color[1] * 255);
        int b = (int) (color[2] * 255);
        int aStart = (int) (alpha * 60);
        int aEnd = (int) (alpha * 200);
        VertexConsumer consumer = bufferSource.getBuffer(LaserRenderTypes.laserAdditive());

        emitVertex(consumer, pose, (float) (start.x - sideOffset.x), (float) (start.y - sideOffset.y), (float) (start.z - sideOffset.z), r, g, b, aStart, 0, 0);
        emitVertex(consumer, pose, (float) (end.x - sideOffset.x), (float) (end.y - sideOffset.y), (float) (end.z - sideOffset.z), r, g, b, aEnd, 1, 0);
        emitVertex(consumer, pose, (float) (end.x + sideOffset.x), (float) (end.y + sideOffset.y), (float) (end.z + sideOffset.z), r, g, b, aEnd, 1, 1);
        emitVertex(consumer, pose, (float) (start.x + sideOffset.x), (float) (start.y + sideOffset.y), (float) (start.z + sideOffset.z), r, g, b, aStart, 0, 1);
    }

    private static void renderDotOnPlaneWorld(Matrix4f pose, MultiBufferSource bufferSource, Vec3 cameraPos,
                                              Vec3 center, Vec3 normal, float[] color) {
        /// > 世界几何路径：保持单层贴面（传入的是世界坐标 - 相机坐标，无视图空间轴可用于 billboard）。
        Vec3 n = normal.normalize();
        Vec3 tangent = Math.abs(n.y) < 0.95 ? n.cross(new Vec3(0, 1, 0)) : n.cross(new Vec3(1, 0, 0));
        if (tangent.lengthSqr() < 1.0e-8) {
            return;
        }
        tangent = tangent.normalize();
        Vec3 bitangent = n.cross(tangent).normalize();

        double camDist = center.length();
        float half = (float) (DOT_HALF_SIZE * Math.max(1.0, camDist / 8.0));
        Vec3 c = center.add(n.scale(DOT_SURFACE_BIAS));
        Vec3 rVec = tangent.scale(half);
        Vec3 uVec = bitangent.scale(half);
        renderQuad(bufferSource, pose, c, rVec, uVec, color, 255, LaserRenderTypes.laserAdditive());
    }

    /// > 绘制依附命中平面的发光光斑（视图空间，双层）：
    /// > - 辉光层：billboard（视图空间相机朝向 -Z，quad 直接用视图 x / y 轴），大尺寸低 alpha，
    /// >   径向渐变纹理柔和淡出——保证掠射角下光斑仍可见并给出泛光晕；
    /// > - 核点层：贴面（normal 所在平面），小尺寸近白高亮——贴合命中表面，给出落点实感。
    /// >
    /// > 两层共用径向渐变贴图 [LaserRenderTypes#laserDot()]，加法混合。
    private static void renderDotOnPlane(Matrix4f pose, MultiBufferSource bufferSource,
                                         Vec3 center, Vec3 normal, float[] color) {
        Vec3 n = normal.normalize();
        Vec3 tangent = Math.abs(n.y) < 0.95 ? n.cross(new Vec3(0, 1, 0)) : n.cross(new Vec3(1, 0, 0));
        if (tangent.lengthSqr() < 1.0e-8) {
            return;
        }
        tangent = tangent.normalize();
        Vec3 bitangent = n.cross(tangent).normalize();

        double camDist = center.length();
        float half = (float) (DOT_HALF_SIZE * Math.max(1.0, camDist / 8.0));
        Vec3 c = center.add(n.scale(DOT_SURFACE_BIAS));

        /// > 辉光层：billboard，2.5× 尺寸低 alpha
        float glowHalf = half * 2.5f;
        renderQuad(bufferSource, pose, c, new Vec3(glowHalf, 0.0, 0.0), new Vec3(0.0, glowHalf, 0.0),
                color, 55, LaserRenderTypes.laserDot());

        /// > 核点层：贴面，0.5× 尺寸近白高亮
        float coreHalf = half * 0.5f;
        float[] coreColor = new float[]{
                (float) (color[0] * 0.55 + 0.45),
                (float) (color[1] * 0.55 + 0.45),
                (float) (color[2] * 0.55 + 0.45)
        };
        renderQuad(bufferSource, pose, c, tangent.scale(coreHalf), bitangent.scale(coreHalf),
                coreColor, 235, LaserRenderTypes.laserDot());
    }

    /// > 以中心 + 两个半宽轴向量绘制一个渐变贴图 quad（纹理 UV 全 0..1，加法混合）。
    private static void renderQuad(MultiBufferSource bufferSource, Matrix4f pose, Vec3 center,
                                   Vec3 rVec, Vec3 uVec, float[] color, int alpha, RenderType renderType) {
        int r = (int) (color[0] * 255);
        int g = (int) (color[1] * 255);
        int b = (int) (color[2] * 255);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        emitVertex(consumer, pose, (float) (center.x - rVec.x - uVec.x), (float) (center.y - rVec.y - uVec.y), (float) (center.z - rVec.z - uVec.z), r, g, b, alpha, 0, 0);
        emitVertex(consumer, pose, (float) (center.x + rVec.x - uVec.x), (float) (center.y + rVec.y - uVec.y), (float) (center.z + rVec.z - uVec.z), r, g, b, alpha, 1, 0);
        emitVertex(consumer, pose, (float) (center.x + rVec.x + uVec.x), (float) (center.y + rVec.y + uVec.y), (float) (center.z + rVec.z + uVec.z), r, g, b, alpha, 1, 1);
        emitVertex(consumer, pose, (float) (center.x - rVec.x + uVec.x), (float) (center.y - rVec.y + uVec.y), (float) (center.z - rVec.z + uVec.z), r, g, b, alpha, 0, 1);
    }

    private static Vec3 smoothDot(Vec3 current, Vec3 target, double factor) {
        if (current == null) {
            return target;
        }
        return current.lerp(target, factor);
    }

    private static HitInfo getCachedFirstPersonHit(Entity shooter, Vec3 worldStart, Vec3 worldDir, float maxRange) {
        long now = System.currentTimeMillis();
        boolean shouldUpdate = cachedFirstPersonHit == null
                || shooter.getId() != lastFirstPersonShooterId
                || Math.abs(maxRange - lastFirstPersonRange) > 1.0e-3f
                || now - lastFirstPersonRaycastTimeMs >= FIRST_PERSON_RAYCAST_INTERVAL_MS;
        if (shouldUpdate) {
            if (shooter.getId() != lastFirstPersonShooterId || Math.abs(maxRange - lastFirstPersonRange) > 1.0e-3f) {
                smoothedFirstPersonDotView = null;
            }
            cachedFirstPersonHit = raycast(shooter.level(), shooter, worldStart, worldDir, maxRange);
            lastFirstPersonRaycastTimeMs = now;
            lastFirstPersonShooterId = shooter.getId();
            lastFirstPersonRange = maxRange;
        }
        return cachedFirstPersonHit;
    }

    private static HitInfo raycast(Level level, Entity shooter, Vec3 worldStart, Vec3 worldDir, float maxRange) {
        Vec3 dir = worldDir.normalize();
        Vec3 end = worldStart.add(dir.scale(maxRange));
        BlockHitResult blockHit = level.clip(new ClipContext(
                worldStart, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        Vec3 hitPos = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : end;
        double hitDist = worldStart.distanceTo(hitPos);
        Vec3 hitNormal = blockHit.getType() == HitResult.Type.BLOCK
                ? Vec3.atLowerCornerOf(blockHit.getDirection().getNormal()) : null;

        AABB searchBox = new AABB(worldStart, hitPos).inflate(1.0);
        for (Entity entity : level.getEntities(shooter, searchBox, e -> !e.isSpectator() && e.isPickable())) {
            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
            var clipped = box.clip(worldStart, hitPos);
            if (clipped.isEmpty()) {
                continue;
            }
            Vec3 entityHit = clipped.get();
            double entityDist = worldStart.distanceTo(entityHit);
            if (entityDist < hitDist) {
                hitPos = entityHit;
                hitDist = entityDist;
                hitNormal = dir.scale(-1.0);
            }
        }
        return new HitInfo(hitPos, hitNormal);
    }

    private static Vec3 viewToWorld(Vec3 viewVec, Camera camera) {
        Vector3f right = camera.getLeftVector().mul(-1.0f, new Vector3f());
        Vector3f up = camera.getUpVector();
        Vector3f forward = new Vector3f(right).cross(up).mul(-1.0f);
        return new Vec3(
                right.x() * viewVec.x + up.x() * viewVec.y - forward.x() * viewVec.z,
                right.y() * viewVec.x + up.y() * viewVec.y - forward.y() * viewVec.z,
                right.z() * viewVec.x + up.z() * viewVec.y - forward.z() * viewVec.z);
    }

    private static Vec3 worldToView(Vec3 worldPos, Camera camera) {
        return worldVectorToView(worldPos.subtract(camera.getPosition()), camera);
    }

    private static Vec3 worldVectorToView(Vec3 worldVec, Camera camera) {
        Vector3f right = camera.getLeftVector().mul(-1.0f, new Vector3f());
        Vector3f up = camera.getUpVector();
        Vector3f forward = new Vector3f(right).cross(up).mul(-1.0f);
        return new Vec3(
                worldVec.x * right.x() + worldVec.y * right.y() + worldVec.z * right.z(),
                worldVec.x * up.x() + worldVec.y * up.y() + worldVec.z * up.z(),
                -(worldVec.x * forward.x() + worldVec.y * forward.y() + worldVec.z * forward.z()));
    }

    private record HitInfo(Vec3 pos, Vec3 normal) {}

    private static void emitVertex(VertexConsumer consumer, Matrix4f pose,
                                   float x, float y, float z, int r, int g, int b, int a, float u, float v) {
        consumer.vertex(pose, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0)
                .normal(0.0f, 1.0f, 0.0f)
                .endVertex();
    }
}
*/
