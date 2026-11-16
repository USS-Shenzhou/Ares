package cn.ussshenzhou.ares.gun.gameplay.timeline.segment;

import cn.ussshenzhou.ares.attachment.entity.GunHolderState;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionGate;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.SegmentToken;
import cn.ussshenzhou.ares.gun.gameplay.timeline.Timer;
import javax.annotation.Nullable;

/// > 拉栓/射后闭锁片段。活跃期间投影 BOLT_ACTION 并收紧动作门禁；结束时可选执行补膛。
/// @author TACZ, USS_Shenzhou
public class BoltSegment extends ActionSegment {

    public static final String ID = "bolt";
    public static final SegmentToken<BoltSegment> TOKEN = SegmentToken.of(ID, BoltSegment.class);

    private final Timer timer = new Timer();
    @Nullable
    private final Runnable reChamber;

    public BoltSegment(int boltActionTicks, @Nullable Runnable reChamber) {
        this.reChamber = reChamber;
        this.timer.start(Math.max(boltActionTicks, 0) * 50L);
    }

    @Override
    public SegmentToken<BoltSegment> token() {
        return TOKEN;
    }

    @Override
    public boolean tick(long deltaMs) {
        if (timer.tick(deltaMs)) {
            if (reChamber != null) {
                reChamber.run();
            }
            finish();
        }
        return isActive();
    }

    @Override
    public void constrain(ActionGate.Builder gate) {
        gate.denyShoot().denyReload().denySprint().denyInspect().denySwitchMode();
    }

    @Nullable
    @Override
    public GunHolderState projectedState() {
        return GunHolderState.BOLT_ACTION;
    }

    @Override
    public String debugInfo() {
        return String.format("bolt %dms left", timer.remainingMs());
    }

    @Override
    public void cancel() {
        super.cancel();
        timer.stop();
    }
}
