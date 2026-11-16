package cn.ussshenzhou.ares.gun.init;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.capability.IGunOperator;
import cn.ussshenzhou.ares.gun.capability.LocalGunOperator;
import cn.ussshenzhou.ares.gun.capability.ServerPlayerGunCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/// The gun holder is a non-serialized data attachment on players.
/// It is created lazily on first access and recreated for the new player entity after respawn.
/// @author TACZ, USS_Shenzhou
public class ModCapabilities {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Ares.MOD_ID);

    // FIXME REVIEW the Forge capability (AttachCapabilitiesEvent, GunHolderCapabilityProvider, @AutoRegisterCapability) is
    // replaced with this NeoForge data attachment.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<IGunOperator>> GUN_HOLDER_CAPABILITY =
            ATTACHMENT_TYPES.register("gun_holder", () -> AttachmentType.builder(ModCapabilities::createGunHolder).build());

    private static IGunOperator createGunHolder(IAttachmentHolder holder) {
        if (holder instanceof ServerPlayer serverPlayer) {
            return new ServerPlayerGunCapability(serverPlayer);
        }
        if (holder instanceof Player player) {
            return new LocalGunOperator(player);
        }
        throw new IllegalArgumentException("Gun holder can only be attached to players, got " + holder);
    }
}
