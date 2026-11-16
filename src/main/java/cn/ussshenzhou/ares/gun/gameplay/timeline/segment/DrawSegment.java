package cn.ussshenzhou.ares.gun.gameplay.timeline.segment;

import cn.ussshenzhou.ares.attachment.entity.GunHolderState;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionGate;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.SegmentToken;
import cn.ussshenzhou.ares.gun.gameplay.timeline.Timer;
import javax.annotation.Nullable;

/// > 拔枪轨：拔枪倒计时，期间投影 {@link GunHolderState#DRAWING}。
/// > 倒计时结束后回 NORMAL（轨道自然结束、投影随之消失）。
/// @author TACZ, USS_Shenzhou
public class DrawSegment extends ActionSegment {

    public static final String ID = "draw";
    public static final SegmentToken<DrawSegment> TOKEN = SegmentToken.of(ID, DrawSegment.class);

    private final Timer timer = new Timer();

    public DrawSegment(long durationMs) {
        this.timer.start(Math.max(durationMs, 1));
    }

    @Override
    public SegmentToken<DrawSegment> token() {
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
        /// > 拔枪期：禁开火 / 换弹 / 检视 / 切换；可冲刺（延续旧 DRAWING 的 canSprint=true）
        gate.denyShoot().denyReload().denyInspect().denySwitchMode();
    }

    @Nullable
    @Override
    public GunHolderState projectedState() {
        return GunHolderState.DRAWING;
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
