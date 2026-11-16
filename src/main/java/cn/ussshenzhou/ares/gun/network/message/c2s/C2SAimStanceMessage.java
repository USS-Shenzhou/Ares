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

/// > 客户端上报当前瞄准视图位是否为战术拒枪姿态（服务端权威落地）。
/// >
/// > 视图位列表属于客户端资源（display），服务端不可知，故只上报「姿态结果」；服务端据此决定
/// > 散布取 `inaccuracy_tactical` 还是 `inaccuracy_aim`，并只接受数据侧确实声明了
/// > 拒枪散布属性的枪（否则该属性会取到默认值 0，造成完美精度）。
/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public record C2SAimStanceMessage(boolean tactical, InteractionHand hand) {
    @Codec
    public static final StreamCodec<FriendlyByteBuf, C2SAimStanceMessage> STREAM_CODEC = StreamCodec.ofMember(
            (msg, buf) -> {
                buf.writeBoolean(msg.tactical);
                buf.writeEnum(msg.hand);
            },
            buf -> new C2SAimStanceMessage(buf.readBoolean(), buf.readEnum(InteractionHand.class))
    );

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        IGunOperator.asGunHolder(player).ifPresent(op -> op.aimStance(hand, tactical));
    }
}
