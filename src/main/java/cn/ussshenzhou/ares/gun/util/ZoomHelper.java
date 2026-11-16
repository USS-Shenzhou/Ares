package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.gun.init.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/// > 瞄具倍率状态的 NBT 存取（双端共用）。
/// > <p>
/// > 写入瞄具配件自身 ItemStack NBT，随配件实例移动；多层树场景下由 {@link AttachmentTreeHelper}
/// > 的自底向上回写机制序列化到顶层枪械。
/// > <ul>
/// >   <li>{@code ZoomIndex}：有级档位索引（int，缺省 0）；</li>
/// >   <li>{@code ZoomProgress}：无级进度（float 0~1，键不存在 = 未使用无级）。
/// >       模型 FOV 与倍率是两个独立数据字段（各自范围），运行时共用这一个进度、各自在范围内插值
/// >       （同比例映射）。</li>
/// > </ul>
/// > 服务端权威落地（经 C2S + {@link AttachmentTreeHelper#setZoom}），客户端预测先行后对账。
/// @author TACZ, USS_Shenzhou
public class ZoomHelper {
    public static void setZoomIndex(ItemStack scope, int index) {
        if (scope.isEmpty() || index < 0) {
            return;
        }
        scope.set(ModDataComponents.ZOOM_INDEX, index);
    }

    /// > 读取有级档位索引；未写入返回 0。
    public static int getZoomIndex(ItemStack scope) {
        if (scope.isEmpty()) {
            return 0;
        }
        return Math.max(0, scope.getOrDefault(ModDataComponents.ZOOM_INDEX, 0));
    }

    /// > 写入无级进度（0~1；范围外忽略）。
    public static void setZoomProgress(ItemStack scope, float progress) {
        if (scope.isEmpty() || progress < 0.0F || progress > 1.0F) {
            return;
        }
        scope.set(ModDataComponents.ZOOM_PROGRESS, progress);
    }

    /// > 读取无级进度；键不存在（未使用无级）返回 -1。
    public static float getZoomProgress(ItemStack scope) {
        if (scope.isEmpty()) {
            return -1.0F;
        }
        Float progress = scope.get(ModDataComponents.ZOOM_PROGRESS);
        if (progress == null) {
            return -1.0F;
        }
        return progress >= 0.0F && progress <= 1.0F ? progress : -1.0F;
    }

    public static boolean hasZoomProgress(ItemStack scope) {
        if (scope.isEmpty()) {
            return false;
        }
        return scope.has(ModDataComponents.ZOOM_PROGRESS);
    }

    public static void removeZoomProgress(ItemStack scope) {
        if (scope.isEmpty()) {
            return;
        }
        scope.remove(ModDataComponents.ZOOM_PROGRESS);
    }
}
