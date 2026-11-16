package cn.ussshenzhou.ares.attachment.entity.session;

import cn.ussshenzhou.ares.attachment.entity.WieldProfile;
import cn.ussshenzhou.ares.gun.attribute.GunAttributes;
import cn.ussshenzhou.ares.gun.gameplay.MuzzleReference;
import cn.ussshenzhou.ares.gun.gameplay.ShootResult;
import cn.ussshenzhou.ares.gun.item.AttachmentPath;
import cn.ussshenzhou.ares.gun.item.IGun;
import cn.ussshenzhou.ares.gun.custom.CommonAssetsManager;
import cn.ussshenzhou.ares.gun.custom.gun.BoltType;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import cn.ussshenzhou.ares.gun.custom.gun.GunIndex;
import cn.ussshenzhou.ares.gun.gameplay.ShootHelper;
import cn.ussshenzhou.ares.gun.gameplay.reload.ReloadPolicy;
import cn.ussshenzhou.ares.gun.gameplay.reload.ServerReloadSteps;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionGate;
import cn.ussshenzhou.ares.gun.gameplay.timeline.FireController;
import cn.ussshenzhou.ares.gun.gameplay.timeline.SegmentSet;
import cn.ussshenzhou.ares.gun.gameplay.timeline.StepSequence;
import cn.ussshenzhou.ares.gun.gameplay.timeline.segment.ReloadSegment;
import cn.ussshenzhou.ares.gun.network.message.c2s.C2SFireModeSwitchMessage;
import cn.ussshenzhou.ares.gun.util.AmmoDataHelper;
import cn.ussshenzhou.ares.gun.util.AttributeDataHelper;
import cn.ussshenzhou.ares.gun.util.AttachmentTreeHelper;
import cn.ussshenzhou.ares.gun.util.FireModeHelper;
import cn.ussshenzhou.ares.gun.util.ModuleModeHelper;
import cn.ussshenzhou.ares.gun.util.MountedWeaponHelper;
import cn.ussshenzhou.ares.util.GunHolderState;
import cn.ussshenzhou.ares.util.GunModuleMode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.EnumSet;
import java.util.Optional;

/// @author TACZ, USS_Shenzhou
public class ServerHandGunSession extends HandGunSession {
    private final ServerPlayer player;

    /// > 下挂模式工作副本：进入 {@link GunModuleMode#UNDER_BARREL} 时从宿主配件树解析出的下挂武器副本。
    /// > <p>
    /// > 下挂武器嵌在宿主 NBT 里没有稳定的活引用，故玩法（开火/换弹/上膛）全部在这个<b>稳定副本实例</b>上
    /// > 读写，再于 tick 边界经 {@link #commitMountedWorkingCopy()} 自底向上回写宿主。
    /// > PRIMARY 模式下为 {@code null}，{@link #activeStack()} 退回宿主活引用。
    @Nullable
    private ItemStack mountedWorkingCopy;
    /// > 下挂武器在宿主配件树中的槽位路径，配合 {@link AttachmentTreeHelper#writeAttachmentBack} 回写。
    @Nullable
    private AttachmentPath mountedPath;
    public long lastShootTimestamp = -1;
    public long lastTickTimestamp = -1;
    private boolean isAiming = false;
    /// > 战术拒枪姿态（服务端权威）：由 C2S 上报写入，决定瞄准散布取 {@code inaccuracy_tactical}
    /// > 还是 {@code inaccuracy_aim}。复位点与 {@link #isAiming} 对齐（掏枪 / 收枪）。
    private boolean tacticalStance = false;
    private int shotCounter = 0;
    private final SegmentSet tracks = new SegmentSet();
    @Nullable
    private FireController fireController;
    private long shootTimePool = 0;
    /// > 当前生效的射击间隔（毫秒）缓存：{@code shootTimePool} 门控 / 扣减 / 上限均读它，避免每 tick 求值脚本。
    /// > 掏枪与每发出膛后经 {@link #refreshFireInterval()} 重算。
    private long currentFireIntervalMs = 120;
    /// > 会话创建 / 掏枪时绑定的<b>活引用</b>（非副本）。服务端物品栏权威，
    /// > 槽位 {@code ItemStack} 实例在物品未被换走期间保持同一对象，
    /// > operator 用 {@code boundInstance == player.getItemInHand(hand)} 做实例身份快速路径。
    /// > 实例被重建（如创造物品栏等强同步边界）但值签名不变时，经 {@link #rebindInstance} 静默重绑。
    private ItemStack boundInstance;
    /// > 会话创建 / 掏枪时主手选中的热键槽索引（副手无槽位概念，恒 -1）。
    /// > 实例重建时配合值签名做「同槽同值」判定，避免把真实换枪误判为同一把。
    private int boundSlot = -1;

    public ServerHandGunSession(ServerPlayer player, InteractionHand hand, ItemStack stack, WieldProfile profile) {
        super(hand, stack.copy(), profile);
        this.player = player;
        this.boundInstance = stack;
        this.boundSlot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : -1;
    }

    /// > 创建 / 掏枪时绑定的活引用，供 operator 做实例身份比较。
    public ItemStack boundInstance() {
        return boundInstance;
    }

    /// > 会话绑定时的主手选中槽索引（副手恒 -1），供「同槽同值」判定。
    public int boundSlot() {
        return boundSlot;
    }

    /// > 实例被重建但值签名不变（同槽同值）时静默重绑：仅更新活引用与会话持有的栈副本，
    /// > 不重置时间线 / 开火序列 / 瞄准等状态（换弹等流程续跑，落到新实例上）。
    public void rebindInstance(ItemStack live) {
        this.boundInstance = live;
        updateStack(live.copy());
    }

    /// > 当前玩法操作的目标物品堆：下挂模式返回下挂工作副本，否则返回宿主活引用。
    /// > <p>
    /// > 开火/换弹/上膛/弹药记账等一切玩法读写都应经此方法，从而在下挂模式下作用于下挂武器自身。
    @Override
    public ItemStack activeStack() {
        if (mountedWorkingCopy != null) {
            return mountedWorkingCopy;
        }
        return getCurrentItem();
    }

    /// > 当前玩法目标的 {@link GunData}（兼容下挂武器）。
    @Nullable
    private GunData activeData() {
        return MountedWeaponHelper.resolveGunData(activeStack());
    }

    private MuzzleReference muzzleReference(int muzzleIndex) {
        return mountedWorkingCopy != null
                ? MuzzleReference.underBarrel(Math.max(muzzleIndex, 0))
                : MuzzleReference.primary(Math.max(muzzleIndex, 0));
    }

    /// > 从宿主配件树解析下挂武器，建立工作副本与回写路径。失败则清空（退回 PRIMARY 语义）。
    private void beginMountedSession() {
        ItemStack host = getCurrentItem();
        Optional<ItemStack> mounted = MountedWeaponHelper.findInstalledMountedWeapon(host);
        Optional<AttachmentPath> path = MountedWeaponHelper.findInstalledMountedWeaponPath(host);
        if (mounted.isEmpty() || path.isEmpty()) {
            mountedWorkingCopy = null;
            mountedPath = null;
            return;
        }
        mountedWorkingCopy = mounted.get().copy();
        mountedPath = path.get();
    }

    /// > 把下挂工作副本自底向上回写宿主 NBT。仅在下挂模式且副本/路径有效时生效。
    /// > 在动作边界（开火后、换弹推进后、切弹种/退弹后、退出下挂模式前）调用，保证服务端权威落地。
    private void commitMountedWorkingCopy() {
        if (mountedWorkingCopy == null || mountedPath == null) {
            return;
        }
        AttachmentTreeHelper.writeAttachmentBack(getCurrentItem(), mountedPath, mountedWorkingCopy);
    }

    /// > 清空下挂工作副本（切枪/退出下挂模式后）。调用方应先视需要 commit。
    private void clearMountedSession() {
        mountedWorkingCopy = null;
        mountedPath = null;
    }

    @Override
    public void tick() {
        long currentTime = System.currentTimeMillis();
        if (lastTickTimestamp == -1) {
            lastTickTimestamp = currentTime;
        }
        long deltaMs = currentTime - lastTickTimestamp;
        lastTickTimestamp = currentTime;

        long maxPool = getFireIntervalMs() * 3;
        shootTimePool = Math.min(shootTimePool + Math.max(deltaMs, 0), maxPool);
        if (fireController != null) {
            fireController.tick();
        }
        /// > 记录 tick 前是否在换弹：换弹完成会在 tracks.tick 内移除片段，需用 tick 前的状态判断本 tick 是否要落地记账。
        boolean wasReloading = tracks.has(ReloadSegment.TOKEN);
        tracks.tick(deltaMs);

        if (player.isSprinting()) {
            ActionGate gate = tracks.gate();
            if (!gate.canSprint()) {
                ReloadSegment reload = currentReloadTrack();
                if (reload != null && reload.canSprintInterrupt()) {
                    cancelReload();
                } else {
                    player.setSprinting(false);
                }
            }
            if (isAiming) {
                player.setSprinting(false);
            }
        }

        /// > 下挂模式：换弹进行中（含本 tick 刚完成）时把 step 推进的弹药记账落地回宿主 NBT。
        /// > 其余玩法路径（开火/补膛/切弹种/退弹）各自在动作末尾已即时 commit，无需每 tick 写。
        if (mountedWorkingCopy != null && (wasReloading || tracks.has(ReloadSegment.TOKEN))) {
            commitMountedWorkingCopy();
        }
    }

    @Nonnull
    @Override
    public EnumSet<GunHolderState> getActiveStates() {
        return tracks.projectStates();
    }

    @Override
    public boolean isIdle() {
        return tracks.isIdle();
    }

    @Override
    public ItemStack getCurrentItem() {
        return player.getItemInHand(hand());
    }

    @Override
    public void draw(ItemStack newStack) {
        updateStack(newStack.copy());
        this.boundInstance = newStack;
        this.boundSlot = hand() == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : -1;
        tracks.clear();
        fireController = null;
        isAiming = false;
        tacticalStance = false;
        clearMountedSession();
        /// > 按宿主 NBT 恢复模块模式（不强制复位，模式跟随枪自身持久化）。
        if (ModuleModeHelper.getModuleMode(newStack) == GunModuleMode.UNDER_BARREL) {
            beginMountedSession();
            if (mountedWorkingCopy == null) {
                /// > 记录为下挂模式但下挂已不在：权威纠正回主枪（合法的状态修正，非无条件复位）。
                ModuleModeHelper.setModuleMode(newStack, GunModuleMode.PRIMARY);
            }
        }
        initFireBehavior();
        shootTimePool = getFireIntervalMs();
    }

    @Override
    public void putAway(ItemStack pendingStack) {
        cancelReload();
        /// > 收枪前把下挂工作副本落地到宿主，避免丢失下挂弹药记账。
        commitMountedWorkingCopy();
        clearMountedSession();
        tacticalStance = false;
    }

    @Override
    public void fire() {
        fireWithDirection(player.getYRot(), player.getXRot());
    }

    public ShootResult fireWithDirection(float yaw, float pitch) {
        ActionGate gate = tracks.gate();
        if (!gate.canShoot()) {
            ReloadSegment reload = currentReloadTrack();
            if (reload != null && reload.canFireInterrupt() && AmmoDataHelper.hasAmmo(activeStack())) {
                cancelReload();
            } else {
                return ShootResult.BLOCKED;
            }
        }
        if (fireController == null) {
            return ShootResult.BLOCKED;
        }
        if (fireController.isBursting()) {
            ShootResult result = fireController.fireBurstFollowUp(yaw, pitch);
            commitMountedWorkingCopy();
            return result;
        }
        if (shootTimePool < getFireIntervalMs()) {
            return ShootResult.COOLDOWN;
        }
        /// > 出膛前门禁：服务端权威拒绝，不生成弹丸。
        if (!fireController.canFire(activeStack())) {
            return ShootResult.BLOCKED;
        }
        ShootResult result = fireController.onTriggerPress(yaw, pitch);
        commitMountedWorkingCopy();
        return result;
    }

    public ShootResult fireRelease(float yaw, float pitch, int chargeTicks) {
        if (fireController == null) {
            return ShootResult.BLOCKED;
        }
        /// > 先注入客户端上报的蓄力原始量，再松手。服务端自行重算倍率，客户端不上报倍率。
        fireController.injectChargeInput(chargeTicks);
        ShootResult result = fireController.onTriggerRelease(yaw, pitch);
        commitMountedWorkingCopy();
        return result;
    }

    @Override
    public long getLastFireTime() {
        return lastShootTimestamp;
    }

    @Override
    public void reload() {
        if (!tracks.gate().canReload()) {
            return;
        }
        ItemStack target = activeStack();
        GunData data = MountedWeaponHelper.resolveGunData(target);
        if (data == null) {
            return;
        }
        /// > 步骤链以实时活引用为读写目标：物品实例可能被强同步边界重建（会话静默重绑、换弹续跑），
        /// > 每步必须落到当前实例上，避免写进已不在背包的旧实例。
        StepSequence.Step initialStep = ServerReloadSteps.build(player, this::activeStack, data);
        if (initialStep == null) {
            return;
        }
        ReloadSegment segment = ReloadSegment.server(initialStep, ReloadPolicy.of(data));

        tracks.add(segment);
        player.setSprinting(false);
        /// > 下挂换弹启动即落地一次（写入 ReloadSegment 已捕获稳定副本，后续 step 推进在 tick 末提交）。
        commitMountedWorkingCopy();
    }

    public void requestChamber() {
        if (!tracks.isIdle()) {
            return;
        }
        if (fireController != null) {
            fireController.requestChamber();
            commitMountedWorkingCopy();
        }
    }

    public void cancelReload() {
        if (tracks.has(ReloadSegment.TOKEN)) {
            tracks.cancel(ReloadSegment.TOKEN);
        }
    }

    public void switchCombinedMode(int modeIndex) {
        GunModuleMode requestedMode = modeIndex == C2SFireModeSwitchMessage.UNDER_BARREL_MODE ? GunModuleMode.UNDER_BARREL : GunModuleMode.PRIMARY;
        if (!canSwitchWeaponMode(requestedMode)) {
            return;
        }
        if (modeIndex == C2SFireModeSwitchMessage.UNDER_BARREL_MODE) {
            if (MountedWeaponHelper.findInstalledMountedWeapon(getCurrentItem()).isEmpty()) {
                return;
            }
            enterUnderBarrelMode();
            return;
        }
        if (!(getCurrentItem().getItem() instanceof IGun iGun)) {
            return;
        }
        GunData data = iGun.getIndex(getCurrentItem()).map(GunIndex::getData).orElse(null);
        if (data == null) {
            return;
        }
        int modeCount = data.getFireModes().size();
        if (modeCount == 0 || modeIndex < 0 || modeIndex >= modeCount) {
            return;
        }
        exitUnderBarrelMode();
        FireModeHelper.setFireModeIndex(getCurrentItem(), modeIndex);
        initFireBehavior();
    }

    /// > 进入下挂模式：建立工作副本/路径并按下挂 data 重建开火序列。建立失败则退回 PRIMARY。
    /// > 模式权威写入宿主 NBT，随物品栏同步下发客户端。
    private void enterUnderBarrelMode() {
        if (mountedWorkingCopy != null) {
            return;
        }
        beginMountedSession();
        if (mountedWorkingCopy == null) {
            ModuleModeHelper.setModuleMode(getCurrentItem(), GunModuleMode.PRIMARY);
            initFireBehavior();
            return;
        }
        ModuleModeHelper.setModuleMode(getCurrentItem(), GunModuleMode.UNDER_BARREL);
        initFireBehavior();
    }

    /// > 退出下挂模式：把工作副本落地回宿主，清空并按宿主 data 重建开火序列。
    /// > 模式权威写入宿主 NBT。
    private void exitUnderBarrelMode() {
        if (mountedWorkingCopy == null) {
            ModuleModeHelper.setModuleMode(getCurrentItem(), GunModuleMode.PRIMARY);
            return;
        }
        commitMountedWorkingCopy();
        clearMountedSession();
        ModuleModeHelper.setModuleMode(getCurrentItem(), GunModuleMode.PRIMARY);
    }

    @Override
    public void aim(boolean aiming) {
        if (aiming && !tracks.gate().canAim()) {
            return;
        }
        if (aiming && tracks.has(ReloadSegment.TOKEN) && !canAimWhileReloading()) {
            return;
        }
        isAiming = aiming;
    }

    @Override
    public boolean isAiming() {
        return isAiming;
    }

    @Override
    public boolean isTacticalStance() {
        /// > 只有真正瞄准时才把散布切到拒枪数值：本会话的 aimingProgress 恒为 1（见下方覆写），
        /// > 散布判定的「瞄准」门禁只能由 isAiming 提供，避免腰射时误用拒枪数值。
        return tacticalStance && isAiming;
    }

    /// > 写入战术拒枪姿态。仅接受数据侧确实声明了拒枪散布属性的枪：未声明时该属性会取到索引默认值 0，
    /// > 等同于完美精度，属于必须由服务端拦住的情况。
    @Override
    public void aimStance(boolean tactical) {
        if (!tactical) {
            this.tacticalStance = false;
            return;
        }
        GunData gunData = MountedWeaponHelper.resolveGunData(activeStack());
        boolean registered = CommonAssetsManager.get().getAttribute(GunAttributes.TACTICAL_INACCURACY) != null;
        this.tacticalStance = gunData != null && registered && gunData.hasAttribute(GunAttributes.TACTICAL_INACCURACY);
    }

    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public Player owner() {
        return player;
    }

    @Override
    public float aimingProgress(double partialTicks) {
        return 1f;
    }

    @Override
    public void inspect() {
        if (!canInspectWeapon()) {
            return;
        }
    }

    @Override
    public GunModuleMode getModuleMode() {
        return ModuleModeHelper.getModuleMode(getCurrentItem());
    }

    @Override
    public void setModuleMode(@Nonnull GunModuleMode mode) {
        if (!canSwitchWeaponMode(mode)) {
            return;
        }
        if (mode == GunModuleMode.UNDER_BARREL) {
            if (MountedWeaponHelper.findInstalledMountedWeapon(getCurrentItem()).isEmpty()) {
                return;
            }
            enterUnderBarrelMode();
        } else {
            exitUnderBarrelMode();
            initFireBehavior();
        }
    }

    public int getShotCounter() {
        return shotCounter;
    }

    public int incrementShotCounter() {
        return shotCounter++;
    }

    private void initFireBehavior() {
        GunData data = activeData();
        if (data != null) {
            installFireTracks(data);
            return;
        }
        fireController = null;
    }

    private void installFireTracks(GunData data) {
        BoltType boltType = BoltType.resolve(data);
        FireController.Hooks hooks = FireController.Hooks.builder(
                        (yaw, pitch, damageMult, speedMult, muzzleIndex) -> ShootHelper.fireOneBullet(
                                player, activeStack(), yaw, pitch, damageMult, speedMult, boltType, hand(), muzzleReference(muzzleIndex)))
                .postPrimaryFire(this::onPostFire)
                .burstFollowUpFire((yaw, pitch, damageMult, speedMult, muzzleIndex) -> ShootHelper.fireOneBullet(
                        player, activeStack(), yaw, pitch, damageMult, speedMult, boltType, hand(), muzzleReference(muzzleIndex)))
                .reChamber(() -> ShootHelper.reChamber(activeStack()))
                .build();
        refreshFireInterval();
        fireController = FireController.create(tracks, this::activeStack, data, hooks);
    }

    private void onPostFire() {
        shootTimePool = Math.max(0, shootTimePool - getFireIntervalMs());
        lastShootTimestamp = System.currentTimeMillis();
        /// > 出膛后重算下一发间隔：动态射速（转速渐进 / 过热降速）逐发生效，对齐客户端节奏。
        refreshFireInterval();
    }

    private long getFireIntervalMs() {
        return currentFireIntervalMs;
    }

    /// > 重算当前射击间隔缓存：由当前枪械 RPM 属性推导（{@code 60000 / RPM}），反映当前枪械最新只读状态。
    /// > 掏枪与每发出膛后调用；每 tick 的 pool 门控读缓存值。
    // FIXME REVIEW the fire interval only comes from RPM (AttributeDataHelper.shootIntervalMs), also in
    // ClientHandGunSession.computeCooldownMs.
    private void refreshFireInterval() {
        this.currentFireIntervalMs = Math.max(AttributeDataHelper.shootIntervalMs(activeStack()), 1);
    }

    @Nullable
    private ReloadSegment currentReloadTrack() {
        return tracks.get(ReloadSegment.TOKEN);
    }

    private boolean canAimWhileReloading() {
        if (!(getCurrentItem().getItem() instanceof IGun iGun)) {
            return true;
        }
        return iGun.getIndex(getCurrentItem()).map(GunIndex::getData).map(GunData::isAllowAimWhileReloading).orElse(true);
    }
}
