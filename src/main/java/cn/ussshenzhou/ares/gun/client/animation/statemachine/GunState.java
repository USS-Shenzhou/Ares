package cn.ussshenzhou.ares.gun.client.animation.statemachine;

import com.maydaymemory.mae.control.statemachine.IAnimationState;

/// > 枪械动画状态机中的状态：在 MAE 的 {@link IAnimationState} 之上补充一个跨后端的稳定 id。
/// > <p>
/// > Molang 后端（{@link GunAnimationState}）与 JS 后端的状态都实现本接口，使
/// > {@link GunStateMachineGraph} 与 {@link GunStateMachineRuntime} 无需关心具体求值后端，
/// > 即可按 id 组织状态图、投影当前状态用于调试。
/// @author TACZ, USS_Shenzhou
public interface GunState extends IAnimationState<GunStateContext> {
    /// > 状态的稳定标识（用于图索引与调试展示）。
    String id();
}
