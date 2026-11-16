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

/// > 改装界面设置配件开关状态请求（C2S，服务端权威）。
/// >
/// > @param slotIndex 枪械所在的背包槽位索引
/// >
/// > @param path      目标槽位路径（末端槽位上的配件被切换）
/// >
/// > @param enabled   目标开关状态
/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public class C2SRefitSetToggleMessage {
    private int slotIndex;
    private AttachmentPath path;
    private boolean enabled;

    public C2SRefitSetToggleMessage(int slotIndex, AttachmentPath path, boolean enabled) {
        this.slotIndex = slotIndex;
        this.path = path;
        this.enabled = enabled;
    }

    @Encoder
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(slotIndex);
        AttachmentPath.encode(path, buf);
        buf.writeBoolean(enabled);
    }

    @Decoder
    public C2SRefitSetToggleMessage(FriendlyByteBuf buf) {
        int slotIndex = buf.readVarInt();
        AttachmentPath path = AttachmentPath.decode(buf);
        boolean enabled = buf.readBoolean();
        this(slotIndex, path, enabled);
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        ItemStack gun = player.getInventory().getItem(slotIndex);
        if (!(gun.getItem() instanceof IGun)) {
            return;
        }
        AttachmentTreeHelper.setToggle(gun, path, enabled);
    }
}