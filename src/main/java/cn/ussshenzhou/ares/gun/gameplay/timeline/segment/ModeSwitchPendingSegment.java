package cn.ussshenzhou.ares.gun.gameplay.timeline.segment;

import cn.ussshenzhou.ares.attachment.entity.GunHolderState;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionGate;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.SegmentToken;
import cn.ussshenzhou.ares.gun.gameplay.timeline.Timer;

/// > 模式切换请求锁定窗口（仅客户端）。
/// > <p>
/// > 客户端切换模块模式 / 快慢机时<b>不做预测</b>：发出 C2S 请求后起本片段锁定输入，等待服务端权威 NBT
/// > 同步回来，由会话 tick 对账真正应用切换并取消本片段。若请求丢失（服务端 NBT 不变、无同步回传），
/// > 本片段计时到期后自然消失解锁，玩家可再次按键。
/// > <p>
/// > 锁定期间禁止开火 / 换弹 / 检视 / 再次切换，与 {@link ModeTransitionSegment} 门禁一致。
/// > 不投影 {@link GunHolderState}（纯门禁，不占用 isIdle）。
/// @author TACZ, USS_Shenzhou
public class ModeSwitchPendingSegment extends ActionSegment {

    public static final String ID = "mode_switch_pending";
    public static final SegmentToken<ModeSwitchPendingSegment> TOKEN = SegmentToken.of(ID, ModeSwitchPendingSegment.class);

    /// > 等待服务端确认的默认超时窗口（毫秒）。到期未确认即解锁，允许重新按键。
    public static final long DEFAULT_TIMEOUT_MS = 2000;

    private final Timer timer = new Timer();

    public ModeSwitchPendingSegment(long timeoutMs) {
        this.timer.start(Math.max(timeoutMs, 1));
    }

    @Override
    public SegmentToken<ModeSwitchPendingSegment> token() {
        return TOKEN;
    }

    @Override
    public boolean tick(long deltaMs) {
        if (timer.tick(deltaMs)) {
            finish();
            return false;
        }
        return true;
    }

    @Override
    public void constrain(ActionGate.Builder gate) {
        gate.denyShoot().denyReload().denyInspect().denySwitchMode();
    }

    @Override
    public String debugInfo() {
        return String.format("%dms left", timer.remainingMs());
    }

    @Override
    public void cancel() {
        super.cancel();
        timer.stop();
    }
}
