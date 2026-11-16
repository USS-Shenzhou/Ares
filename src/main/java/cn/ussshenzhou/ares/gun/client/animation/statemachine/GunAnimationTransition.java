package cn.ussshenzhou.ares.gun.client.animation.statemachine;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.blend.EasingBlendCurve;
import com.maydaymemory.mae.control.blend.IBlendCurve;
import com.maydaymemory.mae.control.statemachine.IAnimationState;
import com.maydaymemory.mae.control.statemachine.IAnimationTransition;
import com.maydaymemory.mae.control.statemachine.TransferOutStrategy;

/// @author TACZ, USS_Shenzhou
public class GunAnimationTransition implements IAnimationTransition<GunStateContext> {
    private final StateMachineTransition definition;
    private final IAnimationState<GunStateContext> targetState;
    private final MolangExpression condition;
    private final MolangExpression afterTrigger;
    private final IBlendCurve curve;

    public GunAnimationTransition(StateMachineTransition definition,
                                  IAnimationState<GunStateContext> targetState,
                                  MolangExpression condition,
                                  MolangExpression afterTrigger) {
        this.definition = definition;
        this.targetState = targetState;
        this.condition = condition != null ? condition : MolangExpression.zero();
        this.afterTrigger = afterTrigger != null ? afterTrigger : MolangExpression.zero();
        this.curve = new EasingBlendCurve(TransitionEasing.resolve(definition.easing));
    }

    @Override
    public IAnimationState<GunStateContext> targetState() {
        return targetState;
    }

    @Override
    public IBlendCurve curve() {
        return curve;
    }

    @Override
    public float duration() {
        return definition.duration;
    }

    @Override
    public TransferOutStrategy transferOutStrategy() {
        /// > 过渡进行中只检测目标状态的出向过渡，避免源状态的进入条件（持续为真）
        /// > 每帧重新触发同一过渡导致 startNanos 不断重置、过渡永不结束。
        return TransferOutStrategy.TO_STATE;
    }

    @Override
    public boolean canTrigger(GunStateContext context) {
        return condition.evaluate(context.molangContext()) != 0.0;
    }

    @Override
    public void afterTrigger(GunStateContext context) {
        /// > 过渡触发瞬间执行声明的动作（如 query.play_once('primary','run_start')），
        /// > 使目标动画在过渡开始就启动，过渡插值终点为目标动画实时帧，避免完成时跳变。
        afterTrigger.evaluate(context.molangContext());
    }

    @Override
    public Pose getInterpolatedPose(GunStateContext context, Pose fromPose, Pose toPose, float alpha) {
        return context.blend(fromPose, toPose, alpha);
    }
}
