package cn.ussshenzhou.ares.gun.network.message.c2s;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.t88.network.annotation.ServerHandler;
import cn.ussshenzhou.t88.network.annotation.Decoder;
import cn.ussshenzhou.t88.network.annotation.Encoder;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/// > 客户端上报当前瞄准视图位是否为战术拒枪姿态（服务端权威落地）。
/// > <p>
/// > 视图位列表属于客户端资源（display），服务端不可知，故只上报「姿态结果」；服务端据此决定
/// > 散布取 {@code inaccuracy_tactical} 还是 {@code inaccuracy_aim}，并只接受数据侧确实声明了
/// > 拒枪散布属性的枪（否则该属性会取到默认值 0，造成完美精度）。
/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public class C2SAimStanceMessage {
    private boolean tactical;
    private InteractionHand hand;

    public C2SAimStanceMessage(boolean tactical, InteractionHand hand) {
        this.tactical = tactical;
        this.hand = hand;
    }

    @Encoder
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(tactical);
        buf.writeEnum(hand);
    }

    @Decoder
    public C2SAimStanceMessage(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readEnum(InteractionHand.class));
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        IGunOperator.asGunHolder(player).ifPresent(op -> op.aimStance(hand, tactical));
    }
}
