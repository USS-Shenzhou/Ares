package cn.ussshenzhou.ares.gun.gameplay.timeline.segment;

import cn.ussshenzhou.ares.util.GunHolderState;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionGate;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.SegmentToken;
import cn.ussshenzhou.ares.gun.gameplay.timeline.Timer;
import javax.annotation.Nullable;

/// > 跑射延迟轨（仅客户端表现）：收编旧的 `stopSprintTimestamp`（短投影窗口）与
/// > `extraBlockSprintTimestamp`（较长的阻止再冲刺窗口）。
/// >
/// > 玩家在冲刺中开火时启动本轨：
/// > - 前 [#STATE_WINDOW_MS]（180ms）内投影 [GunHolderState#STOP_SPRINTING]，
/// >   表达「停止冲刺过渡」的小状态。
/// > - 前 [#SPRINT_BLOCK_MS]（400ms）内收紧冲刺门禁，避免射击打断冲刺后反复进入/取消冲刺。
/// >
/// > 两窗口结束后轨道自然消失。
/// @author TACZ, USS_Shenzhou
public class StopSprintSegment extends ActionSegment {

    public static final String ID = "stop_sprint";
    public static final SegmentToken<StopSprintSegment> TOKEN = SegmentToken.of(ID, StopSprintSegment.class);

    private static final long STATE_WINDOW_MS = 180;
    private static final long SPRINT_BLOCK_MS = 400;

    private final Timer stateWindow = new Timer();
    private final Timer sprintBlock = new Timer();
    private final boolean projectStopSprinting;

    /// > @param projectStopSprinting true 表示投影 STOP_SPRINTING 小状态（射击打断冲刺时）；
    /// >                             false 表示仅阻止再冲刺、不投影（普通射击后的 400ms 冲刺锁）。
    public StopSprintSegment(boolean projectStopSprinting) {
        this.projectStopSprinting = projectStopSprinting;
        if (projectStopSprinting) {
            stateWindow.start(STATE_WINDOW_MS);
        }
        sprintBlock.start(SPRINT_BLOCK_MS);
    }

    /// > 射击打断冲刺：投影 STOP_SPRINTING + 阻止再冲刺。
    public static StopSprintSegment interruptSprint() {
        return new StopSprintSegment(true);
    }

    /// > 普通射击后的冲刺锁：仅阻止再冲刺，不投影状态。
    public static StopSprintSegment blockOnly() {
        return new StopSprintSegment(false);
    }

    @Override
    public SegmentToken<StopSprintSegment> token() {
        return TOKEN;
    }

    /// > 刷新 400ms 冲刺锁窗口（每次射击调用，延续旧 extraBlockSprintTimestamp = now+400 的刷新语义）。
    public void refreshBlock() {
        sprintBlock.start(SPRINT_BLOCK_MS);
    }

    @Override
    public boolean tick(long deltaMs) {
        stateWindow.tick(deltaMs);
        sprintBlock.tick(deltaMs);
        /// > 两窗口都结束时轨道消失
        if (!stateWindow.isRunning() && !sprintBlock.isRunning()) {
            finish();
            return false;
        }
        return true;
    }

    @Override
    public void constrain(ActionGate.Builder gate) {
        if (sprintBlock.isRunning()) {
            gate.denySprint();
        }
        /// > 跑射停顿过渡窗口（投影 STOP_SPRINTING 期间）禁止开火，
        /// > 收编旧实现中 sprintTimeS>0 时射击返回 IS_SPRINTING 的语义，
        /// > 使「冲刺中开火」需等待停顿过渡结束后才真正发射。
        if (projectStopSprinting && stateWindow.isRunning()) {
            gate.denyShoot();
        }
    }

    @Nullable
    @Override
    public GunHolderState projectedState() {
        return (projectStopSprinting && stateWindow.isRunning()) ? GunHolderState.STOP_SPRINTING : null;
    }

    @Override
    public String debugInfo() {
        return String.format("state=%dms block=%dms", stateWindow.remainingMs(), sprintBlock.remainingMs());
    }

    @Override
    public void cancel() {
        super.cancel();
        stateWindow.stop();
        sprintBlock.stop();
    }
}
