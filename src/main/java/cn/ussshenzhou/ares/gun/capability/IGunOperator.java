package cn.ussshenzhou.ares.gun.capability;

import cn.ussshenzhou.ares.gun.init.ModCapabilities;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/// > TODO: 枪械持有者能力接口<br/>
/// > 默认有三种实现，服务端玩家实现，负责实际的逻辑处理<br/>
/// > 客户端LocalPlayer实现，负责第一人称动画和逻辑处理，以及通知服务端<br/>
/// > 客户端其他实体实现，负责第三人称动画，被动接受服务端的状态更新<br/>
/// > 默认仅附加给玩家，其他模组的实体（甚至不一定是实体）若有特殊需求，也可自行实现这个接口并注册能力<br/>
/// > 应该还得提供一种默认的服务端其他实体实现，纯服务端控制
/// @author TACZ, USS_Shenzhou
public interface IGunOperator {

    // FIXME REVIEW returns Optional instead of the Forge LazyOptional; entities other than players get Optional.empty().
    static Optional<IGunOperator> asGunHolder(Entity entity) {
        if (entity instanceof Player player) {
            return Optional.of(player.getData(ModCapabilities.GUN_HOLDER_CAPABILITY));
        }
        return Optional.empty();
    }

    /// > 获取当前活跃的持枪者状态集合（多状态可并存，如 bolt 窗口期同时有 BOLT_ACTION 与 FIRE_COOLDOWN）。
    /// > 空集表示空闲（等价于旧的 NORMAL）。消费方应以「成员查询」方式读取，不再依赖单一优先级状态。
    /// > @return 活跃状态集合（不可变或只读语义，调用方不应修改）
    @Nonnull
    default EnumSet<GunHolderState> getActiveStates() {
        return getActiveStates(InteractionHand.MAIN_HAND);
    }

    @Nonnull
    EnumSet<GunHolderState> getActiveStates(InteractionHand hand);

    default HandGunSession getSession(InteractionHand hand) {
        return null;
    }

    /// > 是否空闲：没有任何活跃投影状态（等价于旧的 currentState == NORMAL）。
    default boolean isIdle() {
        return getActiveStates().isEmpty();
    }

    /// > 调试用：当前活跃轨道的展示行（每行形如 "id: debugInfo"）。默认空列表。
    /// > 仅供 debug overlay 渲染，无玩法语义。
    default List<String> debugTracks() {
        return Collections.emptyList();
    }

    /// > 每个游戏刻更新持枪者状态
    void tick();

    /// > 持枪者开始拔枪的操作
    /// > @param newStack 新的枪械物品堆
    default void draw(ItemStack newStack) {
        draw(InteractionHand.MAIN_HAND, newStack);
    }

    void draw(InteractionHand hand, ItemStack newStack);

    /// > 持枪者开始收枪的操作，收枪完成后自动触发 draw
    /// > @param pendingStack 收枪完成后要切入的物品堆
    default void putAway(ItemStack pendingStack) {
        putAway(InteractionHand.MAIN_HAND, pendingStack);
    }

    void putAway(InteractionHand hand, ItemStack pendingStack);

    /// > 持枪者开始装填弹药的操作
    default void reload() {
        reload(InteractionHand.MAIN_HAND);
    }

    void reload(InteractionHand hand);

    /// > 持枪者扣下枪械扳机时的操作
    default void fire() {
        fire(InteractionHand.MAIN_HAND);
    }

    void fire(InteractionHand hand);

    /// > 持枪者松开扳机（蓄力 / 延迟触发的松手发射）。默认空实现，客户端 {@code LocalGunOperator} 覆写为
    /// > 起松手发射并随 C2S 上报蓄力原始量；服务端由 C2S 权威处理，不经此入口。
    default void fireRelease() {
        fireRelease(InteractionHand.MAIN_HAND);
    }

    default void fireRelease(InteractionHand hand) {
    }

    /// > 获取上次开火的时间戳
    /// > @return 上次开火时间戳，单位毫秒
    long getLastFireTime();

    default void aim(boolean aiming)  {
        aim(InteractionHand.MAIN_HAND, aiming);
    }

    void aim(InteractionHand hand, boolean aiming);

    float aimingProgress(double partialTicks);

    default float aimingProgress(InteractionHand hand, double partialTicks) {
        return aimingProgress(partialTicks);
    }

    default float aimingProgress() {
        return aimingProgress(1.0);
    }

    /// > 是否为战术拒枪开镜姿态（本体第二瞄位）。客户端由本地视图位计算并 C2S 上报，
    /// > 服务端为已落地的权威态；默认 false。
    default boolean isTacticalStance() {
        return isTacticalStance(InteractionHand.MAIN_HAND);
    }

    default boolean isTacticalStance(InteractionHand hand) {
        return false;
    }

    /// > 写入战术拒枪姿态（客户端上报 / 服务端权威落地）。默认空实现。
    default void aimStance(InteractionHand hand, boolean tactical) {
    }

    default String getReloadPhase() {
        return "none";
    }
    /// > 获取当前持有的枪械物品堆
    /// > @return 枪械物品堆
    default ItemStack getCurrentItem() {
        return getCurrentItem(InteractionHand.MAIN_HAND);
    }

    ItemStack getCurrentItem(InteractionHand hand);

    void inspect();

    default void inspect(InteractionHand hand) {
        inspect();
    }

    /// > 当前模块模式（主枪/下挂）。
    /// > 仅当当前持有物品存在下挂武器时才有意义。
    @Nonnull
    default GunModuleMode getModuleMode() {
        return getModuleMode(InteractionHand.MAIN_HAND);
    }

    @Nonnull
    GunModuleMode getModuleMode(InteractionHand hand);

    default void setModuleMode(@Nonnull GunModuleMode mode) {
        setModuleMode(InteractionHand.MAIN_HAND, mode);
    }

    void setModuleMode(InteractionHand hand, @Nonnull GunModuleMode mode);
}
