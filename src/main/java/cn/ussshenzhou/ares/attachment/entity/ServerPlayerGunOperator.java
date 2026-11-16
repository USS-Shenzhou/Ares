package cn.ussshenzhou.ares.attachment.entity;

import cn.ussshenzhou.ares.attachment.entity.session.ServerHandGunSession;
import cn.ussshenzhou.ares.gun.gameplay.ShootResult;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.gun.util.DualWieldHelper;
import cn.ussshenzhou.ares.util.GunOperationSessionKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/// > 服务端玩家持枪能力：权威执行。
/// > <p>
/// > 会话身份判定分两级：
/// > <ul>
/// >   <li><b>实例身份</b>（{@code ==}）：快速路径。物品栏在服务端权威，槽位 {@code ItemStack} 实例在
/// >       物品未被换走期间保持同一对象，NBT 原地修改不换实例。</li>
/// >   <li><b>同槽 + 值签名</b>：实例被重建（如打开创造物品栏等强同步边界）但仍是同一选中槽位、
/// >       且 Item / gunId 值签名不变时，视为同一把枪，静默重绑活引用，不重置会话。</li>
/// > </ul>
/// > 真实换手（F 键对调两手物品，可能同 id 同值）由 {@link #forceHandSwap()} 权威事件驱动强制重建会话。
/// @author TACZ, USS_Shenzhou
public class ServerPlayerGunOperator extends AbstractGunOperator<ServerPlayer, ServerHandGunSession> {
    /// > 真实换手待处理标志：事件在物品实际对调前触发，tick（物品已对调）时消费。
    private boolean handSwapPending = false;

    public ServerPlayerGunOperator(ServerPlayer player) {
        super(player);
    }

    @Override
    protected boolean isOperable(ItemStack stack) {
        return stack.getItem() instanceof IGun;
    }

    @Override
    protected ServerHandGunSession createSession(InteractionHand hand, ItemStack live, WieldProfile profile) {
        return new ServerHandGunSession(player, hand, live, profile);
    }

    @Override
    protected boolean matches(ServerHandGunSession session, ItemStack live, GunOperationSessionKind expectedKind) {
        /// > 实例身份：权威快速路径（NBT 原地修改不换实例，绝大多数 tick 走这里）
        if (session.boundInstance() == live) {
            return session.kind() == expectedKind;
        }
        /// > 实例已重建（如创造物品栏等强同步边界）：仅当仍在同一选中槽位且值签名相同
        /// > （同 Item + 同 gunId）时视为同一把枪，静默重绑活引用，不重置会话。
        /// > 副手无槽位概念，只看值签名。
        if (session.kind() != expectedKind) {
            return false;
        }
        boolean sameSlot = session.hand() != InteractionHand.MAIN_HAND
                || session.boundSlot() == player.getInventory().getSelectedSlot();
        if (sameSlot && DualWieldHelper.sameHeldGun(session.stack(), live)) {
            session.rebindInstance(live);
            return true;
        }
        return false;
    }

    @Override
    protected void beforeAccess(InteractionHand hand) {
        synchronizeSession(hand);
    }

    @Override
    public void tick() {
        if (handSwapPending) {
            handSwapPending = false;
            /// > 真实换手（F 键对调两手物品）：物品可能同 id 同值，值 + 槽启发区分不出，
            /// > 由权威换手事件驱动强制重建两手会话（取消换弹、清空时间线、发生命周期事件），
            /// > 与客户端动画（SimpleBedrockModel forceHandSwapFlag）与会话对齐。
            /// > 事件在物品实际对调前触发，此处 tick 时物品已对调，按实时手持物落地。
            for (InteractionHand hand : InteractionHand.values()) {
                synchronizeSession(hand, true);
            }
        }
        resync();
        tickSessions();
    }

    /// > 真实换手（主副手物品对调）发生：请求在下一 tick 强制重建两手会话。
    /// > 由 {@link net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent.Hands} 驱动（F 键等换手路径）。
    public void forceHandSwap() {
        handSwapPending = true;
    }

    /// > —— 服务端特有方法 ——

    public ShootResult fireWithDirection(InteractionHand hand, float yaw, float pitch) {
        /// > 玩家正在使用物品（如举盾格挡、吃东西）时禁止开火，服务端权威拦截。
        if (player.isUsingItem()) {
            return ShootResult.BLOCKED;
        }
        ServerHandGunSession session = ensureSession(hand, false);
        return session != null ? session.fireWithDirection(yaw, pitch) : ShootResult.BLOCKED;
    }

    public ShootResult fireWithDirection(float yaw, float pitch) {
        return fireWithDirection(preferredHand(), yaw, pitch);
    }

    public ShootResult fireRelease(InteractionHand hand, float yaw, float pitch, int chargeTicks) {
        ServerHandGunSession session = ensureSession(hand, false);
        return session != null ? session.fireRelease(yaw, pitch, chargeTicks) : ShootResult.BLOCKED;
    }

    public ShootResult fireRelease(float yaw, float pitch, int chargeTicks) {
        return fireRelease(preferredHand(), yaw, pitch, chargeTicks);
    }

    public void requestChamber(InteractionHand hand) {
        ServerHandGunSession session = ensureSession(hand, false);
        if (session != null) {
            session.requestChamber();
        }
    }

    public void requestChamber() {
        requestChamber(preferredHand());
    }

    public void cancelReload() {
        cancelReload(preferredHand());
    }

    public void cancelReload(InteractionHand hand) {
        ServerHandGunSession session = ensureSession(hand, false);
        if (session != null) {
            session.cancelReload();
        }
    }

    public void switchCombinedMode(InteractionHand hand, int modeIndex) {
        ServerHandGunSession session = ensureSession(hand, false);
        if (session != null) {
            session.switchCombinedMode(modeIndex);
        }
    }

    public void switchCombinedMode(int modeIndex) {
        switchCombinedMode(preferredHand(), modeIndex);
    }

    public boolean isAiming() {
        ServerHandGunSession session = session(preferredHand());
        return session != null && session.isAiming();
    }

    public int getShotCounter() {
        ServerHandGunSession session = session(preferredHand());
        return session != null ? session.getShotCounter() : 0;
    }

    public int incrementShotCounter() {
        ServerHandGunSession session = ensureSession(preferredHand(), false);
        return session != null ? session.incrementShotCounter() : 0;
    }

    public void reset() {
        sessions.clear();
    }
}
