package cn.ussshenzhou.ares.gun.client.animation.statemachine;

import com.mojang.logging.LogUtils;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.molang.MolangEngineHelper;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.MochaEngine;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.maydaymemory.mae.control.statemachine.IAnimationState;
import com.maydaymemory.mae.control.statemachine.IAnimationTransition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/// > 已编译的状态机图：把无状态的状态机定义编译为不可变、可跨实例共享的状态/过渡对象图。
/// > <p>
/// > 本图对「求值后端」无知：状态与过渡只需实现 MAE 的
/// > {@link IAnimationState} / {@link IAnimationTransition} 接口，方法体既可由 Molang 表达式驱动
/// > （{@link GunAnimationState} / {@link GunAnimationTransition}），也可由 JS 闭包驱动
/// > （脚本状态机）。两条 loader 产出同一种 {@code GunStateMachineGraph}，下游
/// > {@link GunStateMachineRuntime} 完全复用。
/// > <p>
/// > 运行时状态只存在于每实例的 {@link com.maydaymemory.mae.control.statemachine.AnimationStateMachine}
/// > 与 {@link GunStateContext}。因此本图由数据管理器在资源 reload 时构建一次，所有枪械实例共享同一份，
/// > 求值时各自传入独立的 {@link GunStateContext}（各自的轨道 / variable 存储），互不干扰。
/// > <p>
/// > {@link #onInitHook} 在状态机创建时执行一次（见 {@link GunStateMachineRuntime}），用于建立常驻轨道；
/// > Molang 路径下它包装一段 onInit 表达式，JS 路径下它包装脚本注册的 onInit 闭包。
/// @author TACZ, USS_Shenzhou
public class GunStateMachineGraph {
    private final Map<String, GunState> states;
    @Nullable
    private final GunState entryState;
    private final Consumer<GunStateContext> onInitHook;

    private GunStateMachineGraph(Map<String, GunState> states,
                                 @Nullable GunState entryState,
                                 Consumer<GunStateContext> onInitHook) {
        this.states = states;
        this.entryState = entryState;
        this.onInitHook = onInitHook != null ? onInitHook : context -> {};
    }

    /// > 通用工厂：由任意求值后端（Molang / JS）构建图。状态图与 onInit 钩子由调用方组装完毕后传入。
    /// >
    /// > @param states     状态 id -> 状态对象（过渡已绑定到各状态）
    /// > @param entryState 入口状态；为 null 时取 {@code states} 的任意一个并告警
    /// > @param onInitHook 状态机创建时执行一次的钩子（建立常驻轨道等），可为 null
    public static GunStateMachineGraph of(Map<String, GunState> states,
                                          @Nullable GunState entryState,
                                          @Nullable Consumer<GunStateContext> onInitHook) {
        Map<String, GunState> safeStates = states != null ? states : new HashMap<>();
        GunState resolvedEntry = entryState;
        if (resolvedEntry == null && !safeStates.isEmpty()) {
            resolvedEntry = safeStates.values().iterator().next();
        }
        if (resolvedEntry == null) {
            LogUtils.getLogger().warn("[TACZ] Animation state machine has no valid entry state");
        }
        return new GunStateMachineGraph(safeStates, resolvedEntry, onInitHook);
    }

    /// > Molang 路径：把 JSON 反序列化出的 {@link GunStateMachine} 编译为图。定义为空时返回空图。
    public static GunStateMachineGraph compile(GunStateMachine definition) {
        MochaEngine<?> engine = GunMolangContext.sharedCompileEngine();
        MolangExpression onInit = compileAction(engine, definition.onInit, "stateMachine.onInit", definition.entryStateId);
        Map<String, GunState> states = buildStates(engine, definition);
        bindTransitions(engine, definition, states);
        GunState entryState = states.get(definition.entryStateId);
        Consumer<GunStateContext> onInitHook = context -> onInit.evaluate(context.molangContext());
        return of(states, entryState, onInitHook);
    }

    @Nullable
    public GunState entryState() {
        return entryState;
    }

    /// > 执行 onInit 钩子（状态机创建时一次）。对 Molang / JS 两种后端统一。
    public void runOnInit(GunStateContext context) {
        onInitHook.accept(context);
    }

    private static Map<String, GunState> buildStates(MochaEngine<?> engine, GunStateMachine definition) {
        Map<String, GunState> states = new HashMap<>();
        if (definition.states == null) {
            return states;
        }
        for (StateMachineState stateDefinition : definition.states) {
            if (stateDefinition == null || stateDefinition.id == null) {
                continue;
            }
            MolangExpression onEnter = compileAction(engine, stateDefinition.onEnter, "state.onEnter", stateDefinition.id);
            MolangExpression onUpdate = compileAction(engine, stateDefinition.onUpdate, "state.onUpdate", stateDefinition.id);
            MolangExpression onExit = compileAction(engine, stateDefinition.onExit, "state.onExit", stateDefinition.id);
            states.put(stateDefinition.id, new GunAnimationState(stateDefinition, onEnter, onUpdate, onExit));
        }
        return states;
    }

    private static void bindTransitions(MochaEngine<?> engine, GunStateMachine definition, Map<String, GunState> states) {
        if (definition.transitions == null) {
            return;
        }
        Map<String, List<IAnimationTransition<GunStateContext>>> transitionsByState = new HashMap<>();
        for (StateMachineTransition transitionDefinition : definition.transitions) {
            if (transitionDefinition == null || transitionDefinition.from == null || transitionDefinition.to == null) {
                continue;
            }
            if (!"state".equals(transitionDefinition.from.kind) || !"state".equals(transitionDefinition.to.kind)) {
                continue;
            }
            GunState fromState = states.get(transitionDefinition.from.stateId);
            GunState toState = states.get(transitionDefinition.to.stateId);
            if (fromState == null || toState == null) {
                continue;
            }
            MolangExpression condition = compileAction(engine, transitionDefinition.condition, "transition.condition", transitionDefinition.id);
            MolangExpression afterTrigger = compileAction(engine, transitionDefinition.afterTrigger, "transition.afterTrigger", transitionDefinition.id);
            transitionsByState.computeIfAbsent(transitionDefinition.from.stateId, id -> new ArrayList<>())
                    .add(new GunAnimationTransition(transitionDefinition, toState, condition, afterTrigger));
        }
        for (Map.Entry<String, List<IAnimationTransition<GunStateContext>>> entry : transitionsByState.entrySet()) {
            GunState state = states.get(entry.getKey());
            if (state instanceof GunAnimationState animationState) {
                animationState.setTransitions(entry.getValue());
            }
        }
    }

    /// > 编译一段 Molang（条件或动作）。空串返回恒 0 表达式。
    private static MolangExpression compileAction(MochaEngine<?> engine, String expression, String where, String ownerId) {
        if (expression == null || expression.isBlank()) {
            return MolangExpression.zero();
        }
        try {
            return MolangEngineHelper.compileExpression(engine, expression);
        } catch (RuntimeException exception) {
            LogUtils.getLogger().warn("[TACZ] Animation state machine molang compile failed: where={}, owner={}, expr={}",
                    where, ownerId, expression, exception);
            return MolangExpression.zero();
        }
    }
}
