package cn.ussshenzhou.ares.gun.init;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.entity.BulletEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/// @author TACZ, USS_Shenzhou
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Ares.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<BulletEntity>> BULLET =
            ENTITIES.register("bullet", () ->
                    EntityType.Builder.<BulletEntity>of(BulletEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(5)
                            .updateInterval(1)
                            .setShouldReceiveVelocityUpdates(false)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Ares.modLoc("bullet"))));
}
