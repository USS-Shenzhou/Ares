package cn.ussshenzhou.ares.gun.client.util;

import cn.ussshenzhou.ares.gun.TimelessHelperC;
import cn.ussshenzhou.ares.gun.client.resource.display.GunDisplayInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/// > 瞄准视图位的统一发现入口。
/// > <p>
/// > 镜头视点、世界倍率与模型 FOV、模板缓冲会话、开镜变倍四处都按同一份有序列表读取同一枚索引，
/// > 避免各自扫描后产生错位。列表顺序即 Alt+V 的循环顺序：
/// > <ul>
/// >   <li>枪上没有任何模板缓冲瞄具时：本体机瞄位 →（声明时）战术拒枪位；</li>
/// >   <li>装了模板缓冲瞄具时：瞄具1 → … → 瞄具n →（声明时）战术拒枪位。
/// >       本体机瞄位不进列表——瞄具已经把它挡死，切回去没有意义。</li>
/// > </ul>
/// > 索引 0 恒为当前默认视点，保证掏枪后的默认开镜形态与是否新增拒枪位无关。
/// @author TACZ, USS_Shenzhou
public class AimViewHelper {
    /// > 本体机瞄位的定位组骨骼名（模型约定）。
    public static final String IRON_VIEW_BONE = "iron_view";

    /// > 按枪械本体 display 收集视图位。
    public static List<AimView> collect(ItemStack stack) {
        return collect(stack, null);
    }

    /// > 收集视图位。
    /// >
    /// > @param stack   枪械物品堆
    /// > @param display 本次生效的 display（双持单手变体有自己的模型与骨骼名时须传入）；
    /// >                为 null 时按物品取本体 display
    public static List<AimView> collect(ItemStack stack, @Nullable GunDisplayInstance display) {
        GunDisplayInstance resolved = display != null ? display : TimelessHelperC.getGunDisplay(stack).orElse(null);
        List<SightScopeHelper.InstalledSight> sights = SightScopeHelper.collectInstalledSights(stack);
        List<AimView> views = new ArrayList<>(sights.size() + 2);
        if (sights.isEmpty() && resolved != null) {
            views.add(ironView(resolved));
        }
        for (SightScopeHelper.InstalledSight sight : sights) {
            views.add(new AimView.ScopeView(sight.slotId(), sight.stack(), sight.display()));
        }
        /// > 战术拒枪位：本体第二定位组，与瞄具视线不重叠，故可与瞄具并存。
        GunDisplayInstance.TacticalStance stance = resolved != null ? resolved.getTacticalStance() : null;
        if (stance != null) {
            views.add(new AimView.BodyView(AimView.Kind.TACTICAL, stance.viewBone(),
                    stance.zoom(), stance.modelFov(), stance.zoomModelFov()));
        }
        return List.copyOf(views);
    }

    /// > 取指定索引的视图位；越界返回 null。
    @Nullable
    public static AimView viewAt(List<AimView> views, int index) {
        return index >= 0 && index < views.size() ? views.get(index) : null;
    }

    /// > 把当前（含切换过渡）视图位换算成模板缓冲会话需要的瞄具槽位窗口：
    /// > 本体视位对应 null 槽位（没有激活窗口）。
    public static ScopeWindow scopeWindow(List<AimView> views, int activeIndex, int pendingIndex, float progress) {
        boolean transitioning = pendingIndex >= 0 && pendingIndex != activeIndex;
        Identifier activeSlot = slotIdOf(viewAt(views, activeIndex));
        Identifier pendingSlot = transitioning ? slotIdOf(viewAt(views, pendingIndex)) : null;
        return new ScopeWindow(activeSlot, pendingSlot, transitioning, progress);
    }

    @Nullable
    private static Identifier slotIdOf(@Nullable AimView view) {
        return view instanceof AimView.ScopeView scope ? scope.slotId() : null;
    }

    /// > 模板缓冲会话的激活窗口：槽位为 null 表示该视图位不是瞄具。
    public record ScopeWindow(@Nullable Identifier activeSlotId, @Nullable Identifier pendingSlotId,
                              boolean transitioning, float progress) {
        public static final ScopeWindow NONE = new ScopeWindow(null, null, false, 1f);
    }

    private static AimView ironView(GunDisplayInstance display) {
        return new AimView.BodyView(AimView.Kind.IRON, IRON_VIEW_BONE,
                display.getIronZoom(), display.getModelFov(), display.getZoomModelFov());
    }
}
