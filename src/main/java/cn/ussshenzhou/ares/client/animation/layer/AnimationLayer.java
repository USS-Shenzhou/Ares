package cn.ussshenzhou.ares.client.animation.layer;

import com.mojang.logging.LogUtils;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.montage.AnimationMontage;
import cn.ussshenzhou.ares.client.animation.context.AnimationEventSink;
import cn.ussshenzhou.ares.client.animation.context.WeaponAnimationContext;
import cn.ussshenzhou.ares.client.animation.montage.MontageContext;
import cn.ussshenzhou.ares.client.animation.statemachine.GunStateMachineGraph;
import cn.ussshenzhou.ares.client.animation.statemachine.GunStateMachineRuntime;
import cn.ussshenzhou.ares.client.resource.ClientAssetsManager;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

/// @author TACZ, USS_Shenzhou
public class AnimationLayer {
    private final String name;
    private final ControllerLayerBindingCfg binding;
    private final GunStateMachineRuntime stateMachineRuntime;

    public AnimationLayer(String name, ControllerLayerBindingCfg binding, WeaponAnimationContext context,
                          Function<String, BedrockAnimation> animationResolver,
                          Function<Identifier, AnimationMontage<MontageContext>> montageResolver,
                          AnimationEventSink eventSink) {
        this.name = name;
        this.binding = binding;
        this.stateMachineRuntime = createStateMachineRuntime(context, animationResolver, montageResolver, eventSink);
    }

    public void tick() {
        if (stateMachineRuntime != null) {
            stateMachineRuntime.tick();
        }
    }

    public Pose evaluatePose() {
        return stateMachineRuntime != null ? stateMachineRuntime.evaluatePose() : DummyPose.INSTANCE;
    }

    public String currentStateId() {
        return stateMachineRuntime != null ? stateMachineRuntime.currentStateId() : "none";
    }

    public String blendMode() {
        return binding.blend != null && binding.blend.mode != null ? binding.blend.mode : "additive";
    }

    public float blendWeight() {
        return binding.blend != null ? binding.blend.weight : 1.0f;
    }

    private GunStateMachineRuntime createStateMachineRuntime(WeaponAnimationContext context,
                                                             Function<String, BedrockAnimation> animationResolver,
                                                             Function<Identifier, AnimationMontage<MontageContext>> montageResolver,
                                                             AnimationEventSink eventSink) {
        if (!"state_machine".equals(binding.type) || binding.machine == null) {
            LogUtils.getLogger().warn("[TACZ] Animation layer unsupported: layer={}, type={}", name, binding.type);
            return null;
        }
        GunStateMachineGraph graph = ClientAssetsManager.INSTANCE.getAnimationStateMachine(binding.machine);
        if (graph == null) {
            LogUtils.getLogger().warn("[TACZ] Animation state machine missing: layer={}, machine={}", name, binding.machine);
            return null;
        }
        return new GunStateMachineRuntime(graph, context, animationResolver, montageResolver, eventSink);
    }
}
