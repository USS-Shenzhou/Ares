package cn.ussshenzhou.ares.gun.init;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.item.AttachmentType;
import cn.ussshenzhou.ares.gun.custom.CustomItemType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW Forge IForgeRegistry replaced with NewRegistryEvent.create. The *_SUPPLIER fields became *_REGISTRY fields holding
// the Registry, and RegistryObject became DeferredHolder here and in ModCustomItemTypes and ModAttachmentTypes.
@EventBusSubscriber(modid = Ares.MOD_ID)
public class ModRegistries {
    public static final ResourceKey<Registry<CustomItemType<?, ?>>> CUSTOM_ITEM_TYPE = ResourceKey.createRegistryKey(
            Ares.modLoc("custom_item_type")
    );
    public static Registry<CustomItemType<?, ?>> CUSTOM_ITEM_TYPE_REGISTRY;

    public static final ResourceKey<Registry<AttachmentType>> ATTACHMENT_TYPE = ResourceKey.createRegistryKey(
            Ares.modLoc("attachment_type")
    );
    public static Registry<AttachmentType> ATTACHMENT_TYPE_REGISTRY;

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        CUSTOM_ITEM_TYPE_REGISTRY = event.create(new RegistryBuilder<>(CUSTOM_ITEM_TYPE));
        ATTACHMENT_TYPE_REGISTRY = event.create(new RegistryBuilder<>(ATTACHMENT_TYPE));
    }
}
