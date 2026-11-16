package cn.ussshenzhou.ares.gun.network.message.c2s;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.t88.network.annotation.ServerHandler;
import cn.ussshenzhou.t88.network.annotation.Decoder;
import cn.ussshenzhou.t88.network.annotation.Encoder;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
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
public class C2SRefitInstallMessage {
    private int slotIndex;
    private AttachmentPath path;
    private Identifier attachmentId;

    public C2SRefitInstallMessage(int slotIndex, AttachmentPath path, Identifier attachmentId) {
        this.slotIndex = slotIndex;
        this.path = path;
        this.attachmentId = attachmentId;
    }

    @Encoder
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(slotIndex);
        AttachmentPath.encode(path, buf);
        buf.writeIdentifier(attachmentId);
    }

    @Decoder
    public C2SRefitInstallMessage(FriendlyByteBuf buf) {
        int slotIndex = buf.readVarInt();
        AttachmentPath path = AttachmentPath.decode(buf);
        Identifier attachmentId = buf.readIdentifier();
        this(slotIndex, path, attachmentId);
    }

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
