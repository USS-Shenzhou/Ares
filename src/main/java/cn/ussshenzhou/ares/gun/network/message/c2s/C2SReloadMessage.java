package cn.ussshenzhou.ares.gun.network.message.c2s;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.t88.network.annotation.Codec;
import cn.ussshenzhou.t88.network.annotation.ServerHandler;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.attachment.entity.ServerPlayerGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public record C2SReloadMessage(byte action, InteractionHand hand) {
    public static final byte ACTION_START = 0;
    public static final byte ACTION_CANCEL = 1;

    @Codec
    public static final StreamCodec<FriendlyByteBuf, C2SReloadMessage> STREAM_CODEC = StreamCodec.ofMember(
            (msg, buf) -> {
                buf.writeByte(msg.action);
                buf.writeEnum(msg.hand);
            },
            buf -> new C2SReloadMessage(buf.readByte(), buf.readEnum(InteractionHand.class))
    );

    public C2SReloadMessage(byte action) {
        this(action, InteractionHand.MAIN_HAND);
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        IGunOperator.asGunHolder(player).ifPresent(op -> {
            if (action == ACTION_START) {
                op.reload(hand);
            } else if (action == ACTION_CANCEL) {
                if (op instanceof ServerPlayerGunOperator serverOp) {
                    serverOp.cancelReload(hand);
                }
            }
        });
    }
}
