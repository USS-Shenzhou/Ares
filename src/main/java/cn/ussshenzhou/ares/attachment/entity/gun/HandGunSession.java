package cn.ussshenzhou.ares.attachment.entity.gun;

import cn.ussshenzhou.ares.gun.gameplay.ShootResult;
import cn.ussshenzhou.ares.util.GunHolderState;
import cn.ussshenzhou.ares.util.GunModuleMode;
import cn.ussshenzhou.ares.util.GunOperationSessionKind;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/// @author TACZ, USS_Shenzhou
public abstract class HandGunSession {
    private final InteractionHand hand;
    private WieldProfile profile;
    private ItemStack stack;

    protected HandGunSession(InteractionHand hand, ItemStack stack, WieldProfile profile) {
        this.hand = hand;
        this.stack = stack;
        this.profile = profile;
    }

    public InteractionHand hand() {
        return hand;
    }

    public WieldProfile profile() {
        return profile;
    }

    /// > 重绑持枪形态。客户端会话在收枪窗口结束、按实时手持物落地为新形态时调用
    /// > （如双持移除副手后主手从单手形态切回双手形态）。服务端不使用。
    public void rebind(WieldProfile profile) {
        this.profile = profile;
    }

    public ItemStack stack() {
        return stack;
    }

    public void updateStack(ItemStack stack) {
        this.stack = stack;
    }

    public GunOperationSessionKind kind() {
        return profile.kind();
    }

    public boolean canInspectWeapon() {
        return profile.canInspect();
    }

    /// > 当前是否允许换弹 / 退弹 / 切换弹种（三者共用同一道门禁）。默认允许，
    /// > 由子类根据活跃时间线（如换弹中）收紧。
    public boolean canReload() {
        return true;
    }

    public boolean canSwitchWeaponMode(GunModuleMode mode) {
        return profile.canSwitchModule();
    }

    public EnumSet<GunHolderState> getActiveStates() {
        return EnumSet.noneOf(GunHolderState.class);
    }

    public boolean isIdle() {
        return getActiveStates().isEmpty();
    }

    public List<String> debugTracks() {
        return Collections.emptyList();
    }

    public ItemStack getCurrentItem() {
        return stack;
    }

    /// > 当前玩法操作的目标物品堆：默认即手持物；下挂模式下由子类返回下挂武器（副本 / 缓存解析）。
    /// > 供共享脚本上下文等只读消费方统一取用，无需关心主枪 / 下挂差异。
    public ItemStack activeStack() {
        return getCurrentItem();
    }

    /// > 持枪者是否处于瞄准状态。默认 false，由子类按各自瞄准态覆写。
    public boolean isAiming() {
        return false;
    }

    /// > 当前瞄准视图位是否为战术拒枪姿态（本体第二瞄位）。默认 false，由子类覆写。
    public boolean isTacticalStance() {
        return false;
    }

    /// > 写入战术拒枪姿态。默认空实现：客户端本地态不从服务端回流，服务端由 C2S 权威写入。
    public void aimStance(boolean tactical) {
    }

    /// > 本会话是否在客户端逻辑端（客户端预判 / 服务端权威的区分依据）。
    public abstract boolean isClient();

    /// > 持枪者玩家。供只读上下文查询玩家状态 / 背包等。理论上恒非空（会话总绑定在某个玩家上），
    /// > 但基类不持有玩家引用，默认返回 null，由子类提供。
    @Nullable
    public Player owner() {
        return null;
    }

    public void tick() {
    }

    public void draw(ItemStack newStack) {
        updateStack(newStack.copy());
    }

    public void putAway(ItemStack pendingStack) {
        draw(pendingStack);
    }

    public void fire() {
    }

    public ShootResult fireRelease(float yaw, float pitch) {
        return ShootResult.BLOCKED;
    }

    public void reload() {
    }

    public void aim(boolean aiming) {
    }

    public float aimingProgress(double partialTicks) {
        return 0f;
    }

    public void inspect() {
    }

    public long getLastFireTime() {
        return -1L;
    }

    public GunModuleMode getModuleMode() {
        return GunModuleMode.PRIMARY;
    }

    public void setModuleMode(GunModuleMode mode) {
    }
}
