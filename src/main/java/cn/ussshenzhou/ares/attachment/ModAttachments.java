package cn.ussshenzhou.ares.attachment;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.attachment.entity.LocalGunOperator;
import cn.ussshenzhou.ares.attachment.entity.ServerPlayerGunCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/// The gun holder is a non-serialized data attachment on players.
/// It is created lazily on first access and recreated for the new player entity after respawn.
/// @author TACZ, USS_Shenzhou
public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Ares.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<IGunOperator>> GUN_HOLDER_CAPABILITY =
            ATTACHMENT_TYPES.register("gun_holder", () -> AttachmentType.<IGunOperator>builder(holder -> {
                if (holder instanceof ServerPlayer serverPlayer) {
                    return new ServerPlayerGunCapability(serverPlayer);
                }
                if (holder instanceof Player player) {
                    return new LocalGunOperator(player);
                }
                throw new IllegalArgumentException("Gun holder can only be attached to players, got " + holder);
            }).build());
}
