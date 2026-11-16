package cn.ussshenzhou.ares.gun.network.message.c2s;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.t88.network.annotation.ServerHandler;
import cn.ussshenzhou.t88.network.annotation.Decoder;
import cn.ussshenzhou.t88.network.annotation.Encoder;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.gun.util.GunCharmDataHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/// > 改装界面卸下挂饰请求（C2S，服务端权威）。
/// >
/// > 移除枪械 NBT 中的挂饰结合数据，并把挂饰物品归还玩家背包。
/// >
/// > @param gunSlotIndex 枪械所在背包槽位索引
/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public class C2SUnbindCharmMessage {
    private int gunSlotIndex;

    public C2SUnbindCharmMessage(int gunSlotIndex) {
        this.gunSlotIndex = gunSlotIndex;
    }

    @Encoder
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(gunSlotIndex);
    }

    @Decoder
    public C2SUnbindCharmMessage(FriendlyByteBuf buf) {
        this(buf.readVarInt());
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        Inventory inventory = player.getInventory();
        ItemStack gun = inventory.getItem(gunSlotIndex);
        if (IGun.asGun(gun) == null) {
            return;
        }
        for (ItemStack removed : GunCharmDataHelper.removeAll(gun)) {
            if (!removed.isEmpty()) {
                // NEEDCHECK SERVER_ONLY follows the vanilla server-side callers.
                inventory.placeItemBackInInventory(removed, Prediction.SERVER_ONLY);
            }
        }
        inventory.setChanged();
    }
}
