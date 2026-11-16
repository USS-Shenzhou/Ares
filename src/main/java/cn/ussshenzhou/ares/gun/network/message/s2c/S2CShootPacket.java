package cn.ussshenzhou.ares.gun.network.message.s2c;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.t88.network.annotation.ClientHandler;
import cn.ussshenzhou.t88.network.annotation.Codec;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import cn.ussshenzhou.ares.gun.gameplay.MuzzleReference;
import cn.ussshenzhou.ares.gun.client.event.ClientShootEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;

/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public record S2CShootPacket(int shooterId, InteractionHand hand, MuzzleReference muzzleReference) {
    @Codec
    public static final StreamCodec<FriendlyByteBuf, S2CShootPacket> STREAM_CODEC = StreamCodec.ofMember(
            (pkt, buf) -> {
                buf.writeVarInt(pkt.shooterId);
                buf.writeBoolean(pkt.hand == InteractionHand.OFF_HAND);
                pkt.muzzleReference.write(buf);
            },
            buf -> new S2CShootPacket(buf.readVarInt(),
                    buf.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                    MuzzleReference.read(buf))
    );

    @ClientHandler
    public void clientHandler(IPayloadContext context) {
        ClientShootEventHandler.onShootPacket(shooterId, hand, muzzleReference);
    }
}
