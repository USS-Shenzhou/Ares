package cn.ussshenzhou.ares.gun.network.message.c2s;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.t88.network.annotation.ServerHandler;
import cn.ussshenzhou.t88.network.annotation.Decoder;
import cn.ussshenzhou.t88.network.annotation.Encoder;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import cn.ussshenzhou.ares.gun.capability.IGunOperator;
import cn.ussshenzhou.ares.gun.capability.ServerPlayerGunCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public class C2SReloadMessage {
    private byte action;
    private InteractionHand hand;

    public C2SReloadMessage(byte action, InteractionHand hand) {
        this.action = action;
        this.hand = hand;
    }

    public static final byte ACTION_START = 0;
    public static final byte ACTION_CANCEL = 1;

    public C2SReloadMessage(byte action) {
        this(action, InteractionHand.MAIN_HAND);
    }

    @Encoder
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(action);
        buf.writeEnum(hand);
    }

    @Decoder
    public C2SReloadMessage(FriendlyByteBuf buf) {
        this(buf.readByte(), buf.readEnum(InteractionHand.class));
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        IGunOperator.asGunHolder(player).ifPresent(op -> {
            if (action == ACTION_START) {
                op.reload(hand);
            } else if (action == ACTION_CANCEL) {
                if (op instanceof ServerPlayerGunCapability serverOp) {
                    serverOp.cancelReload(hand);
                }
            }
        });
    }
}
