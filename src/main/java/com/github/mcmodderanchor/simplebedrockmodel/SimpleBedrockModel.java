package com.github.mcmodderanchor.simplebedrockmodel;

import cn.ussshenzhou.ares.Ares;
import com.github.mcmodderanchor.simplebedrockmodel.v1.network.NetworkHandler;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.config.WorldMeshConfig;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/// SBM is bundled into Ares rather than loaded as a separate mod: it uses the Ares mod id and is initialized from the Ares constructor.
public class SimpleBedrockModel {
    public static final String MOD_ID = Ares.MOD_ID;
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static void init(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(NetworkHandler::register);
        // FIXME RENDER modContainer.registerConfig(ModConfig.Type.CLIENT, WorldMeshConfig.SPEC, "ares-simplebedrockmodel-client.toml");
    }

    public static Identifier modLoc(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
