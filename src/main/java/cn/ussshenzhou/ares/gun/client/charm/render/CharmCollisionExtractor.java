package cn.ussshenzhou.ares.gun.client.charm.render;

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.ICube;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBoneDefinition;
import cn.ussshenzhou.ares.gun.client.charm.physics.CharmCollisionBox;
import cn.ussshenzhou.ares.gun.client.model.BedrockGunModel;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/// > 挂饰防穿模：从枪体几何提取锚点骨骼附近的 cube，转成「锚点骨骼局部空间」的碰撞盒（OBB）。
/// >
/// > 用 **bind pose（静态、不含动画）** 计算并按 `(枪 display id, 锚点骨骼名, 关注中心量化)` 缓存：
/// > 枪上各骨骼的相对位姿在 bind pose 下固定，动画带来的相对偏移很小，挂饰防穿模追求观感而非绝对精确，
/// > 故每帧复用同一份碰撞盒即可，省去逐帧遍历整把枪所有 cube 的开销，也避免动画抖动。
/// > 资源重载时 [#clearCache()]。
/// >
/// > 物理在锚点骨骼局部空间（方块单位）推进，故碰撞盒变换为
/// > `anchorInvBind × boneBind × cubeRotation`：把 cube 局部 AABB 连同其自身旋转一起变到锚点骨骼空间。
/// > 枪体 cube 通常远多于上限，按「到关注中心（挂饰静止质点）的距离」保留最近的若干个——挂饰只会扫到
/// > 锚点附近区域，远处的大主体盒对防穿模无意义。
/// @author TACZ, USS_Shenzhou
public class CharmCollisionExtractor {
    /// > 单挂饰最多保留的枪体碰撞盒数量上限，防极端模型逐盒求解卡顿。按到关注中心距离保留最近的。
    private static final int MAX_COLLISION_BOXES = 48;

    private static final Map<Key, List<CharmCollisionBox>> CACHE = new HashMap<>();

    public static void clearCache() {
        CACHE.clear();
    }

    /// > 取某枪在指定锚点骨骼局部空间下、关注中心附近的枪体碰撞盒列表（缓存）。
    /// >
    /// > @param gunDisplayId 枪 display id（缓存键，区分不同枪）
    /// >
    /// > @param gunModel     枪模型（提供 bind pose 静态骨骼变换与几何）
    /// >
    /// > @param anchorBone   挂饰结合到的锚点骨骼名（碰撞盒变换到此骨骼局部空间）
    /// >
    /// > @param focusCenter  关注中心（锚点骨骼局部空间，方块）：挂饰静止质点，按到它的距离保留最近的盒
    public static List<CharmCollisionBox> get(Identifier gunDisplayId, BedrockGunModel gunModel,
                                              String anchorBone, Vector3f focusCenter) {
        if (gunDisplayId == null || gunModel == null || anchorBone == null || anchorBone.isBlank()
                || focusCenter == null || !finite(focusCenter)) {
            return List.of();
        }
        Key key = new Key(gunDisplayId, anchorBone,
                Math.round(focusCenter.x * 32.0f), Math.round(focusCenter.y * 32.0f), Math.round(focusCenter.z * 32.0f));
        List<CharmCollisionBox> cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        List<CharmCollisionBox> built = build(gunModel, anchorBone, focusCenter);
        CACHE.put(key, built);
        return built;
    }

    private static List<CharmCollisionBox> build(BedrockGunModel gunModel, String anchorBone, Vector3f focusCenter) {
        Matrix4f anchorBind = gunModel.getBindGlobalTransform(anchorBone);
        if (anchorBind == null) {
            return List.of();
        }
        Matrix4f anchorInv = new Matrix4f(anchorBind);
        if (Math.abs(anchorInv.determinant()) < 1.0e-6f) {
            return List.of();
        }
        anchorInv.invert();

        List<CharmCollisionBox> boxes = new ArrayList<>();
        for (TreeBoneDefinition bone : gunModel.getBaseModel().bones()) {
            ICube[] cubes = bone.cubes();
            if (cubes.length == 0) {
                continue;
            }
            String name = bone.name();
            /// > 跳过手臂定位骨骼、视角 / 相机 / locator 辅助骨骼。
            /// > 注意：不跳过锚点骨骼自身——挂饰只是挂在它表面一点，仍应被该骨骼的其余几何（如机匣主体）挡住。
            if (name != null && isNonCollidableBone(name)) {
                continue;
            }
            /// > 用静态可见性兜底：bind 状态下被标记隐藏的骨骼（如某些适配器占位）不计入
            BoneState state = gunModel.getBone(bone.index());
            if (state != null && !state.visible) {
                continue;
            }
            Matrix4f boneBind = gunModel.getBindGlobalTransform(name);
            if (boneBind == null) {
                continue;
            }
            /// > boneToAnchor：枪骨骼 bind 全局 → 锚点骨骼局部
            Matrix4f boneToAnchor = new Matrix4f(anchorInv).mul(boneBind);
            for (ICube cube : cubes) {
                Matrix4f boxToAnchor = new Matrix4f(boneToAnchor);
                if (cube.hasRotation()) {
                    float[] pivot = cube.pivot();
                    boxToAnchor.translate(pivot[0], pivot[1], pivot[2]);
                    boxToAnchor.rotate(cube.rotation());
                    boxToAnchor.translate(-pivot[0], -pivot[1], -pivot[2]);
                }
                float inflate = cube.inflate();
                Vector3f min = new Vector3f(cube.x() - inflate, cube.y() - inflate, cube.z() - inflate);
                Vector3f max = new Vector3f(
                        cube.x() + cube.width() + inflate,
                        cube.y() + cube.height() + inflate,
                        cube.z() + cube.depth() + inflate);
                CharmCollisionBox box = CharmCollisionBox.transformed(min, max, boxToAnchor);
                if (box.finite()) {
                    boxes.add(box);
                }
            }
        }

        /// > 按到关注中心的距离保留最近的若干盒：挂饰只会扫到锚点附近，远处盒无意义
        boxes.sort(Comparator.comparingDouble(b -> b.distanceSquaredTo(focusCenter)));
        if (boxes.size() > MAX_COLLISION_BOXES) {
            boxes = new ArrayList<>(boxes.subList(0, MAX_COLLISION_BOXES));
        }
        return List.copyOf(boxes);
    }

    /// > 是否为不参与碰撞的辅助骨骼：手臂定位、视角 / 相机锚点、locator 类。
    private static boolean isNonCollidableBone(String boneName) {
        String lower = boneName.toLowerCase(Locale.ROOT);
        return lower.equals("lefthand_pos") || lower.equals("righthand_pos")
                || lower.contains("hand")
                || lower.contains("view") || lower.contains("camera")
                || lower.endsWith("locator") || lower.contains("_locator");
    }

    private static boolean finite(Vector3f v) {
        return v != null && Float.isFinite(v.x) && Float.isFinite(v.y) && Float.isFinite(v.z);
    }

    private record Key(Identifier gunDisplayId, String anchorBone, int fx, int fy, int fz) {
    }
}
