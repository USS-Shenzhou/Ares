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
public record C2SRefitSetToggleMessage(int slotIndex, AttachmentPath path, boolean enabled) {
    @Codec
    public static final StreamCodec<FriendlyByteBuf, C2SRefitSetToggleMessage> STREAM_CODEC = StreamCodec.ofMember(
            (msg, buf) -> {
                buf.writeVarInt(msg.slotIndex);
                AttachmentPath.encode(msg.path, buf);
                buf.writeBoolean(msg.enabled);
            },
            buf -> new C2SRefitSetToggleMessage(buf.readVarInt(), AttachmentPath.decode(buf), buf.readBoolean())
    );

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
