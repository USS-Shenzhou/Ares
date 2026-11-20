package cn.ussshenzhou.ares.client.resource;

import com.google.gson.JsonElement;
import cn.ussshenzhou.ares.gun.resource.GunGsonHelper;
import cn.ussshenzhou.ares.gun.resource.JsonDataManager;
import cn.ussshenzhou.ares.client.animation.statemachine.GunStateMachineCfg;
import cn.ussshenzhou.ares.client.animation.statemachine.GunStateMachineGraph;

/// > 状态机数据管理器：在资源 reload 的 `apply` 阶段把 [GunStateMachineCfg]
/// > 编译为不可变、可跨实例共享的 [GunStateMachineGraph]。
/// >
/// > 编译产物随 [JsonDataManager] 的 reload 生命周期重建（`dataMap.clear()` + 重填），
/// > 无需额外的缓存与失效逻辑，与其它数据管理器（如 `GunDisplayManager`）范式一致。
/// @author TACZ, USS_Shenzhou
public class AnimationStateMachineManager extends JsonDataManager<GunStateMachineGraph> {
    public AnimationStateMachineManager() {
        super(GunStateMachineGraph.class, GunGsonHelper.GSON, "animations/statemachines", "AnimationStateMachine");
    }

    @Override
    protected GunStateMachineGraph parseJson(JsonElement element) {
        GunStateMachineCfg cfg = getGson().fromJson(element, GunStateMachineCfg.class);
        return cfg == null ? null : GunStateMachineGraph.compile(cfg);
    }
}
