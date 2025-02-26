package cn.ussshenzhou.ares;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;


/**
 * @author USS_Shenzhou
 */
@Mod(Ares.MODID)
public class Ares {
    public static final String MODID = "ares";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Ares(IEventBus modEventBus, ModContainer modContainer) {
    }
}
