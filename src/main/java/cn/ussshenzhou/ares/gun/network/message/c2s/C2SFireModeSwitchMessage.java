package cn.ussshenzhou.ares.gun.network.message.c2s;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.util.GunModuleMode;
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

/// > 统一的模式切换消息。
/// > modeIndex >= 0：切换到对应的开火模式索引（主枪模式）。
/// > modeIndex == UNDER_BARREL_MODE：切换到下挂武器模式。
/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public record C2SFireModeSwitchMessage(int modeIndex, InteractionHand hand) {
    public static final int UNDER_BARREL_MODE = -1;

    @Codec
    public static final StreamCodec<FriendlyByteBuf, C2SFireModeSwitchMessage> STREAM_CODEC = StreamCodec.ofMember(
            (msg, buf) -> {
                buf.writeVarInt(msg.modeIndex);
                buf.writeEnum(msg.hand);
            },
            buf -> new C2SFireModeSwitchMessage(buf.readVarInt(), buf.readEnum(InteractionHand.class))
    );

    public C2SFireModeSwitchMessage(int modeIndex) {
        this(modeIndex, InteractionHand.MAIN_HAND);
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        IGunOperator.asGunHolder(player).ifPresent(op -> {
            if (op instanceof ServerPlayerGunOperator serverOp) {
                serverOp.switchCombinedMode(hand, modeIndex);
            } else {
                op.setModuleMode(hand, modeIndex == UNDER_BARREL_MODE
                        ? GunModuleMode.UNDER_BARREL
                        : GunModuleMode.PRIMARY);
            }
        });
    }
}
