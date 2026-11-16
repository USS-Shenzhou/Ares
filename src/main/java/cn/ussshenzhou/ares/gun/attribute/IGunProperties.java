package cn.ussshenzhou.ares.gun.attribute;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Map;

/// > 枪械属性只读契约：由「枪械数据 + 配件树 + 弹种 + 开火模式」派生的属性值。
/// > <p>
/// > 实现与缓存生命周期见 {@code cn.ussshenzhou.ares.gun.custom.attribute.GunPropertiesCaches}；本接口只表达读取语义，
/// > 供不关心实现的消费方（弹丸上下文、脚本视图、玩法逻辑）依赖。
/// > <p>
/// > 语义要点：
/// > <ul>
/// >   <li>{@link #getAttributeOrNull(Identifier)} 区分「未注册」（返回 null）与「已注册但本枪无来源」
/// >       （回落注册默认值）；</li>
/// >   <li>{@link #hasAttribute(Identifier)} 表示本枪是否被显式赋值（基础属性声明或任一修饰来源），
/// >       仅回落默认值不算；</li>
/// >   <li>属性来源是封闭集合，就地改写相关 NBT 的写入方负责让实现失效。</li>
/// > </ul>
/// @author TACZ, USS_Shenzhou
public interface IGunProperties {
    /// > 单个属性的 base 值与最终值（{@code modified} 已按注册定义 clamp）。
    record CachedValue(double base, double modified) {
    }

    /// > 属性最终值。未注册的属性返回 0（脚本 / 状态机读值请用 {@link #getAttributeOrNull(Identifier)}）。
    double getAttribute(Identifier attr);

    /// > 属性最终值：未注册的属性返回 null；已注册但本枪无来源时回落注册基准值。
    @Nullable
    Double getAttributeOrNull(Identifier attr);

    /// > 该属性在本枪上是否有显式来源（基础属性声明，或任一修饰来源）。
    boolean hasAttribute(Identifier attr);

    /// > 已计算属性的缓存视图（key 为属性 id）。
    Map<Identifier, CachedValue> getCachedAttribute();

    /// > 枪械各槽位的配件快照，只读。
    @UnmodifiableView
    Map<Identifier, ItemStack> getAttachmentCaches();

    /// > 已安装下挂武器的副本；无则 null。
    @Nullable
    ItemStack getMountedWeapon();

    /// > 按姿态取散布值（度）。
    double getInaccuracy(InaccuracyType inaccuracyType);

    /// > 弹匣扩容等级（0 起，随已安装扩容配件递增）。
    int getMagLevel();
}
