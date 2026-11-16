package cn.ussshenzhou.ares.gun.network.message.s2c;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.t88.network.annotation.ClientHandler;
import cn.ussshenzhou.t88.network.annotation.Decoder;
import cn.ussshenzhou.t88.network.annotation.Encoder;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import cn.ussshenzhou.ares.gun.client.handler.RenderCrosshairHandler;
import net.minecraft.network.FriendlyByteBuf;

/// > 服务端权威命中反馈（命中/爆头/击杀），定向发给射手本人，驱动客户端命中标记。
/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public class S2CGunHitMessage {
    private boolean headshot;
    private boolean kill;

    public S2CGunHitMessage(boolean headshot, boolean kill) {
        this.headshot = headshot;
        this.kill = kill;
    }

    @Encoder
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(headshot);
        buf.writeBoolean(kill);
    }

    @Decoder
    public S2CGunHitMessage(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readBoolean());
    }

    @ClientHandler
    public void clientHandler(IPayloadContext context) {
        RenderCrosshairHandler.markHit(headshot, kill);
    }
}
