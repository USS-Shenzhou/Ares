package cn.ussshenzhou.ares.gun.gameplay.timeline;

import javax.annotation.Nullable;

/// > 拉栓窗口协调能力：把「起真正 {@link cn.ussshenzhou.ares.gun.gameplay.timeline.segment.BoltSegment} 拉栓窗口」
/// > 收口为一个跨域可复用的入口。
/// > <p>
/// > {@link FireController} 实现它（拉栓窗口时长、{@code onBoltStart} 自愈屏蔽锁的逻辑都在那里）。
/// > 换弹编排（{@code ServerReloadStepApi}）持有它，从而换弹 Step 也能起<b>与开火同源的</b>拉栓窗口
/// > （走正常片段通道、投影 {@code BOLT_ACTION}、收门禁），而非硬编码即时补膛。这是「换弹后是否/如何拉栓」
/// > 跨域协调的收口点。
/// @author TACZ, USS_Shenzhou
public interface BoltCoordinator {

    /// > 起一个拉栓窗口。窗口时长由枪械数据（{@code bolt_action_time}）推导；无窗口（时长为 0）时：
    /// > 若给了 {@code onWindowEnd} 则即时执行该副作用（无窗口的退化路径），否则什么都不做。
    /// > 起窗口会统一触发端侧的拉栓启动副作用（客户端自愈屏蔽锁）。
    /// >
    /// > @param onWindowEnd 窗口结束时执行的权威副作用（如补膛）；{@code null} 表示只起闭锁窗口不带副作用。
    void startBoltWindow(@Nullable Runnable onWindowEnd);

    /// > 当前是否处于拉栓窗口内。
    boolean isBolting();

    /// > 拉栓窗口时长（毫秒）。0 表示该枪无拉栓窗口。
    long getBoltWindowMs();
}
