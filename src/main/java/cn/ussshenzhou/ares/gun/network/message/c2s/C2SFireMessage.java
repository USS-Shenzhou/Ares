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

/// > 开火 C2S。{@code chargeTicks} 仅在 {@link #ACTION_RELEASE} 时有意义：客户端松手时随包上报的
/// > 蓄力原始累积量（tick 数），服务端据此用同一脚本纯函数重算倍率（不上报倍率，见蓄力权威模型）。
/// > PRESS 时恒 0。
/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public class C2SFireMessage {
    private float yaw;
    private float pitch;
    private byte action;
    private int chargeTicks;
    private InteractionHand hand;

    public C2SFireMessage(float yaw, float pitch, byte action, int chargeTicks, InteractionHand hand) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.action = action;
        this.chargeTicks = chargeTicks;
        this.hand = hand;
    }

    public static final byte ACTION_PRESS = 0;
    public static final byte ACTION_RELEASE = 1;

    public C2SFireMessage(float yaw, float pitch, byte action, InteractionHand hand) {
        this(yaw, pitch, action, 0, hand);
    }

    public C2SFireMessage(float yaw, float pitch, byte action) {
        this(yaw, pitch, action, 0, InteractionHand.MAIN_HAND);
    }

    @Encoder
    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeByte(action);
        buf.writeVarInt(chargeTicks);
        buf.writeEnum(hand);
    }

    @Decoder
    public C2SFireMessage(FriendlyByteBuf buf) {
        this(buf.readFloat(), buf.readFloat(), buf.readByte(), buf.readVarInt(),
                buf.readEnum(InteractionHand.class));
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        IGunOperator.asGunHolder(player).ifPresent(op -> {
            if (op instanceof ServerPlayerGunOperator serverOp) {
                if (action == ACTION_PRESS) {
                    serverOp.fireWithDirection(hand, yaw, pitch);
                } else if (action == ACTION_RELEASE) {
                    serverOp.fireRelease(hand, yaw, pitch, chargeTicks);
                }
            } else {
                op.fire(hand);
            }
        });
    }
}
