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

/// > 统一的模式切换消息。
/// > modeIndex >= 0：切换到对应的开火模式索引（主枪模式）。
/// > modeIndex == UNDER_BARREL_MODE：切换到下挂武器模式。
/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public class C2SFireModeSwitchMessage {
    private int modeIndex;
    private InteractionHand hand;

    public C2SFireModeSwitchMessage(int modeIndex, InteractionHand hand) {
        this.modeIndex = modeIndex;
        this.hand = hand;
    }

    public C2SFireModeSwitchMessage(int modeIndex) {
        this(modeIndex, InteractionHand.MAIN_HAND);
    }
    public static final int UNDER_BARREL_MODE = -1;

    @Encoder
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(modeIndex);
        buf.writeEnum(hand);
    }

    @Decoder
    public C2SFireModeSwitchMessage(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readEnum(InteractionHand.class));
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        IGunOperator.asGunHolder(player).ifPresent(op -> {
            if (op instanceof ServerPlayerGunCapability serverOp) {
                serverOp.switchCombinedMode(hand, modeIndex);
            } else {
                op.setModuleMode(hand, modeIndex == UNDER_BARREL_MODE
                        ? cn.ussshenzhou.ares.gun.capability.GunModuleMode.UNDER_BARREL
                        : cn.ussshenzhou.ares.gun.capability.GunModuleMode.PRIMARY);
            }
        });
    }
}
