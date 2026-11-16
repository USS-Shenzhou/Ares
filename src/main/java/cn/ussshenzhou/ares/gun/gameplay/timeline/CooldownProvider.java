package cn.ussshenzhou.ares.gun.gameplay.timeline;

/// > 下一发射击冷却间隔（毫秒）的供给器。
/// > <p>
/// > 由 session 在装配开火行为时构建：默认实现按当前枪械 RPM 实时推导（{@code 60000 / RPM}）；
/// > 声明了 {@code fire.plan} 的 {@code cooldownMs} 时替换为脚本驱动实现（据热量 / 余弹等动态缩放）。
/// > <p>
/// > 双端各持一份：客户端在 {@link cn.ussshenzhou.ares.gun.gameplay.timeline.segment.CooldownSegment#onShotFired}
/// > 每发出膛时现取，喂墙上时钟节奏；服务端在 {@code getFireIntervalMs} 每次门控 / 扣减时现取，喂
/// > {@code shootTimePool} credit-pool。因是纯函数（输入 = 枪械只读状态），双端各读该端最新状态得等价节奏；
/// > fire 双端本就不对称（{@code docs/scripting-boundaries.md} §9.2），偏差由 S2C 同步纠正。
/// @author TACZ, USS_Shenzhou
@FunctionalInterface
public interface CooldownProvider {

    /// > 返回下一发允许开火的冷却间隔（毫秒），恒 &gt;= 1。
    long nextCooldownMs();
}
