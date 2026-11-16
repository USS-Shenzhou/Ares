package cn.ussshenzhou.ares.client.animation.statemachine.source;

import com.maydaymemory.mae.basic.IEvaluationContext;
import com.maydaymemory.mae.basic.Pose;

/// > 状态机轨道的「播放后端」抽象。
/// >
/// > 状态机本身对 Pose 从哪来无知（见 `GunAnimationState.evaluatePose`），
/// > 只要求「给我一个 Pose、推进时间、告诉我是否结束」。本接口把这层能力抽象出来，
/// > 使常规 `AnimationRunner` 与 `AnimationMontageRunner` 成为状态机可互换的两种轨道后端：
/// > - [AnimationRunnerSource]：循环 / 一次性 / 手动相位（idle、movement locomotion）。
/// > - [MontageRunnerSource]：一次性动作 + 变速 + notify + 多轨（reload、inspect、draw、holster）。
///
/// @author TACZ, USS_Shenzhou
public interface PoseSource {
    /// > 推进时间 / 进度。
    void tick();

    /// > 求值出当前 Pose。
    /// >
    /// > @param ctx 求值上下文，可空（无 Molang 动态关键帧时）。montage 后端当前忽略此参数。
    Pose evaluate(IEvaluationContext ctx);

    /// > 一次性播放是否已结束（loop / manual 恒返回 false）。用于 once / montage 轨道的过渡条件。
    boolean isEnd();

    /// > 进度控制句柄。仅 manual / locomotion 轨道需要；montage 后端返回 [IProgressControl#NONE]。
    IProgressControl progress();
}
