package cn.ussshenzhou.ares.attachment.entity;

import cn.ussshenzhou.ares.gun.util.DualWieldHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

/// > 持枪 operator 的共享骨架：承载 per-hand 会话表、会话同步、preferredHand 路由。
/// > <p>
/// > 端差异由两点抽象方法收敛：
/// > <ul>
/// >   <li>{@link #isOperable(ItemStack)}：哪些物品可建立会话（服务端仅 {@code IGun}，客户端含独立下挂）。</li>
/// >   <li>{@link #matches(HandGunSession, ItemStack, GunOperationSessionKind)}：会话是否仍对应当前手持物。
/// >       服务端以实例身份（{@code ==}）为快速路径，实例被重建时回退「同槽 + 值签名」判定（权威可靠），
/// >       客户端用值启发式（无稳定实例）。</li>
/// > </ul>
/// > 会话的创建 / 销毁是生命周期的唯一权威入口，全部经由 {@link #synchronizeSession(InteractionHand)}。
/// > 但「何时触发同步」两端不同：
/// > <ul>
/// >   <li>服务端：{@link #tick()} 每刻 resync，且 {@link #onBeforeAccess} 在访问会话前即时同步
/// >       （实例身份比较廉价且权威）。</li>
/// >   <li>客户端：{@link #tick()} 不 resync、访问不自轮询；改由 {@code InventoryEventHandler}
/// >       作为唯一检测器，在槽位 / 物品变化时调 {@code putAway}、在持枪形态切换时调 {@link #resync()}。</li>
/// > </ul>
/// >
/// > @param <P> 玩家类型
/// > @param <S> 会话类型
/// @author TACZ, USS_Shenzhou
public abstract class AbstractGunOperator<P extends Player, S extends HandGunSession> implements IGunOperator {
    protected final P player;
    protected final EnumMap<InteractionHand, S> sessions = new EnumMap<>(InteractionHand.class);

    protected AbstractGunOperator(P player) {
        this.player = player;
    }

    /// > —— 抽象端差异 ——

    /// > 该物品是否可建立持枪会话。
    protected abstract boolean isOperable(ItemStack stack);

    /// > 创建一个绑定到 live 物品的会话。
    protected abstract S createSession(InteractionHand hand, ItemStack live, WieldProfile profile);

    /// > 现有会话是否仍对应当前手持物（含形态 kind）。
    protected abstract boolean matches(S session, ItemStack live, GunOperationSessionKind expectedKind);

    /// > 会话被销毁（收枪）时的副作用。服务端发生命周期事件，客户端默认空实现。
    protected void onSessionPutAway(ItemStack oldStack, ItemStack newStack) {
    }

    /// > 会话被创建（掏枪）时的副作用。服务端发生命周期事件，客户端默认空实现。
    protected void onSessionDrawn(ItemStack oldStack, ItemStack newStack) {
    }

    /// > —— 会话同步（生命周期唯一入口）——

    /// > 全量重判双手会话。客户端检测器与服务端 tick 均经此入口。
    public void resync() {
        for (InteractionHand hand : InteractionHand.values()) {
            synchronizeSession(hand);
        }
    }

    protected void synchronizeSession(InteractionHand hand) {
        synchronizeSession(hand, false);
    }

    /// > 会话同步。{@code forceRebuild} 为真时跳过 {@link #matches} 短路，强制拆除并重建会话，
    /// > 事件上下文（old/new）由本方法统一计算。用于「值层面无差异但确需重建」的权威事件
    /// > （如真实换手对调两把同 id 同值枪，服务端经 {@code ServerPlayerGunCapability} 使用）。
    protected void synchronizeSession(InteractionHand hand, boolean forceRebuild) {
        ItemStack live = player.getItemInHand(hand);
        GunOperationSessionKind expectedKind = resolveSessionKind(hand, live);
        S current = sessions.get(hand);
        if (expectedKind == null) {
            if (current != null) {
                ItemStack oldStack = current.stack();
                current.putAway(live);
                sessions.remove(hand);
                onSessionPutAway(oldStack, live);
            }
            return;
        }
        if (!forceRebuild && current != null && matches(current, live, expectedKind)) {
            return;
        }
        ItemStack oldStack = ItemStack.EMPTY;
        if (current != null) {
            oldStack = current.stack();
            current.putAway(live);
            onSessionPutAway(oldStack, live);
        }
        S next = createSession(hand, live, profileFor(expectedKind));
        sessions.put(hand, next);
        next.draw(live);
        onSessionDrawn(oldStack, live);
    }

    @Nullable
    protected GunOperationSessionKind resolveSessionKind(InteractionHand hand, ItemStack stack) {
        if (!isOperable(stack)) {
            return null;
        }
        /// > 副手为单手形态 → ONE_HAND；非单手形态（双手枪放副手）副手不建立会话。
        if (DualWieldHelper.isOneHandedForm(player, hand)) {
            return GunOperationSessionKind.ONE_HAND;
        }
        return hand == InteractionHand.MAIN_HAND ? GunOperationSessionKind.TWO_HAND : null;
    }

    protected static WieldProfile profileFor(GunOperationSessionKind kind) {
        return kind == GunOperationSessionKind.ONE_HAND ? OneHandProfile.INSTANCE : TwoHandProfile.INSTANCE;
    }

    @Nullable
    protected S session(InteractionHand hand) {
        beforeAccess(hand);
        return sessions.get(hand);
    }

    /// > 取会话；{@code allowEmpty} 为真且当前手无会话时，新建一个双手会话兜底。
    @Nullable
    protected S ensureSession(InteractionHand hand, boolean allowEmpty) {
        beforeAccess(hand);
        S session = sessions.get(hand);
        if (session == null && allowEmpty) {
            session = createSession(hand, player.getItemInHand(hand), TwoHandProfile.INSTANCE);
            sessions.put(hand, session);
        }
        return session;
    }

    /// > 访问会话前的同步钩子。服务端覆盖为即时 {@link #synchronizeSession}（实例身份比较廉价权威）；
    /// > 客户端默认空实现——客户端只由 {@code InventoryEventHandler} 检测器驱动同步，访问不自轮询。
    protected void beforeAccess(InteractionHand hand) {
    }

    /// > 推进现存会话。
    protected void tickSessions() {
        sessions.values().forEach(HandGunSession::tick);
    }

    protected InteractionHand preferredHand() {
        return DualWieldHelper.getPreferredHand(player);
    }

    /// > —— IGunOperator 通用路由 ——

    @Override
    public void tick() {
        tickSessions();
    }

    @Override
    public HandGunSession getSession(InteractionHand hand) {
        return sessions.get(hand);
    }

    @Nonnull
    @Override
    public EnumSet<GunHolderState> getActiveStates() {
        return getActiveStates(preferredHand());
    }

    @Nonnull
    @Override
    public EnumSet<GunHolderState> getActiveStates(InteractionHand hand) {
        S session = session(hand);
        return session != null ? session.getActiveStates() : EnumSet.noneOf(GunHolderState.class);
    }

    @Override
    public boolean isIdle() {
        S session = session(preferredHand());
        return session == null || session.isIdle();
    }

    @Override
    public List<String> debugTracks() {
        S session = session(preferredHand());
        return session != null ? session.debugTracks() : List.of();
    }

    @Nonnull
    @Override
    public ItemStack getCurrentItem() {
        return getCurrentItem(preferredHand());
    }

    @Override
    public ItemStack getCurrentItem(InteractionHand hand) {
        return player.getItemInHand(hand);
    }

    @Override
    public void draw(ItemStack newStack) {
        draw(preferredHand(), newStack);
    }

    @Override
    public void draw(InteractionHand hand, ItemStack newStack) {
        S session = ensureSession(hand, true);
        if (session != null) {
            session.draw(newStack);
        }
    }

    @Override
    public void putAway(ItemStack pendingStack) {
        putAway(preferredHand(), pendingStack);
    }

    @Override
    public void putAway(InteractionHand hand, ItemStack pendingStack) {
        S session = ensureSession(hand, true);
        if (session != null) {
            session.putAway(pendingStack);
        }
    }

    @Override
    public void reload() {
        reload(preferredHand());
    }

    @Override
    public void reload(InteractionHand hand) {
        S session = ensureSession(hand, false);
        if (session != null) {
            session.reload();
        }
    }

    @Override
    public void fire() {
        fire(preferredHand());
    }

    @Override
    public void fire(InteractionHand hand) {
        S session = ensureSession(hand, false);
        if (session != null) {
            session.fire();
        }
    }

    @Override
    public long getLastFireTime() {
        S session = session(preferredHand());
        return session != null ? session.getLastFireTime() : -1L;
    }

    @Override
    public void aim(boolean aiming) {
        aim(preferredHand(), aiming);
    }

    @Override
    public void aim(InteractionHand hand, boolean aiming) {
        S session = ensureSession(hand, false);
        if (session != null) {
            session.aim(aiming);
        }
    }

    @Override
    public float aimingProgress(double partialTicks) {
        return aimingProgress(preferredHand(), partialTicks);
    }

    @Override
    public float aimingProgress(InteractionHand hand, double partialTicks) {
        S session = session(hand);
        return session != null ? session.aimingProgress(partialTicks) : 0f;
    }

    @Override
    public boolean isTacticalStance(InteractionHand hand) {
        S session = session(hand);
        return session != null && session.isTacticalStance();
    }

    /// > 与 {@link #aimingProgress()} 同一手位口径：默认读取优先手，保证散布判定的一致性。
    @Override
    public boolean isTacticalStance() {
        return isTacticalStance(preferredHand());
    }

    @Override
    public void aimStance(InteractionHand hand, boolean tactical) {
        S session = ensureSession(hand, false);
        if (session != null) {
            session.aimStance(tactical);
        }
    }

    @Override
    public void inspect() {
        inspect(preferredHand());
    }

    @Override
    public void inspect(InteractionHand hand) {
        S session = ensureSession(hand, false);
        if (session != null) {
            session.inspect();
        }
    }

    @Nonnull
    @Override
    public GunModuleMode getModuleMode() {
        return getModuleMode(preferredHand());
    }

    @Override
    @Nonnull
    public GunModuleMode getModuleMode(InteractionHand hand) {
        S session = session(hand);
        return session != null ? session.getModuleMode() : GunModuleMode.PRIMARY;
    }

    @Override
    public void setModuleMode(@Nonnull GunModuleMode mode) {
        setModuleMode(preferredHand(), mode);
    }

    @Override
    public void setModuleMode(InteractionHand hand, @Nonnull GunModuleMode mode) {
        S session = ensureSession(hand, false);
        if (session != null) {
            session.setModuleMode(mode);
        }
    }
}
