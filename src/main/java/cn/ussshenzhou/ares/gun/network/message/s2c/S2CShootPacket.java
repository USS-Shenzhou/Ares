package cn.ussshenzhou.ares.gun.network.message.s2c;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.t88.network.annotation.ClientHandler;
import cn.ussshenzhou.t88.network.annotation.Decoder;
import cn.ussshenzhou.t88.network.annotation.Encoder;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import cn.ussshenzhou.ares.gun.gameplay.MuzzleReference;
import cn.ussshenzhou.ares.gun.client.event.ClientShootEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;

/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public class S2CShootPacket {
    private int shooterId;
    private InteractionHand hand;
    private MuzzleReference muzzleReference;

    public S2CShootPacket(int shooterId, InteractionHand hand, MuzzleReference muzzleReference) {
        this.shooterId = shooterId;
        this.hand = hand;
        this.muzzleReference = muzzleReference;
    }

    @Encoder
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(shooterId);
        buf.writeBoolean(hand == InteractionHand.OFF_HAND);
        muzzleReference.write(buf);
    }

    @Decoder
    public S2CShootPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(),
                buf.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                MuzzleReference.read(buf));
    }

    @ClientHandler
    public void clientHandler(IPayloadContext context) {
        ClientShootEventHandler.onShootPacket(shooterId, hand, muzzleReference);
    }
}
