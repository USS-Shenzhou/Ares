package cn.ussshenzhou.ares.client.animation.statemachine;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.time.AnimationClocks;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.montage.AnimationMontage;
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import cn.ussshenzhou.ares.client.animation.context.AnimationEventSink;
import cn.ussshenzhou.ares.client.animation.context.WeaponAnimationContext;
import cn.ussshenzhou.ares.client.animation.montage.MontageContext;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

/// > 状态机运行时：只承载每实例独有的运行时状态（[GunStateContext] 与 [AnimationStateMachine]）。
/// > 无状态的编译产物（状态/过渡图 + Molang 表达式）由 [GunStateMachineGraph] 在资源 reload 时编译一次，
/// > 所有实例共享同一份，此处仅按需 new 一个状态机壳并绑定各自的上下文。
/// @author TACZ, USS_Shenzhou
public class GunStateMachineRuntime {
    private final GunStateContext context;
    private final AnimationStateMachine<GunStateContext> stateMachine;

    public GunStateMachineRuntime(GunStateMachineGraph graph,
                                  WeaponAnimationContext weaponContext,
                                  Function<String, BedrockAnimation> animationResolver,
                                  Function<Identifier, AnimationMontage<MontageContext>> montageResolver,
                                  AnimationEventSink eventSink) {
        this.context = new GunStateContext(weaponContext, animationResolver, montageResolver, eventSink);
        /// > 先执行 onInit 建立常驻轨道（并行播放所有动画），状态只负责选择读取哪个轨道。
        /// > 这样目标状态的轨道在过渡开始前就已存在并被同步，过渡插值两端都有实时姿态，
        /// > 既不会过渡期卡住（目标轨道未建立），也不会过渡完成时切换轨道导致闪现。
        graph.runOnInit(context);
        GunState entryState = graph.entryState();
        this.stateMachine = entryState != null ? new AnimationStateMachine<>(entryState, context, AnimationClocks.client()::nowNanos) : null;
        if (entryState != null) {
            entryState.onEnter(context, null);
        }
    }

    public void tick() {
        if (stateMachine == null) {
            return;
        }
        /// > 先推进轨道（loop/once 时间推进 + 所有 manual 轨道相位统一同步），再 tick 状态机。
        /// > 轨道相位同步独立于状态机当前状态，保证过渡期间相位也是实时的，避免跳变。
        context.tick();
        stateMachine.tick();
    }

    public Pose evaluatePose() {
        return stateMachine != null ? stateMachine.getPose() : DummyPose.INSTANCE;
    }

    /// > 当前激活状态的 id；过渡进行中（state 为 null）返回目标状态 id 加 "*" 后缀，供调试展示。
    public String currentStateId() {
        if (stateMachine == null) {
            return "none";
        }
        var state = stateMachine.getCurrentState();
        if (state instanceof GunState gunState) {
            return gunState.id();
        }
        var transition = stateMachine.getCurrentTransition();
        if (transition != null && transition.getTransition().targetState() instanceof GunState target) {
            return target.id() + "*";
        }
        return "none";
    }
}
