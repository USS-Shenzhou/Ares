package cn.ussshenzhou.ares.gun.client.util;

import cn.ussshenzhou.ares.gun.TimelessHelperC;
import cn.ussshenzhou.ares.gun.item.AttachmentSlot;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.gun.client.resource.display.AttachmentDisplayInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// > 安装的模板缓冲瞄具的发现入口。
/// > <p>
/// > 本表只负责「枪上装了哪些需要模板缓冲的瞄具」，顺序恒为枪械附件槽位声明顺序。
/// > 视图位的完整列表（本体机瞄 / 战术拒枪位 / 瞄具）与索引口径见 {@link AimViewHelper}。
/// @author TACZ, USS_Shenzhou
public class SightScopeHelper {
    /// > 收集顶层枪械上需要模板缓冲的瞄具，顺序为槽位声明顺序。
    public static List<InstalledSight> collectInstalledSights(ItemStack stack) {
        IGun gun = IGun.asGun(stack);
        if (gun == null) {
            return List.of();
        }
        List<InstalledSight> result = new ArrayList<>();
        for (Map.Entry<AttachmentSlot, ItemStack> entry : gun.getAttachmentAndSlots(stack).entrySet()) {
            ItemStack attachment = entry.getValue();
            if (attachment.isEmpty()) {
                continue;
            }
            AttachmentDisplayInstance display = TimelessHelperC.getAttachmentDisplay(attachment).orElse(null);
            if (display == null || display.getModel() == null || !display.getModel().needsStencil()) {
                continue;
            }
            result.add(new InstalledSight(entry.getKey().id(), attachment, display));
        }
        return List.copyOf(result);
    }

    public record InstalledSight(Identifier slotId, ItemStack stack, AttachmentDisplayInstance display) {
    }
}
