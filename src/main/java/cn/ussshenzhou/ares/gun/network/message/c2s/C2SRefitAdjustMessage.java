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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/// > 改装界面导轨微调请求（C2S，服务端权威）。
/// >
/// > @param slotIndex 枪械所在的背包槽位索引
/// > @param path      目标槽位路径（多层树，末端为要微调的槽位）
/// > @param offset    沿导轨方向的偏移标量
/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public class C2SRefitAdjustMessage {
    private int slotIndex;
    private AttachmentPath path;
    private float offset;

    public C2SRefitAdjustMessage(int slotIndex, AttachmentPath path, float offset) {
        this.slotIndex = slotIndex;
        this.path = path;
        this.offset = offset;
    }

    @Encoder
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(slotIndex);
        AttachmentPath.encode(path, buf);
        buf.writeFloat(offset);
    }

    @Decoder
    public C2SRefitAdjustMessage(FriendlyByteBuf buf) {
        int slotIndex = buf.readVarInt();
        AttachmentPath path = AttachmentPath.decode(buf);
        float offset = buf.readFloat();
        this(slotIndex, path, offset);
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        ItemStack gun = player.getInventory().getItem(slotIndex);
        if (!(gun.getItem() instanceof IGun)) {
            return;
        }
        AttachmentTreeHelper.adjust(gun, path, offset);
    }
}
