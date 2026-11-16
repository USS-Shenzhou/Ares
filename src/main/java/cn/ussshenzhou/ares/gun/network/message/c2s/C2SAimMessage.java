package cn.ussshenzhou.ares.gun.network.message.c2s;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.t88.network.annotation.ServerHandler;
import cn.ussshenzhou.t88.network.annotation.Decoder;
import cn.ussshenzhou.t88.network.annotation.Encoder;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import cn.ussshenzhou.ares.gun.capability.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW all 20 messages under network/message are converted to T88 @NetPacket classes: the @Decoder constructor reads,
// write() encodes.
@NetPacket(modid = Ares.MOD_ID)
public class C2SAimMessage {
    private boolean aiming;
    private InteractionHand hand;

    public C2SAimMessage(boolean aiming, InteractionHand hand) {
        this.aiming = aiming;
        this.hand = hand;
    }

    public C2SAimMessage(boolean aiming) {
        this(aiming, InteractionHand.MAIN_HAND);
    }

    @Encoder
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(aiming);
        buf.writeEnum(hand);
    }

    @Decoder
    public C2SAimMessage(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readEnum(InteractionHand.class));
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        IGunOperator.asGunHolder(player).ifPresent(op -> op.aim(hand, aiming));
    }
}
