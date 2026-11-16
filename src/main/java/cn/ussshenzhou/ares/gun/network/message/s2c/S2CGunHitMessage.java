package cn.ussshenzhou.ares.gun.network.message.s2c;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.t88.network.annotation.ClientHandler;
import cn.ussshenzhou.t88.network.annotation.Codec;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import cn.ussshenzhou.ares.client.handler.RenderCrosshairHandler;
import net.minecraft.network.FriendlyByteBuf;

/// > 服务端权威命中反馈（命中/爆头/击杀），定向发给射手本人，驱动客户端命中标记。
/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public record S2CGunHitMessage(boolean headshot, boolean kill) {
    @Codec
    public static final StreamCodec<FriendlyByteBuf, S2CGunHitMessage> STREAM_CODEC = StreamCodec.ofMember(
            (pkt, buf) -> {
                buf.writeBoolean(pkt.headshot);
                buf.writeBoolean(pkt.kill);
            },
            buf -> new S2CGunHitMessage(buf.readBoolean(), buf.readBoolean())
    );

    @ClientHandler
    public void clientHandler(IPayloadContext context) {
        RenderCrosshairHandler.markHit(headshot, kill);
    }
}
