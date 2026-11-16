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
public class C2SRefitSetVariantMessage {
    private int slotIndex;
    private AttachmentPath path;
    @Nullable
    private Identifier variantId;

    public C2SRefitSetVariantMessage(int slotIndex, AttachmentPath path, @Nullable Identifier variantId) {
        this.slotIndex = slotIndex;
        this.path = path;
        this.variantId = variantId;
    }

    @Encoder
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(slotIndex);
        AttachmentPath.encode(path, buf);
        buf.writeBoolean(variantId != null);
        if (variantId != null) {
            buf.writeIdentifier(variantId);
        }
    }

    @Decoder
    public C2SRefitSetVariantMessage(FriendlyByteBuf buf) {
        int slotIndex = buf.readVarInt();
        AttachmentPath path = AttachmentPath.decode(buf);
        Identifier variantId = buf.readBoolean() ? buf.readIdentifier() : null;
        this(slotIndex, path, variantId);
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
