package cn.ussshenzhou.ares.gun.network.message.c2s;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.t88.network.annotation.Codec;
import cn.ussshenzhou.t88.network.annotation.ServerHandler;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW all 20 messages under network/message are T88 @NetPacket records. STREAM_CODEC passes the TACZ encode and
// decode logic to StreamCodec.ofMember as lambdas. C2SBindCharmMessage, C2SRefitSetAdapterMessage and
// C2SRefitSetVariantMessage keep static encode and decode methods.
@NetPacket(modid = Ares.MOD_ID)
public record C2SAimMessage(boolean aiming, InteractionHand hand) {
    @Codec
    public static final StreamCodec<FriendlyByteBuf, C2SAimMessage> STREAM_CODEC = StreamCodec.ofMember(
            (msg, buf) -> {
                buf.writeBoolean(msg.aiming);
                buf.writeEnum(msg.hand);
            },
            buf -> new C2SAimMessage(buf.readBoolean(), buf.readEnum(InteractionHand.class))
    );

    public C2SAimMessage(boolean aiming) {
        this(aiming, InteractionHand.MAIN_HAND);
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        IGunOperator.asGunHolder(player).ifPresent(op -> op.aim(hand, aiming));
    }
}
