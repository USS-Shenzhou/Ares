package cn.ussshenzhou.ares.client.charm.render;

import cn.ussshenzhou.ares.client.charm.physics.CharmAxialRollState;
import cn.ussshenzhou.ares.client.charm.physics.CharmHingeState;
import cn.ussshenzhou.ares.client.charm.physics.CharmPhysicsState;
import cn.ussshenzhou.ares.gun.custom.charm.GunCharmData;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/// > 挂饰物理状态缓存。
/// >
/// > 按「挂饰绑定」的唯一标识（charmId + displayId + bone + hit/normal/offset）缓存各层物理状态：
/// > 主摆动 [CharmPhysicsState]、吊环铰链 [CharmHingeState]、摆体轴向滚转
/// > [CharmAxialRollState]。切枪 / 改绑定会自然换键得到新状态，避免姿态错乱。
/// >
/// > 物理纯客户端表现，状态随渲染帧推进，不走服务端同步。
/// @author TACZ, USS_Shenzhou
public class CharmRenderStateCache {
    private static final Map<Key, CharmPhysicsState> SWING_STATES = new HashMap<>();
    private static final Map<Key, CharmHingeState> HINGE_STATES = new HashMap<>();
    private static final Map<Key, CharmAxialRollState> BODY_ROLL_STATES = new HashMap<>();

    public static CharmPhysicsState swing(GunCharmData binding) {
        return SWING_STATES.computeIfAbsent(Key.of(binding), ignored -> new CharmPhysicsState());
    }

    public static CharmHingeState ringHinge(GunCharmData binding) {
        return HINGE_STATES.computeIfAbsent(Key.of(binding), ignored -> new CharmHingeState());
    }

    public static CharmAxialRollState bodyRoll(GunCharmData binding) {
        return BODY_ROLL_STATES.computeIfAbsent(Key.of(binding), ignored -> new CharmAxialRollState());
    }

    public static void clear() {
        SWING_STATES.clear();
        HINGE_STATES.clear();
        BODY_ROLL_STATES.clear();
    }

    private record Key(
            String charmId,
            Identifier displayId,
            String boneName,
            int hitX, int hitY, int hitZ,
            int normalX, int normalY, int normalZ,
            int offsetX, int offsetY, int offsetZ
    ) {
        private static Key of(GunCharmData binding) {
            Identifier charmId = binding.charmId();
            Vector3f hit = binding.localHit();
            Vector3f normal = binding.localNormal();
            Vector3f offset = binding.localOffset();
            return new Key(
                    charmId == null ? "missing" : charmId.toString(),
                    binding.displayId(),
                    binding.boneName(),
                    bits(hit.x), bits(hit.y), bits(hit.z),
                    bits(normal.x), bits(normal.y), bits(normal.z),
                    bits(offset.x), bits(offset.y), bits(offset.z)
            );
        }

        private static int bits(float value) {
            return Float.floatToIntBits(Float.isFinite(value) ? value : 0.0f);
        }
    }
}
