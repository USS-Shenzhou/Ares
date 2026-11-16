package cn.ussshenzhou.ares.gun.network.message.c2s;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.t88.network.annotation.ServerHandler;
import cn.ussshenzhou.t88.network.annotation.Decoder;
import cn.ussshenzhou.t88.network.annotation.Encoder;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import cn.ussshenzhou.ares.gun.capability.GunModuleMode;
import cn.ussshenzhou.ares.gun.capability.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public class C2SModuleModeMessage {
    private byte mode;

    public C2SModuleModeMessage(byte mode) {
        this.mode = mode;
    }

    @Encoder
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(mode);
    }

    @Decoder
    public C2SModuleModeMessage(FriendlyByteBuf buf) {
        this(buf.readByte());
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        GunModuleMode[] modes = GunModuleMode.values();
        if (mode < 0 || mode >= modes.length) {
            return;
        }
        IGunOperator.asGunHolder(player).ifPresent(op -> op.setModuleMode(modes[mode]));
    }
}
