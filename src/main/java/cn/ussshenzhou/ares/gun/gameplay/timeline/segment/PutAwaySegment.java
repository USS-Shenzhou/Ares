package cn.ussshenzhou.ares.gun.gameplay.timeline.segment;

import cn.ussshenzhou.ares.util.GunHolderState;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionGate;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.SegmentToken;
import cn.ussshenzhou.ares.gun.gameplay.timeline.Timer;
import javax.annotation.Nullable;

/// > 收枪轨：收枪倒计时，期间投影 {@link GunHolderState#PUTTING_AWAY}。
/// > 倒计时结束触发 {@link #onComplete} 回调（由 operator 切入待拔出的物品并启动 {@link DrawSegment}）。
/// @author TACZ, USS_Shenzhou
public class PutAwaySegment extends ActionSegment {

    public static final String ID = "put_away";
    public static final SegmentToken<PutAwaySegment> TOKEN = SegmentToken.of(ID, PutAwaySegment.class);

    private final Timer timer = new Timer();
    @Nullable
    private final Runnable onComplete;

    public PutAwaySegment(long durationMs, @Nullable Runnable onComplete) {
        this.timer.start(Math.max(durationMs, 1));
        this.onComplete = onComplete;
    }

    @Override
    public SegmentToken<PutAwaySegment> token() {
        return TOKEN;
    }

    @Override
    public boolean tick(long deltaMs) {
        if (timer.tick(deltaMs)) {
            finish();
            if (onComplete != null) {
                onComplete.run();
            }
            return false;
        }
        return true;
    }

    @Override
    public void constrain(ActionGate.Builder gate) {
        /// > 收枪期：禁开火 / 换弹 / 冲刺 / 检视 / 切换（延续旧 PUTTING_AWAY 全禁）
        gate.denyShoot().denyReload().denySprint().denyInspect().denySwitchMode().denyAim();
    }

    @Nullable
    @Override
    public GunHolderState projectedState() {
        return GunHolderState.PUTTING_AWAY;
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
