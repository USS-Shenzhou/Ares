package cn.ussshenzhou.ares;

import cn.ussshenzhou.ares.gun.config.ClientConfig;
import cn.ussshenzhou.ares.gun.init.ModAttachmentTypes;
import cn.ussshenzhou.ares.gun.init.ModCapabilities;
import cn.ussshenzhou.ares.gun.init.ModCustomItemTypes;
import cn.ussshenzhou.ares.gun.init.ModDataComponents;
import cn.ussshenzhou.ares.gun.init.ModEntities;
import cn.ussshenzhou.ares.gun.init.ModItems;
import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;


@Mod(Ares.MOD_ID)
public class Ares {
    public static final String MOD_ID = "ares";

    // FIXME REVIEW registers every DeferredRegister of the gun module and the client config.
    public Ares(IEventBus modEventBus, ModContainer modContainer) {
        ModCustomItemTypes.CUSTOM_ITEM_TYPE.register(modEventBus);
        ModAttachmentTypes.ATTACHMENT_TYPE.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModItems.TABS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModCapabilities.ATTACHMENT_TYPES.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        SimpleBedrockModel.init(modEventBus, modContainer);
    }

    // FIXME REVIEW counterpart of TACZ GunMod.modLoc.
    public static Identifier modLoc(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }
}
