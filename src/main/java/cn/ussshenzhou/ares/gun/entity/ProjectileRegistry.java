package cn.ussshenzhou.ares.gun.entity;

import javax.annotation.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// > 弹丸类型注册表。
/// > <p>
/// > 以字符串 key 映射到 {@link ProjectileProvider}，取代早期写死的枚举 + switch 工厂。
/// > 内置类型（bullet/grenade/arrow）在类加载时自注册；第三方可通过 {@link #register}
/// > 在 mod setup 阶段注册新类型，无需修改本类。
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW only the bullet provider is registered; grenade, arrow and laser projectiles are not ported.
public class ProjectileRegistry {
    public static final String DEFAULT_KEY = "bullet";

    private static final Map<String, ProjectileProvider> PROVIDERS = new ConcurrentHashMap<>();

    static {
        register(DEFAULT_KEY, new BulletProvider());
    }

    /// > 注册一个弹丸类型。key 大小写不敏感（内部转小写存储）。
    public static void register(String key, ProjectileProvider provider) {
        PROVIDERS.put(normalize(key), provider);
    }

    /// > 按 key 获取 provider，找不到时回退到默认（bullet）。
    public static ProjectileProvider get(@Nullable String key) {
        if (key == null || key.isEmpty()) {
            return PROVIDERS.get(DEFAULT_KEY);
        }
        return PROVIDERS.getOrDefault(normalize(key), PROVIDERS.get(DEFAULT_KEY));
    }

    private static String normalize(String key) {
        return key.toLowerCase(Locale.ROOT);
    }

    /// > ===== 内置类型 provider =====

    private static class BulletProvider implements ProjectileProvider {
        @Override
        public AbstractGunProjectile create(net.minecraft.world.level.Level level) {
            return new BulletEntity(level);
        }
    }
}
