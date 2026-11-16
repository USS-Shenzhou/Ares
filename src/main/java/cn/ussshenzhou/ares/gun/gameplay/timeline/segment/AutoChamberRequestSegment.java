package cn.ussshenzhou.ares.gun.gameplay.timeline.segment;

import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.SegmentToken;
import cn.ussshenzhou.ares.gun.gameplay.timeline.Timer;

/// > 客户端 Bolt 请求后锁定，等待网络同步回来的窗口
/// @author TACZ, USS_Shenzhou
public class AutoChamberRequestSegment extends ActionSegment {

    public static final String ID = "auto_chamber_request";
    public static final SegmentToken<AutoChamberRequestSegment> TOKEN = SegmentToken.of(ID, AutoChamberRequestSegment.class);

    public static final long NETWORK_TOLERANCE_MS = 500;

    private final Timer cooldown = new Timer();

    public AutoChamberRequestSegment(long lockMs) {
        cooldown.start(Math.max(lockMs, NETWORK_TOLERANCE_MS));
    }

    public static AutoChamberRequestSegment afterClientBoltRequest(long boltWindowMs) {
        return new AutoChamberRequestSegment(boltWindowMs + NETWORK_TOLERANCE_MS);
    }

    @Override
    public SegmentToken<AutoChamberRequestSegment> token() {
        return TOKEN;
    }

    @Override
    public boolean tick(long deltaMs) {
        if (cooldown.tick(deltaMs)) {
            finish();
            return false;
        }
        return true;
    }

    @Override
    public void cancel() {
        super.cancel();
        cooldown.stop();
    }

    @Override
    public String debugInfo() {
        return String.format("throttle %dms left", cooldown.remainingMs());
    }
}
