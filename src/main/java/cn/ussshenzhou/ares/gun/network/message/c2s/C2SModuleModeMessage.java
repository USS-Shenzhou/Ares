package cn.ussshenzhou.ares.gun.network.message.c2s;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.t88.network.annotation.Codec;
import cn.ussshenzhou.t88.network.annotation.ServerHandler;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import cn.ussshenzhou.ares.util.GunModuleMode;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public record C2SModuleModeMessage(byte mode) {
    @Codec
    public static final StreamCodec<FriendlyByteBuf, C2SModuleModeMessage> STREAM_CODEC = StreamCodec.ofMember(
            (msg, buf) -> buf.writeByte(msg.mode),
            buf -> new C2SModuleModeMessage(buf.readByte())
    );

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
