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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/// > 变倍请求（C2S，服务端权威落地）。客户端预测先行，本消息携带激活瞄具槽位路径与目标值；
/// > 服务端宽松校验后写入配件 NBT，随常规物品同步回客户端。
/// > <p>
/// > 档位数（views_fov 长度）属于客户端资源，服务端不可知，越界由客户端消费端 {@code % length} 兜底。
/// > 无级进度 0~1 为唯一运行时状态（模型 FOV 与倍率由各自声明范围插值），范围外表示清除无级进度。
/// >
/// > @param hand        持枪手（服务端按此手写 NBT）
/// > @param path        激活瞄具的槽位路径（端点槽位上的瞄具被写入倍率）
/// > @param zoomIndex   有级档位索引（>= 0）
/// > @param zoomProgress 无级进度（0~1 生效；范围外清除）
/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public class C2SZoomMessage {
    private InteractionHand hand;
    private AttachmentPath path;
    private int zoomIndex;
    private float zoomProgress;

    public C2SZoomMessage(InteractionHand hand, AttachmentPath path, int zoomIndex, float zoomProgress) {
        this.hand = hand;
        this.path = path;
        this.zoomIndex = zoomIndex;
        this.zoomProgress = zoomProgress;
    }

    @Encoder
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
        AttachmentPath.encode(path, buf);
        buf.writeVarInt(zoomIndex);
        buf.writeFloat(zoomProgress);
    }

    @Decoder
    public C2SZoomMessage(FriendlyByteBuf buf) {
        this(buf.readEnum(InteractionHand.class), AttachmentPath.decode(buf),
                buf.readVarInt(), buf.readFloat());
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        ItemStack gun = player.getItemInHand(hand);
        if (!(gun.getItem() instanceof IGun)) {
            return;
        }
        AttachmentTreeHelper.setZoom(gun, path, zoomIndex, zoomProgress);
    }
}