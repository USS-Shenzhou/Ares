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
import cn.ussshenzhou.ares.gun.util.ToggleHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/// > 快捷键翻转枪械配件树上指定效果的开关状态请求（C2S，服务端权威）。
/// >
/// > 服务端对枪自身效果键（若该枪有对应效果 display）与配件树中全部「当前生效形态声明了
/// > 该效果」的可开关配件逐个取反，随后随物品栏同步下发权威状态（客户端预写自愈）。
/// >
/// > @param slotIndex 枪械所在的背包槽位索引
/// >
/// > @param effect    目标效果（laser / flashlight）
/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public record C2SToggleEffectMessage(int slotIndex, String effect) {
    @Codec
    public static final StreamCodec<FriendlyByteBuf, C2SToggleEffectMessage> STREAM_CODEC = StreamCodec.ofMember(
            (msg, buf) -> {
                buf.writeVarInt(msg.slotIndex);
                buf.writeUtf(msg.effect);
            },
            buf -> new C2SToggleEffectMessage(buf.readVarInt(), buf.readUtf())
    );

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (effect == null || effect.isEmpty()) {
            return;
        }
        ItemStack gun = player.getInventory().getItem(slotIndex);
        if (!(gun.getItem() instanceof IGun)) {
            return;
        }

        /// > 枪自身效果键（服务端不校验 display 声明，由客户端渲染端兜底）
        ToggleHelper.setGunEffectEnabled(gun, effect, !ToggleHelper.getGunEffectEnabled(gun, effect));

        /// > 配件树中全部匹配的可开关配件逐个取反（翻转不改变树结构，路径稳定）
        List<AttachmentPath> paths = new ArrayList<>();
        List<Boolean> enabledBefore = new ArrayList<>();
        AttachmentTreeHelper.forEachEffectToggleable(gun, effect, (path, attachment) -> {
            paths.add(path);
            enabledBefore.add(ToggleHelper.isEnabled(attachment));
        });
        for (int i = 0; i < paths.size(); i++) {
            AttachmentTreeHelper.setToggle(gun, paths.get(i), !enabledBefore.get(i));
        }
    }
}
