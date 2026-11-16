package cn.ussshenzhou.ares.gun.client.renderer.item;

import cn.ussshenzhou.ares.gun.gameplay.MuzzleReference;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/// > 记录实体按完整发射源区分的最近开火时间戳。
/// >
/// > 由 {@code S2CShootPacket} 写入；当前保留给后续第三人称枪口表现使用。
/// @author TACZ, USS_Shenzhou
public class MuzzleFlashHandler {
    private static final Map<Entity, Map<Source, Long>> SHOOT_TIMESTAMPS = new WeakHashMap<>();

    public static void onEntityShoot(Entity entity, InteractionHand hand, MuzzleReference muzzleReference) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            SHOOT_TIMESTAMPS.computeIfAbsent(entity, ignored -> new HashMap<>())
                    .put(new Source(hand, muzzleReference), mc.level.getGameTime());
        }
    }

    public static Long getShootTimestamp(Entity entity, InteractionHand hand, MuzzleReference muzzleReference) {
        Map<Source, Long> sources = SHOOT_TIMESTAMPS.get(entity);
        return sources != null ? sources.get(new Source(hand, muzzleReference)) : null;
    }

    public record Source(InteractionHand hand, MuzzleReference muzzleReference) {
        public Source {
            hand = hand != null ? hand : InteractionHand.MAIN_HAND;
            muzzleReference = muzzleReference != null ? muzzleReference : MuzzleReference.primary();
        }
    }
}
