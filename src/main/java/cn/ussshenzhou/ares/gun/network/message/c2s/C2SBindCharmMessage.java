package cn.ussshenzhou.ares.gun.network.message.c2s;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.t88.network.annotation.ServerHandler;
import cn.ussshenzhou.t88.network.annotation.Decoder;
import cn.ussshenzhou.t88.network.annotation.Encoder;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import cn.ussshenzhou.ares.gun.item.ICustomItem;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.gun.custom.charm.GunCharmData;
import cn.ussshenzhou.ares.gun.item.CharmItem;
import cn.ussshenzhou.ares.gun.util.CustomItemDataHelper;
import cn.ussshenzhou.ares.gun.util.GunCharmDataHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.joml.Vector3f;

/// > 改装界面绑定挂饰请求（C2S，服务端权威）。
/// >
/// > 客户端在改装界面拾取枪体 cube 解出 `boneName` / `localHit` / `localNormal` /
/// > `localOffset`（骨骼局部空间），连同被选挂饰所在背包槽位发往服务端。服务端校验后取出 1 个
/// > 挂饰、归还旧挂饰（若有），并把结合数据写入枪械 NBT（[GunCharmDataHelper]）。
/// >
/// > @param gunSlotIndex 枪械所在背包槽位索引
/// >
/// > @param charmInvSlot 被选挂饰所在背包槽位索引
/// >
/// > @param displayId    枪械 display id（绑定时记录，用于渲染端校验模型一致）
/// >
/// > @param boneName     结合到枪体的骨骼名
/// >
/// > @param localHit     骨骼局部命中点（像素）
/// >
/// > @param localNormal  局部命中面法线（单位向量）
/// >
/// > @param localOffset  局部附加偏移（像素，沿法线离面）
/// @author TACZ, USS_Shenzhou
@NetPacket(modid = Ares.MOD_ID)
public class C2SBindCharmMessage {
    private int gunSlotIndex;
    private int charmInvSlot;
    private Identifier displayId;
    private String boneName;
    private Vector3f localHit;
    private Vector3f localNormal;
    private Vector3f localOffset;

    public C2SBindCharmMessage(int gunSlotIndex, int charmInvSlot, Identifier displayId, String boneName, Vector3f localHit, Vector3f localNormal, Vector3f localOffset) {
        this.gunSlotIndex = gunSlotIndex;
        this.charmInvSlot = charmInvSlot;
        this.displayId = displayId;
        this.boneName = boneName;
        this.localHit = localHit;
        this.localNormal = localNormal;
        this.localOffset = localOffset;
    }

    @Encoder
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(gunSlotIndex);
        buf.writeVarInt(charmInvSlot);
        buf.writeIdentifier(displayId);
        buf.writeUtf(boneName);
        writeVec(buf, localHit);
        writeVec(buf, localNormal);
        writeVec(buf, localOffset);
    }

    @Decoder
    public C2SBindCharmMessage(FriendlyByteBuf buf) {
        int gunSlotIndex = buf.readVarInt();
        int charmInvSlot = buf.readVarInt();
        Identifier displayId = buf.readIdentifier();
        String boneName = buf.readUtf();
        Vector3f localHit = readVec(buf);
        Vector3f localNormal = readVec(buf);
        Vector3f localOffset = readVec(buf);
        this(gunSlotIndex, charmInvSlot, displayId, boneName, localHit, localNormal, localOffset);
    }

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        Inventory inventory = player.getInventory();
        ItemStack gun = inventory.getItem(gunSlotIndex);
        IGun iGun = IGun.asGun(gun);
        if (iGun == null) {
            return;
        }
        /// > 校验 display id 一致，避免界面与权威枪不匹配
        if (!iGun.getDisplayId(gun).equals(displayId)) {
            return;
        }
        ItemStack charmSource = inventory.getItem(charmInvSlot);
        if (!(charmSource.getItem() instanceof CharmItem)) {
            return;
        }
        Identifier charmId = CustomItemDataHelper.getCustomId(charmSource);
        if (charmId == null || ICustomItem.EMPTY_ID.equals(charmId)) {
            return;
        }
        /// > 取出 1 个挂饰
        ItemStack boundCharm = charmSource.copyWithCount(1);
        /// > 归还旧挂饰（若已绑定）
        for (ItemStack old : GunCharmDataHelper.removeAll(gun)) {
            if (!old.isEmpty()) {
                // NEEDCHECK SERVER_ONLY follows the vanilla server-side callers.
                inventory.placeItemBackInInventory(old, Prediction.SERVER_ONLY);
            }
        }
        GunCharmData data = new GunCharmData(ItemStackTemplate.fromNonEmptyStack(boundCharm), displayId, boneName,
                localHit, localNormal, localOffset);
        GunCharmDataHelper.setSingle(gun, data);
        charmSource.shrink(1);
        inventory.setChanged();
    }

    private static void writeVec(FriendlyByteBuf buf, Vector3f v) {
        buf.writeFloat(v.x);
        buf.writeFloat(v.y);
        buf.writeFloat(v.z);
    }

    private static Vector3f readVec(FriendlyByteBuf buf) {
        return new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }
}
