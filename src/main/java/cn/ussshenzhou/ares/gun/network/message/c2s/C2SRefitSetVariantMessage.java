package cn.ussshenzhou.ares.gun.network.message.c2s;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.t88.network.annotation.Codec;
import cn.ussshenzhou.t88.network.annotation.ServerHandler;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import cn.ussshenzhou.ares.gun.item.AttachmentPath;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.gun.util.AttachmentTreeHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

/// > 改装界面设置配件变体请求（C2S，服务端权威）。
/// >
/// > @param slotIndex 枪械所在的背包槽位索引
/// >
/// > @param path      目标槽位路径（末端槽位上的配件被设置变体）
/// >
/// > @param variantId 变体 index id；为空表示清除变体（恢复基础几何）
/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public record C2SRefitSetVariantMessage(int slotIndex, AttachmentPath path, @Nullable Identifier variantId) {
    @Codec
    public static final StreamCodec<FriendlyByteBuf, C2SRefitSetVariantMessage> STREAM_CODEC = StreamCodec.ofMember(C2SRefitSetVariantMessage::encode, C2SRefitSetVariantMessage::decode);

    public static void encode(C2SRefitSetVariantMessage msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.slotIndex);
        AttachmentPath.encode(msg.path, buf);
        buf.writeBoolean(msg.variantId != null);
        if (msg.variantId != null) {
            buf.writeIdentifier(msg.variantId);
        }
    }

    public static C2SRefitSetVariantMessage decode(FriendlyByteBuf buf) {
        int slotIndex = buf.readVarInt();
        AttachmentPath path = AttachmentPath.decode(buf);
        Identifier variantId = buf.readBoolean() ? buf.readIdentifier() : null;
        return new C2SRefitSetVariantMessage(slotIndex, path, variantId);
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        ItemStack gun = player.getInventory().getItem(slotIndex);
        if (!(gun.getItem() instanceof IGun)) {
            return;
        }
        AttachmentTreeHelper.setVariant(gun, path, variantId);
    }
}
