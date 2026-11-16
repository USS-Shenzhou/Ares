package cn.ussshenzhou.ares.gun.entity;

import cn.ussshenzhou.ares.gun.gameplay.ShootHelper;
import net.minecraft.world.level.Level;

/// > 弹丸类型 provider：负责该类型的整体发射（创建飞行实体或执行 hitscan 判定）。
/// >
/// > 通过 [ProjectileRegistry] 以字符串 key 注册。
/// > - 飞行实体类（bullet/arrow/grenade）：走默认 [#fire]，
/// >   由 [#create] 建实体、[ShootHelper#applyCommonFields] 设通用字段、
/// >   [#setup] 处理类型特有配置，最后加入世界。
/// > - 瞬时射线类（laser hitscan）：重写 [#fire] 直接做射线判定，不创建实体，
/// >   [#create] 不被调用。
///
/// @author TACZ, USS_Shenzhou
public interface ProjectileProvider {
    /// > 执行一次发射。
    /// >
    /// > 默认实现：创建飞行实体 + 通用字段设置 + 类型特有 setup + 加入世界。
    /// > hitscan 类型应重写此方法。
    /// >
    /// > @param ctx   射前上下文（含通用字段与类型特有数据）
    /// >
    /// > @param yaw   已散布调整的偏航角
    /// >
    /// > @param pitch 已散布调整的俯仰角
    default void fire(ProjectileContext ctx, float yaw, float pitch) {
        AbstractGunProjectile projectile = create(ctx.level());
        ShootHelper.applyCommonFields(projectile, ctx, yaw, pitch);
        setup(projectile, ctx);
        ctx.level().addFreshEntity(projectile);
    }

    /// > 创建弹丸实体实例。飞行实体类必需；hitscan 类不会被调用，可抛
    /// > [UnsupportedOperationException]。
    AbstractGunProjectile create(Level level);

    /// > 类型特有的射前准备（可选）。调用时通用字段已由 [ShootHelper#applyCommonFields] 设置完毕。
    default void setup(AbstractGunProjectile projectile, ProjectileContext ctx) {
    }
}
