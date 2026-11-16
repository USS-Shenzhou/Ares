package cn.ussshenzhou.ares.client.animation.statemachine;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.statemachine.IAnimationState;
import com.maydaymemory.mae.control.statemachine.IAnimationTransition;

import java.util.List;

/// @author TACZ, USS_Shenzhou
public class GunAnimationState implements GunState {
    private final StateMachineState definition;
    private final MolangExpression onEnter;
    private final MolangExpression onUpdate;
    private final MolangExpression onExit;
    private final List<String> poseTracks;
    private List<IAnimationTransition<GunStateContext>> transitions = List.of();

    public GunAnimationState(StateMachineState definition,
                             MolangExpression onEnter,
                             MolangExpression onUpdate,
                             MolangExpression onExit) {
        this.definition = definition;
        this.onEnter = onEnter;
        this.onUpdate = onUpdate;
        this.onExit = onExit;
        this.poseTracks = definition.pose != null ? definition.pose.resolveTracks() : List.of();
    }

    public StateMachineState definition() {
        return definition;
    }

    @Override
    public String id() {
        return definition.id;
    }

    public void setTransitions(List<IAnimationTransition<GunStateContext>> transitions) {
        this.transitions = transitions != null ? transitions : List.of();
    }

    @Override
    public Iterable<IAnimationTransition<GunStateContext>> transitions() {
        return transitions;
    }

    @Override
    public void onEnter(GunStateContext context, IAnimationState<GunStateContext> fromState) {
        onEnter.evaluate(context.molangContext());
    }

    @Override
    public void onExit(GunStateContext context, IAnimationTransition<GunStateContext> triggeredTransition) {
        onExit.evaluate(context.molangContext());
    }

    @Override
    public void onUpdate(GunStateContext context) {
        onUpdate.evaluate(context.molangContext());
    }

    @Override
    public Pose evaluatePose(GunStateContext context) {
        if (poseTracks.isEmpty()) {
            return DummyPose.INSTANCE;
        }
        if (poseTracks.size() == 1) {
            return context.evaluateTrackPose(poseTracks.getFirst());
        }
        return context.evaluateTrackPoses(poseTracks);
    }
}
