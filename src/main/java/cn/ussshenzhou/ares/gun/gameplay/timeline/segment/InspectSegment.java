package cn.ussshenzhou.ares.gun.gameplay.timeline.segment;

import cn.ussshenzhou.ares.util.GunHolderState;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionGate;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.SegmentSet;
import cn.ussshenzhou.ares.gun.gameplay.timeline.SegmentToken;
import javax.annotation.Nullable;

/// > 检视轨（仅客户端表现）：投影 {@link GunHolderState#INSPECT}。
/// > <p>
/// > 检视没有固定时长（由动画驱动，玩家任何动作打断），因此本轨不自计时，
/// > 持续活跃直到 operator 显式 {@link SegmentSet#cancel(String)}
/// > （开火 / 瞄准 / 换弹 / 切换等动作触发取消）。
/// > <p>
/// > 检视期间仍允许开火 / 换弹 / 瞄准（它们会取消检视），故只投影状态、不收紧这些门禁；
/// > 仅禁止冲刺与再次检视（延续旧 INSPECT 语义：canSprint=true 实际由 STOP_SPRINTING 接管，
/// > 这里保留 INSPECT 不阻止冲刺的旧行为，故不 denySprint）。
/// @author TACZ, USS_Shenzhou
public class InspectSegment extends ActionSegment {

    public static final String ID = "inspect";
    public static final SegmentToken<InspectSegment> TOKEN = SegmentToken.of(ID, InspectSegment.class);

    @Override
    public SegmentToken<InspectSegment> token() {
        return TOKEN;
    }

    @Override
    public boolean tick(long deltaMs) {
        /// > 无固定时长：持续活跃直到被显式取消
        return true;
    }

    @Override
    public void constrain(ActionGate.Builder gate) {
        /// > 检视期禁止再次检视；其余动作（开火/换弹/瞄准）允许并会触发取消，故不收紧
        gate.denyInspect();
    }

    @Nullable
    @Override
    public GunHolderState projectedState() {
        return GunHolderState.INSPECT;
    }
}
