package com.github.mcmodderanchor.simplebedrockmodel.v1.network;

import com.github.mcmodderanchor.simplebedrockmodel.v1.network.message.ServerMessageSwapItem;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {
    private static final String VERSION = "0.1.0";

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(
                ServerMessageSwapItem.TYPE,
                ServerMessageSwapItem.STREAM_CODEC,
                ServerMessageSwapItem::handle
        );
    }

    public static void sendToClientPlayer(CustomPacketPayload message, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, message);
        }
    }

    /**
     * 发送给所有监听此实体的玩家
     */
    public static void sendToTrackingEntityAndSelf(Entity centerEntity, CustomPacketPayload message) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(centerEntity, message);
    }

    public static void sendToAllPlayers(CustomPacketPayload message) {
        PacketDistributor.sendToAllPlayers(message);
    }

    public static void sendToTrackingEntity(CustomPacketPayload message, final Entity centerEntity) {
        PacketDistributor.sendToPlayersTrackingEntity(centerEntity, message);
    }

    public static void sendToDimension(CustomPacketPayload message, final Entity centerEntity) {
        if (centerEntity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersInDimension(serverLevel, message);
        }
    }
}
