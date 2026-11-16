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

/// > 改装界面设置槽位适配器请求（C2S，服务端权威）。
/// >
/// > @param slotIndex 枪械所在的背包槽位索引
/// >
/// > @param path      目标槽位路径（末端为要设置适配器的槽位）
/// >
/// > @param adapterId 适配器 index id；为空表示清除适配器
/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public record C2SRefitSetAdapterMessage(int slotIndex, AttachmentPath path, @Nullable Identifier adapterId) {
    @Codec
    public static final StreamCodec<FriendlyByteBuf, C2SRefitSetAdapterMessage> STREAM_CODEC = StreamCodec.ofMember(C2SRefitSetAdapterMessage::encode, C2SRefitSetAdapterMessage::decode);

    public static void encode(C2SRefitSetAdapterMessage msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.slotIndex);
        AttachmentPath.encode(msg.path, buf);
        buf.writeBoolean(msg.adapterId != null);
        if (msg.adapterId != null) {
            buf.writeIdentifier(msg.adapterId);
        }
    }

    public static C2SRefitSetAdapterMessage decode(FriendlyByteBuf buf) {
        int slotIndex = buf.readVarInt();
        AttachmentPath path = AttachmentPath.decode(buf);
        Identifier adapterId = buf.readBoolean() ? buf.readIdentifier() : null;
        return new C2SRefitSetAdapterMessage(slotIndex, path, adapterId);
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        ItemStack gun = player.getInventory().getItem(slotIndex);
        if (!(gun.getItem() instanceof IGun)) {
            return;
        }
        AttachmentTreeHelper.setSlotAdapter(gun, path, adapterId);
    }
}
