package cn.ussshenzhou.ares.gun.network.message.c2s;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.t88.network.annotation.ServerHandler;
import cn.ussshenzhou.t88.network.annotation.Decoder;
import cn.ussshenzhou.t88.network.annotation.Encoder;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.attachment.entity.ServerPlayerGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/// > 客户端拉栓意图。无负载——服务端读主手枪械自行判定。
/// > <p>
/// > 用于客户端驱动的自动上膛恢复：客户端检测到「持枪 + 弹匣有弹 + 未上膛」时发送，
/// > 服务端在 {@link ServerPlayerGunOperator#requestChamber()} 中同条件二次校验后执行补膛。
/// > 补膛是弹药从弹匣移到膛、总量守恒，重复请求无害（!chambered 守卫），无作弊面。
/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public class C2SBoltMessage {
    private InteractionHand hand;

    public C2SBoltMessage(InteractionHand hand) {
        this.hand = hand;
    }

    public C2SBoltMessage() {
        this(InteractionHand.MAIN_HAND);
    }

    @Encoder
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
    }

    @Decoder
    public C2SBoltMessage(FriendlyByteBuf buf) {
        this(buf.readEnum(InteractionHand.class));
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        IGunOperator.asGunHolder(player).ifPresent(op -> {
            if (op instanceof ServerPlayerGunOperator serverOp) {
                serverOp.requestChamber(hand);
            }
        });
    }
}
