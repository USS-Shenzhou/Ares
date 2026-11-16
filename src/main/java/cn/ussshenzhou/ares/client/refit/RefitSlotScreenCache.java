package cn.ussshenzhou.ares.client.refit;

import cn.ussshenzhou.ares.gun.item.AttachmentPath;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/// > 缓存改装界面下各槽位挂点投影到屏幕的 2D 坐标（GUI 像素空间）。
/// >
/// > 由 [RefitScreenCacheCollector] 在第一人称改装渲染时递归写入整棵配件树：取每个槽位
/// > `pos_group` 骨骼在模型空间的位置，经视图矩阵 + 投影矩阵投影到 NDC，再换算为 GUI 像素坐标。
/// > [cn.ussshenzhou.ares.client.gui.GunRefitScreen] 据此在枪上对应部位绘制可点击标记。
/// >
/// > 以 [AttachmentPath] 为 key 唯一标识树中任意层槽位（不同层可能有同名 slotId）。
/// > 每帧重建，仅在改装态有效。
/// @author TACZ, USS_Shenzhou
public class RefitSlotScreenCache {
    /// > 槽位挂点的屏幕坐标（GUI 像素）。
    /// >
    /// > @param x       GUI x（像素）
    /// >
    /// > @param y       GUI y（像素）
    /// >
    /// > @param visible 该挂点是否在相机前方且可见（z 在 0~1 之间）
    public record SlotMarker(float x, float y, boolean visible) {
    }

    private static final Map<AttachmentPath, SlotMarker> MARKERS = new HashMap<>();

    /// > 清空缓存（每帧渲染前调用）。
    public static void clear() {
        MARKERS.clear();
    }

    public static void put(AttachmentPath path, SlotMarker marker) {
        MARKERS.put(path, marker);
    }

    @Nullable
    public static SlotMarker get(AttachmentPath path) {
        return MARKERS.get(path);
    }

    public static Map<AttachmentPath, SlotMarker> all() {
        return MARKERS;
    }
}
