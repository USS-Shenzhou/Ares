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

/// > 改装界面安装配件请求（C2S，服务端权威）。
/// >
/// > @param slotIndex    枪械所在的背包槽位索引
/// >
/// > @param path         目标槽位路径（多层树）
/// >
/// > @param attachmentId 要安装的配件注册 id
/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public record C2SRefitInstallMessage(int slotIndex, AttachmentPath path, Identifier attachmentId) {
    @Codec
    public static final StreamCodec<FriendlyByteBuf, C2SRefitInstallMessage> STREAM_CODEC = StreamCodec.ofMember(
            (msg, buf) -> {
                buf.writeVarInt(msg.slotIndex);
                AttachmentPath.encode(msg.path, buf);
                buf.writeIdentifier(msg.attachmentId);
            },
            buf -> new C2SRefitInstallMessage(buf.readVarInt(), AttachmentPath.decode(buf), buf.readIdentifier())
    );

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        ItemStack gun = player.getInventory().getItem(slotIndex);
        if (!(gun.getItem() instanceof IGun)) {
            return;
        }
        AttachmentTreeHelper.install(gun, path, attachmentId);
    }
}
