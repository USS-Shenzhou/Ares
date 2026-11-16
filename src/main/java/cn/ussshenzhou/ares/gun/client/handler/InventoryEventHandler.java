package cn.ussshenzhou.ares.gun.client.handler;

import net.neoforged.fml.common.EventBusSubscriber;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.event.SwapItemWithOffHand;
import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.attachment.entity.IGunOperator;
import cn.ussshenzhou.ares.attachment.entity.LocalGunOperator;
import cn.ussshenzhou.ares.gun.item.ICustomItem;
import cn.ussshenzhou.ares.gun.util.DualWieldHelper;
import cn.ussshenzhou.ares.gun.util.DualWieldSignature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

/// > 客户端持枪会话的**唯一检测器**。
/// >
/// > 客户端无稳定物品实例可追踪（任何 NBT 变化都会让网络层重建新 `ItemStack`），
/// > 故以「选中槽位 + 双持签名」为快照做值启发式检测，避免弹药 / 配件 NBT 变化被误判为切枪：
/// > - 双持签名变化（含主 / 副手持枪形态切换）→ 全量 `resync` 重判双手会话，
/// >   使主手能从单手模式重新进入双手模式（反之亦然），并触发掏枪。
/// > - 选中槽位变化 / 同槽位换成不同枪 → 触发主手收枪（收枪完成后自动掏新枪）。
/// >
/// > [LocalGunOperator] 不再自轮询，仅推进现存会话。
/// @author TACZ, USS_Shenzhou
@EventBusSubscriber(modid = Ares.MOD_ID, value = Dist.CLIENT)
public class InventoryEventHandler {
    /// > 用于切枪逻辑
    private static int oldHotbarSelected = -1;
    private static ItemStack oldHotbarSelectItem = ItemStack.EMPTY;
    private static DualWieldSignature oldDualWieldSignature = DualWieldSignature.inactive();

    @SubscribeEvent
    public static void onPlayerChangeSelect(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        ItemStack currentItem = inventory.getItem(inventory.getSelectedSlot());
        DualWieldSignature currentSignature = DualWieldHelper.signature(player);
        if (!currentSignature.equals(oldDualWieldSignature)) {
            /// > 持枪形态切换（单手双持 ↔ 双手）：全量重判双手会话并触发掏枪
            resync(player);
            updateSnapshot(player);
            return;
        }
        /// > 玩家切换选中框的情况
        if (oldHotbarSelected != inventory.getSelectedSlot()) {
            if (Minecraft.getInstance().gameMode != null) {
                Minecraft.getInstance().gameMode.ensureHasSentCarriedItem();
            }
            forceSwitchMainHand(player);

            updateSnapshot(player);
            return;
        }

        if (!isSameGun(oldHotbarSelectItem, currentItem)) {
            if (Minecraft.getInstance().gameMode != null) {
                Minecraft.getInstance().gameMode.ensureHasSentCarriedItem();
            }
            forceSwitchMainHand(player);
            updateSnapshot(player);
        }
    }

    @SubscribeEvent
    public static void onCreativeInventoryInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen)) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        forceRebuildSessions(player);
        updateSnapshot(player);
    }

    @SubscribeEvent
    public static void onSwapItemWithOffHand(SwapItemWithOffHand event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        /// > 真实换手（F 键对调两手物品）：可能同 id 同值（值启发区分不出）。
        /// > 与动画层（SimpleBedrockModel forceHandSwapFlag 强制收枪→掏枪）对齐：
        /// > 强制两手会话走收枪窗口 → 掏枪，并刷新检测快照。
        forceSwitchMainHand(player);
        forceSwitchOffHand(player);
        updateSnapshot(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        /// > 离开游戏时重置客户端 draw 状态
        oldHotbarSelected = -1;
        oldHotbarSelectItem = ItemStack.EMPTY;
        oldDualWieldSignature = DualWieldSignature.inactive();
    }

    private static void resync(LocalPlayer player) {
        IGunOperator.asGunHolder(player).ifPresent(cap -> {
            if (cap instanceof LocalGunOperator localOp) {
                localOp.resync();
            }
        });
    }

    private static void forceRebuildSessions(LocalPlayer player) {
        IGunOperator.asGunHolder(player).ifPresent(cap -> {
            if (cap instanceof LocalGunOperator localOp) {
                localOp.forceRebuildSessions();
            }
        });
    }

    private static void updateSnapshot(LocalPlayer player) {
        Inventory inventory = player.getInventory();
        oldHotbarSelected = inventory.getSelectedSlot();
        oldHotbarSelectItem = inventory.getItem(inventory.getSelectedSlot()).copy();
        oldDualWieldSignature = DualWieldHelper.signature(player);
    }

    /// > 主手强制切枪（切格子 / 同格子换枪）。客户端无稳定实例，外部检测器已确定切换发生，
    /// > 直接令主手会话起收枪窗口，绕过 operator 内的值启发式短路。
    private static void forceSwitchMainHand(LocalPlayer player) {
        IGunOperator.asGunHolder(player).ifPresent(cap -> {
            if (cap instanceof LocalGunOperator localOp) {
                localOp.forceSwitch(InteractionHand.MAIN_HAND);
            }
        });
    }

    /// > 副手强制切枪（真实换手对调）。逻辑同主手：起收枪窗口，落地时按实时手持物重绑。
    private static void forceSwitchOffHand(LocalPlayer player) {
        IGunOperator.asGunHolder(player).ifPresent(cap -> {
            if (cap instanceof LocalGunOperator localOp) {
                localOp.forceSwitch(InteractionHand.OFF_HAND);
            }
        });
    }

    /// > 判断两个物品是否是"同一把枪"。
    /// > 只比较 Item 类型和枪械自定义 ID，忽略弹药/配件等 NBT 变化。
    /// > 非自定义物品退化为 Item.is 比较。
    private static boolean isSameGun(ItemStack oldStack, ItemStack newStack) {
        if (oldStack.isEmpty() && newStack.isEmpty()) {
            return true;
        }
        if (oldStack.isEmpty() || newStack.isEmpty()) {
            return false;
        }
        if (!oldStack.is(newStack.getItem())) {
            return false;
        }

        /// > 对自定义物品，比较枪械 ID
        if (newStack.getItem() instanceof ICustomItem<?, ?> newCustom) {
            if (oldStack.getItem() instanceof ICustomItem<?, ?> oldCustom) {
                Identifier oldId = oldCustom.getCustomId(oldStack);
                Identifier newId = newCustom.getCustomId(newStack);
                return oldId.equals(newId);
            }
            return false;
        }
        return true;
    }

}

