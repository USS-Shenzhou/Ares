package com.github.mcmodderanchor.simplebedrockmodel.v1.network.message;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.event.SwapItemWithOffHand;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ServerMessageSwapItem() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerMessageSwapItem> TYPE = new CustomPacketPayload.Type<>(SimpleBedrockModel.modLoc("swap_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerMessageSwapItem> STREAM_CODEC = StreamCodec.unit(new ServerMessageSwapItem());

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerMessageSwapItem message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                NeoForge.EVENT_BUS.post(new SwapItemWithOffHand());
            }
        });
    }
}
