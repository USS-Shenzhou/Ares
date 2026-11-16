package cn.ussshenzhou.ares.client.charm.render;

import cn.ussshenzhou.ares.client.charm.CharmAnchorPlacement;
import cn.ussshenzhou.ares.client.model.BedrockCharmModel;
import cn.ussshenzhou.ares.gun.custom.charm.CharmModelLayout;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/// > 挂饰分层几何缓存（对接 SimpleBedrockModel v2）。
/// >
/// > 从挂饰模型按 layout 解析各部件骨骼（anchor / fixedPivot / dynamicPivot / body）在 bind pose 下的
/// > pivot 偏移，并据「forward 轴对齐命中面法线」朝向把这些偏移旋转到锚点骨骼局部空间，得到
/// > [cn.ussshenzhou.ares.client.charm.CharmLayerPlacement#fromAnchor] 所需的各层偏移。
/// > 结果按 (模型 id, 法线量化, forward 轴量化) 缓存，资源重载时清空。
/// @author TACZ, USS_Shenzhou
public class CharmLayoutGeometry {
    private static final Map<Key, CharmLayoutGeometry> CACHE = new HashMap<>();

    private final Vector3f anchorOffset;
    private final Vector3f fixedPivotOffset;
    private final Vector3f dynamicPivotOffset;
    private final Vector3f massOffset;
    private final Vector3f bodyEccentricOffset;
    /// > 渲染链所需的原始（未朝向）模型空间 pivot 偏移
    private final Vector3f rawFixedPivotOffset;
    private final Vector3f rawDynamicPivotOffset;
    private final Vector3f rawBodyPivotOffset;
    private final boolean layered;
    private final String bodyBoneName;

    private CharmLayoutGeometry(Vector3f anchorOffset, Vector3f fixedPivotOffset, Vector3f dynamicPivotOffset,
                                Vector3f massOffset, Vector3f bodyEccentricOffset, Vector3f rawFixedPivotOffset,
                                Vector3f rawDynamicPivotOffset, Vector3f rawBodyPivotOffset, boolean layered,
                                String bodyBoneName) {
        this.anchorOffset = anchorOffset;
        this.fixedPivotOffset = fixedPivotOffset;
        this.dynamicPivotOffset = dynamicPivotOffset;
        this.massOffset = massOffset;
        this.bodyEccentricOffset = bodyEccentricOffset;
        this.rawFixedPivotOffset = rawFixedPivotOffset;
        this.rawDynamicPivotOffset = rawDynamicPivotOffset;
        this.rawBodyPivotOffset = rawBodyPivotOffset;
        this.layered = layered;
        this.bodyBoneName = bodyBoneName;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static CharmLayoutGeometry get(Identifier charmId, BedrockCharmModel model, CharmModelLayout layout,
                                          Vector3f localNormal, Vector3f forwardAxis) {
        Key key = Key.of(charmId, localNormal, forwardAxis);
        return CACHE.computeIfAbsent(key, ignored -> build(model, layout, localNormal, forwardAxis));
    }

    private static CharmLayoutGeometry build(BedrockCharmModel model, CharmModelLayout layout,
                                             Vector3f localNormal, Vector3f forwardAxis) {
        if (model == null || layout == null || !layout.isLayered()) {
            return new CharmLayoutGeometry(new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f(),
                    new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f(), false, "");
        }
        String bodyBone = bodyBoneName(layout);
        Vector3f anchorOffset = oriented(pivot(model, layout.anchorPart()), localNormal, forwardAxis);
        Vector3f fixedPivotOffset = oriented(pivot(model, layout.fixedPivotPart()), localNormal, forwardAxis);
        Vector3f dynamicPivotOffset = oriented(pivot(model, layout.dynamicPivotPart()), localNormal, forwardAxis);
        /// > 摆体质心（已朝向）作为物理质点静止位置；无 cube 时退回摆体枢轴
        Vector3f massOffset = model.subtreeVolumeWeightedCenter(bodyBone)
                .map(center -> oriented(center, localNormal, forwardAxis))
                .orElseGet(() -> oriented(pivot(model, bodyBone), localNormal, forwardAxis));
        Vector3f eccentric = bodyEccentricOffset(model, bodyBone, localNormal, forwardAxis);
        Vector3f rawFixedPivot = pivot(model, layout.fixedPivotPart());
        Vector3f rawDynamicPivot = pivot(model, layout.dynamicPivotPart());
        Vector3f rawBodyPivot = pivot(model, bodyBone);
        return new CharmLayoutGeometry(anchorOffset, fixedPivotOffset, dynamicPivotOffset, massOffset,
                eccentric, rawFixedPivot, rawDynamicPivot, rawBodyPivot, true, bodyBone);
    }

    private static Vector3f pivot(BedrockCharmModel model, String boneName) {
        if (boneName == null || boneName.isBlank()) {
            return new Vector3f();
        }
        return model.pivotOffset(boneName).orElseGet(Vector3f::new);
    }

    private static Vector3f oriented(Vector3f modelOffset, Vector3f localNormal, Vector3f forwardAxis) {
        return CharmAnchorPlacement.orientVectorToSurface(modelOffset, localNormal, forwardAxis);
    }

    /// > 摆体偏心偏移：质心相对摆体枢轴的偏移（已旋转到锚点局部空间），用于轴向滚转力矩。
    private static Vector3f bodyEccentricOffset(BedrockCharmModel model, String bodyBone,
                                                Vector3f localNormal, Vector3f forwardAxis) {
        Optional<Vector3f> center = model.subtreeVolumeWeightedCenter(bodyBone);
        if (center.isEmpty()) {
            return new Vector3f();
        }
        Vector3f massCenter = oriented(center.get(), localNormal, forwardAxis);
        Vector3f pivot = oriented(pivot(model, bodyBone), localNormal, forwardAxis);
        Vector3f offset = new Vector3f(massCenter).sub(pivot);
        return finite(offset) ? offset : new Vector3f();
    }

    private static String bodyBoneName(CharmModelLayout layout) {
        for (String name : layout.dynamicParts()) {
            if (name != null && !name.isBlank() && !name.equals(layout.dynamicPivotPart())) {
                return name;
            }
        }
        return layout.dynamicPivotPart();
    }

    public boolean isLayered() {
        return layered;
    }

    public String bodyBoneName() {
        return bodyBoneName;
    }

    public Vector3f anchorOffset() {
        return new Vector3f(anchorOffset);
    }

    public Vector3f fixedPivotOffset() {
        return new Vector3f(fixedPivotOffset);
    }

    public Vector3f dynamicPivotOffset() {
        return new Vector3f(dynamicPivotOffset);
    }

    public Vector3f massOffset() {
        return new Vector3f(massOffset);
    }

    public Vector3f bodyEccentricOffset() {
        return new Vector3f(bodyEccentricOffset);
    }

    /// > 摆体枢轴（charm_ring）在模型空间的原始 pivot 偏移（未朝向）。渲染链做 dynamicDelta 用。
    public Vector3f rawDynamicPivotOffset() {
        return new Vector3f(rawDynamicPivotOffset);
    }

    /// > 固定枢轴（gun_ring）在模型空间的原始 pivot 偏移（未朝向）。
    public Vector3f rawFixedPivotOffset() {
        return new Vector3f(rawFixedPivotOffset);
    }

    /// > 摆体（body）在模型空间的原始 pivot 偏移（未朝向）。
    public Vector3f rawBodyPivotOffset() {
        return new Vector3f(rawBodyPivotOffset);
    }

    private static boolean finite(Vector3f value) {
        return value != null && Float.isFinite(value.x) && Float.isFinite(value.y) && Float.isFinite(value.z);
    }

    private record Key(Identifier charmId, int normalX, int normalY, int normalZ, int forwardX, int forwardY, int forwardZ) {
        private static Key of(Identifier charmId, Vector3f normal, Vector3f forward) {
            return new Key(charmId,
                    quantized(normal, 0), quantized(normal, 1), quantized(normal, 2),
                    quantized(forward, 0), quantized(forward, 1), quantized(forward, 2));
        }

        private static int quantized(Vector3f vector, int axis) {
            if (vector == null) {
                return 0;
            }
            float value = axis == 0 ? vector.x : axis == 1 ? vector.y : vector.z;
            if (!Float.isFinite(value)) {
                return 0;
            }
            return Math.round(value * 64.0f);
        }
    }
}
