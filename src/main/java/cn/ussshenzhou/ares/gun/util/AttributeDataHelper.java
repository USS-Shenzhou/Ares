package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.attribute.GunAttributes;
import cn.ussshenzhou.ares.gun.custom.attribute.GunPropertiesCaches;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

/// > 属性读取助手（脚本 / 状态机 / 玩法共用）。
/// > <p>
/// > 语义见 {@code docs/draft/attribute-system.md} 第三节：
/// > <ul>
/// >   <li>未注册的属性返回 {@code null}（区分"未注册"与"值为 0"）；</li>
/// >   <li>已注册但本枪无来源时返回注册基准值（{@code default}）；</li>
/// >   <li>属性 id 缺省 namespace 补 {@code tacz:}，与 {@code gun.has_attachment} 等同款约定。</li>
/// > </ul>
/// @author TACZ, USS_Shenzhou
public class AttributeDataHelper {
    /// > 解析属性 id。缺省 namespace 补 {@code tacz:}；空白或非法返回 null。
    @Nullable
    public static Identifier parseId(@Nullable String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        // FIXME REVIEW the default namespace was hard-coded as "tacz:".
        String normalized = rawId.contains(":") ? rawId : Ares.MOD_ID + ":" + rawId;
        return Identifier.tryParse(normalized);
    }

    /// > 属性最终值（含配件 / 弹种 / 开火模式修饰）。未注册、id 非法或物品不可用时返回 null。
    @Nullable
    public static Double getAttribute(@Nullable ItemStack stack, @Nullable String rawId) {
        return getAttribute(stack, parseId(rawId));
    }

    /// > 属性最终值（含配件 / 弹种 / 开火模式修饰）。未注册或物品不可用时返回 null。
    @Nullable
    public static Double getAttribute(@Nullable ItemStack stack, @Nullable Identifier id) {
        if (id == null || stack == null || stack.isEmpty()) {
            return null;
        }
        return GunPropertiesCaches.of(stack).getAttributeOrNull(id);
    }

    /// > 该属性在本枪上是否被显式赋值（枪械基础属性声明，或任一修饰来源）。
    /// > 未注册、id 非法或物品不可用时返回 false。
    public static boolean hasAttribute(@Nullable ItemStack stack, @Nullable String rawId) {
        Identifier id = parseId(rawId);
        if (id == null || stack == null || stack.isEmpty()) {
            return false;
        }
        return GunPropertiesCaches.of(stack).hasAttribute(id);
    }

    /// > ==================== 属性 -> 玩法语义 ====================

    /// > 基准射击间隔（毫秒）：{@code 60000 / RPM}；无有效 RPM（含枪械不可用）时回退 120。
    public static long shootIntervalMs(@Nullable ItemStack stack) {
        Double rpm = getAttribute(stack, GunAttributes.RPM);
        if (rpm == null || rpm <= 0) {
            return 120;
        }
        return (long) (60000.0 / rpm);
    }

    /// > 持枪移速倍率：{@code 1 - weight * 0.01}（重量每点降低 1% 移速）；枪械不可用时为 1。
    public static double moveSpeedMultiplier(@Nullable ItemStack stack) {
        Double weight = getAttribute(stack, GunAttributes.WEIGHT);
        return weight == null ? 1.0 : 1.0 - weight * 0.01;
    }
}
