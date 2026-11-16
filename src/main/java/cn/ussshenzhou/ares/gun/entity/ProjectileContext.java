package cn.ussshenzhou.ares.gun.entity;

import cn.ussshenzhou.ares.gun.attribute.IGunProperties;
import cn.ussshenzhou.ares.gun.gameplay.MuzzleReference;
import cn.ussshenzhou.ares.gun.custom.ammo.AmmoData;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

/// > 弹丸射前上下文（只读数据载体）。
/// > <p>
/// > 由 ShootHelper 构造一次并在一次射击的所有弹丸间复用。除类型特有配置（如榴弹爆炸参数）外，
/// > 通用射前字段（damage/pierce/gravity/lifespan 等）也一并携带，使 {@link ProjectileProvider}
/// > 能自洽完成发射（{@link ProjectileProvider#fire}）——无论创建飞行实体还是执行 hitscan 判定。
/// @author TACZ, USS_Shenzhou
public record ProjectileContext(
        ServerPlayer player,
        ServerLevel level,
        ItemStack gun,
        @Nullable GunData gunData,
        @Nullable AmmoData ammoData,
        IGunProperties cache,
        @Nullable Identifier gunId,
        @Nullable Identifier ammoId,
        InteractionHand hand,
        MuzzleReference muzzleReference,
        float damage,
        float speed,
        int pierce,
        float gravity,
        int lifespan,
        float headshotMultiplier,
        float penetration,
        float limbMultiplier,
        float pierceDecay,
        float knockback,
        boolean tracer
) {
}
