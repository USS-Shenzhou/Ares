package cn.ussshenzhou.ares.gun.client.util;

import cn.ussshenzhou.ares.gun.client.resource.display.AttachmentDisplayInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

/// > 一条「瞄准视图位」：Alt+V 循环列表中的一项，同时也是镜头视点 / 倍率 / 模板窗口的统一索引目标。
/// > <p>
/// > 视图位分两类：
/// > <ul>
/// >   <li>{@link BodyView}：枪械本体模型上的定位组骨骼（机瞄位 / 战术拒枪位），
/// >       视点即该骨骼在动画后的全局变换。</li>
/// >   <li>{@link ScopeView}：安装的模板缓冲瞄具，视点由静态挂点 × 附件模型 {@code scope_view} 骨骼给出。</li>
/// > </ul>
/// @author TACZ, USS_Shenzhou
public sealed interface AimView {

    enum Kind {
        /// > 本体机瞄位：无模板缓冲瞄具时列表的首项。
        IRON,
        /// > 本体战术拒枪位：第二个定位组，由 display 声明。
        TACTICAL,
        /// > 安装的模板缓冲瞄具。
        SCOPE
    }

    Kind kind();

    /// > 本体视位的定位组骨骼名；瞄具返回 null。
    @Nullable
    default String viewBone() {
        return null;
    }

    /// > 本体视位。
    /// >
    /// > @param kind         机瞄位 / 战术拒枪位
    /// > @param bone         定位组骨骼名（模型空间）
    /// > @param zoom         世界倍率：本体视位为单档，不参与有级 / 无级变倍
    /// > @param modelFov     非瞄准时的模型 FOV
    /// > @param zoomModelFov 开镜时的模型 FOV（与 {@code zoom} 配套）
    record BodyView(Kind kind, String bone, float zoom, float modelFov, float zoomModelFov) implements AimView {
        @Override
        public String viewBone() {
            return bone;
        }
    }

    /// > 安装的模板缓冲瞄具。
    record ScopeView(Identifier slotId, ItemStack stack, AttachmentDisplayInstance display) implements AimView {
        @Override
        public Kind kind() {
            return Kind.SCOPE;
        }
    }
}
