package cn.ussshenzhou.ares.gun.client.resource;

import com.google.gson.JsonElement;
import cn.ussshenzhou.ares.gun.resource.GunGsonHelper;
import cn.ussshenzhou.ares.gun.resource.JsonDataManager;
import cn.ussshenzhou.ares.gun.client.animation.statemachine.GunStateMachine;
import cn.ussshenzhou.ares.gun.client.animation.statemachine.GunStateMachineGraph;

/// > 状态机数据管理器：在资源 reload 的 {@code apply} 阶段把 {@link GunStateMachine}
/// > 编译为不可变、可跨实例共享的 {@link GunStateMachineGraph}。
/// > <p>
/// > 编译产物随 {@link JsonDataManager} 的 reload 生命周期重建（{@code dataMap.clear()} + 重填），
/// > 无需额外的缓存与失效逻辑，与其它数据管理器（如 {@code GunDisplayManager}）范式一致。
/// @author TACZ, USS_Shenzhou
public class AnimationStateMachineManager extends JsonDataManager<GunStateMachineGraph> {
    public AnimationStateMachineManager() {
        super(GunStateMachineGraph.class, GunGsonHelper.GSON, "animations/statemachines", "AnimationStateMachine");
    }

    @Override
    protected GunStateMachineGraph parseJson(JsonElement element) {
        GunStateMachine definition = getGson().fromJson(element, GunStateMachine.class);
        return definition == null ? null : GunStateMachineGraph.compile(definition);
    }
}
