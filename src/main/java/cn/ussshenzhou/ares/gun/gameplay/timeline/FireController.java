package cn.ussshenzhou.ares.gun.gameplay.timeline;

import cn.ussshenzhou.ares.gun.gameplay.ShootResult;
import cn.ussshenzhou.ares.gun.custom.gun.BoltType;
import cn.ussshenzhou.ares.gun.custom.gun.FireModeEntry;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import cn.ussshenzhou.ares.gun.gameplay.fire.TriggerStrategy;
import cn.ussshenzhou.ares.gun.gameplay.timeline.segment.BoltSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.segment.BurstSequenceSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.segment.CooldownSegment;
import cn.ussshenzhou.ares.gun.util.AmmoDataHelper;
import cn.ussshenzhou.ares.gun.util.FireModeHelper;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

import java.util.function.LongConsumer;
import java.util.function.Supplier;

/// > 常驻开火控制器：承载 trigger 策略与射击片段编排，本身不投影状态、不进入 SegmentSet。
/// >
/// > 出膛后的编排决策（起 burst / 起拉栓窗口 / 即时补膛）收口到 [#orchestrateAfterShot(boolean)]，
/// > 是后续脚本编排（`fire.plan` 的 `afterShot`）的接入点。
/// @author TACZ, USS_Shenzhou
public class FireController implements BoltCoordinator, FireAfterShotOps, FirePreShotOps {

    /// > burst 全部打完后的尾随闭锁窗口（毫秒，等价旧的 4 tick）。
    private static final long BURST_TAIL_COOLDOWN_MS = 200;

    /// > 端相关副作用回调。控制器只负责时序编排，不直接知道客户端/服务端细节。
    /// >
    /// > 双端各提供各自需要的回调，用 [Builder] 构造。哪端提供哪个见各字段注释；
    /// > 端差异只体现在这些回调上，时序骨架由控制器双端共享推导。
    public static class Hooks {
        /// > 主发出膛副作用（必填）。客户端：播放动画/音效/后坐力 + 预消耗弹药 + 发 C2S；服务端：生成弹丸。
        public final TriggerStrategy.FireCallback fireBullet;
        /// > 客户端专属：burst 后续发自驱回调（客户端按间隔本地驱动）。服务端为 `null`。
        @Nullable
        public final Runnable burstShot;
        /// > 主发出膛后的端副作用（可选）。客户端：muzzle-flash 时间戳 / 阻冲刺；服务端：扣 shootTimePool / 记时。
        @Nullable
        public final Runnable postPrimaryFire;
        /// > 服务端专属：burst 后续发的权威发射（由 C2S 后续发包逐发驱动）。客户端为 `null`。
        @Nullable
        public final TriggerStrategy.FireCallback burstFollowUpFire;
        /// > 补膛权威副作用（可选）。服务端：写 chamber；客户端：`null`（不写 NBT，服务端权威同步）。
        @Nullable
        public final Runnable reChamber;
        /// > 拉栓窗口启动时的副作用（客户端用：起自愈屏蔽锁，避免 bolt 窗口结束后被 tryAutoChamber 重复补偿触发）。
        /// > 参数为本次 bolt 窗口时长（毫秒）。服务端不需要，为 `null`。
        @Nullable
        public final LongConsumer onBoltStart;

        private Hooks(Builder b) {
            this.fireBullet = b.fireBullet;
            this.burstShot = b.burstShot;
            this.postPrimaryFire = b.postPrimaryFire;
            this.burstFollowUpFire = b.burstFollowUpFire;
            this.reChamber = b.reChamber;
            this.onBoltStart = b.onBoltStart;
        }

        public static Builder builder(TriggerStrategy.FireCallback fireBullet) {
            return new Builder(fireBullet);
        }

        public static class Builder {
            private final TriggerStrategy.FireCallback fireBullet;
            @Nullable
            private Runnable burstShot;
            @Nullable
            private Runnable postPrimaryFire;
            @Nullable
            private TriggerStrategy.FireCallback burstFollowUpFire;
            @Nullable
            private Runnable reChamber;
            @Nullable
            private LongConsumer onBoltStart;

            private Builder(TriggerStrategy.FireCallback fireBullet) {
                this.fireBullet = fireBullet;
            }

            /// > 客户端专属：burst 后续发自驱回调。
            public Builder burstShot(@Nullable Runnable burstShot) {
                this.burstShot = burstShot;
                return this;
            }

            /// > 主发出膛后的端副作用。
            public Builder postPrimaryFire(@Nullable Runnable postPrimaryFire) {
                this.postPrimaryFire = postPrimaryFire;
                return this;
            }

            /// > 服务端专属：burst 后续发的权威发射。
            public Builder burstFollowUpFire(@Nullable TriggerStrategy.FireCallback burstFollowUpFire) {
                this.burstFollowUpFire = burstFollowUpFire;
                return this;
            }

            /// > 补膛权威副作用（服务端写 chamber，客户端不提供）。
            public Builder reChamber(@Nullable Runnable reChamber) {
                this.reChamber = reChamber;
                return this;
            }

            /// > 拉栓窗口启动副作用（客户端起自愈屏蔽锁）。
            public Builder onBoltStart(@Nullable LongConsumer onBoltStart) {
                this.onBoltStart = onBoltStart;
                return this;
            }

            public Hooks build() {
                return new Hooks(this);
            }
        }
    }

    private final SegmentSet segments;
    /// > 实时手持物供给器（而非构造时快照）。换枪 / 物品栈替换后弹药与上膛判定需读当前实例的 NBT，
    /// > 持快照会导致 requestChamber / shouldBoltAfterSuccessfulShot 基于过期数据误判。
    private final Supplier<ItemStack> gunSupplier;
    private final FireModeEntry mode;
    private final BoltType boltType;
    private final int boltActionTicks;
    private final Hooks hooks;
    private final TriggerStrategy trigger;
    /// > burst 后续发是否由客户端自驱（true=客户端本地按间隔驱动；false=服务端由 C2S 后续发包驱动）。
    /// > 由端注入的 [Hooks#burstShot] 是否存在决定：只有客户端会提供该自驱回调。
    private final boolean clientBurstDriven;
    /// > 出膛后编排决策器：内置默认（[AfterShotDecider#builtin()]）或脚本驱动（`fire.plan` 的 afterShot）。
    /// > 由 [#orchestrateAfterShot(boolean)] 调用。
    private final AfterShotDecider afterShotDecider;
    /// > 出膛前门禁决策器：内置默认（[PreShotDecider#allowAll()] 恒放行）或脚本驱动（`fire.plan` 的 canFire）。
    /// > 由 [#canFire(ItemStack)] 在内置判定通过后调用。
    private final PreShotDecider preShotDecider;

    private FireController(SegmentSet segments, Supplier<ItemStack> gunSupplier, FireModeEntry mode,
                           BoltType boltType, int boltActionTicks, Hooks hooks,
                           AfterShotDecider afterShotDecider, PreShotDecider preShotDecider,
                           TriggerStrategyProvider triggerProvider) {
        this.segments = segments;
        this.gunSupplier = gunSupplier;
        this.mode = mode;
        this.boltType = boltType;
        this.boltActionTicks = Math.max(boltActionTicks, 0);
        this.hooks = hooks;
        this.trigger = (triggerProvider != null ? triggerProvider : TriggerStrategyProvider.builtin())
                .create(mode, this::firePrimary);
        this.clientBurstDriven = hooks.burstShot != null;
        this.afterShotDecider = afterShotDecider != null ? afterShotDecider : AfterShotDecider.builtin();
        this.preShotDecider = preShotDecider != null ? preShotDecider : PreShotDecider.allowAll();
    }

    public static FireController create(SegmentSet segments, Supplier<ItemStack> gunSupplier, GunData data, Hooks hooks) {
        return create(segments, gunSupplier, data, hooks, null, null, null);
    }

    /// > 构建开火控制器。`afterShotDecider` 为 null 时用内置默认编排（零脚本回退）；
    /// > 声明了 `fire_plan_script` 时由 session 传入脚本驱动的决策器。
    public static FireController create(SegmentSet segments, Supplier<ItemStack> gunSupplier, GunData data,
                                        Hooks hooks, @Nullable AfterShotDecider afterShotDecider) {
        return create(segments, gunSupplier, data, hooks, afterShotDecider, null, null);
    }

    /// > 构建开火控制器（含出膛前门禁）。`afterShotDecider` / `preShotDecider` 为 null 时分别用
    /// > 内置默认（出膛后编排 / 恒放行门禁），等价零脚本行为；声明了 `fire_plan_script` 时由 session
    /// > 传入脚本驱动的决策器。两者独立：只声明 canFire 的脚本不影响出膛后编排，反之亦然。
    public static FireController create(SegmentSet segments, Supplier<ItemStack> gunSupplier, GunData data,
                                        Hooks hooks, @Nullable AfterShotDecider afterShotDecider,
                                        @Nullable PreShotDecider preShotDecider) {
        return create(segments, gunSupplier, data, hooks, afterShotDecider, preShotDecider, null);
    }

    /// > 构建开火控制器（完整重载，含触发策略供给器）。`triggerProvider` 为 null 时用内置默认
    /// > （[TriggerStrategyProvider#builtin()] 按 fire mode 走三策略），等价零脚本行为；声明了
    /// > `fire.plan` 的 `trigger` 时由 session 传入脚本驱动供给器。三者互不依赖，各自独立探测装配。
    public static FireController create(SegmentSet segments, Supplier<ItemStack> gunSupplier, GunData data,
                                        Hooks hooks, @Nullable AfterShotDecider afterShotDecider,
                                        @Nullable PreShotDecider preShotDecider,
                                        @Nullable TriggerStrategyProvider triggerProvider) {
        FireModeEntry mode = FireModeHelper.getCurrentFireMode(gunSupplier.get(), data);
        if (mode == null) {
            mode = FireModeEntry.ofType("semi");
        }
        return new FireController(segments, gunSupplier, mode, BoltType.resolve(data), data.getBoltActionTime(),
                hooks, afterShotDecider, preShotDecider, triggerProvider);
    }

    public void tick() {
        trigger.tick();
    }

    public ShootResult onTriggerPress(float yaw, float pitch) {
        if (isBolting() || isBurstBlocked()) {
            return ShootResult.BLOCKED;
        }
        return trigger.onPress(yaw, pitch);
    }

    public ShootResult onTriggerRelease(float yaw, float pitch) {
        return trigger.onRelease(yaw, pitch);
    }

    /// > 当前蓄力原始输入量（tick 数），供客户端松手时随 C2S 上报。非蓄力策略返回 0。
    public int chargeInput() {
        return trigger.chargeInput();
    }

    /// > 注入客户端上报的蓄力原始量（服务端权威流用，宽松防呆）。非蓄力策略忽略。
    public void injectChargeInput(int raw) {
        trigger.injectChargeInput(raw);
    }

    public ShootResult fireBurstFollowUp(float yaw, float pitch) {
        BurstSequenceSegment burst = segments.get(BurstSequenceSegment.TOKEN);
        return burst != null ? burst.fireServerFollowUp(yaw, pitch) : ShootResult.BLOCKED;
    }

    public boolean canFire(ItemStack gun) {
        boolean ammoReady = boltType.usesChamber()
                ? AmmoDataHelper.isChambered(gun)
                : AmmoDataHelper.hasAmmo(gun);
        boolean builtinOk = !trigger.isActive()
                && !isBurstBlocked()
                && !isBolting()
                && ammoReady;
        if (!builtinOk) {
            return false;
        }
        /// > 内置判定通过后，追加脚本出膛前否决位（未声明时恒放行）。
        return preShotDecider.allowFire(this, gun);
    }

    public BoltType getBoltType() {
        return boltType;
    }

    public boolean isBursting() {
        BurstSequenceSegment burst = segments.get(BurstSequenceSegment.TOKEN);
        return burst != null && burst.isBursting();
    }

    private boolean isBurstBlocked() {
        BurstSequenceSegment burst = segments.get(BurstSequenceSegment.TOKEN);
        return burst != null && burst.blocksFire();
    }

    @Override
    public boolean isBolting() {
        return segments.has(BoltSegment.TOKEN);
    }

    /// > 起一个不带补膛副作用的拉栓窗口（客户端自愈拉栓入口用：只起表现窗口，补膛由服务端权威同步）。
    public void startBoltWindow() {
        startBoltWindow(null);
    }

    /// > [BoltCoordinator] 入口：起拉栓窗口，窗口结束时执行 `onWindowEnd`（如换弹补膛）。
    /// > 无窗口时退化为即时执行 `onWindowEnd`。
    @Override
    public void startBoltWindow(@Nullable Runnable onWindowEnd) {
        if (boltActionTicks > 0) {
            segments.add(new BoltSegment(boltActionTicks, onWindowEnd));
            notifyBoltStart();
        } else if (onWindowEnd != null) {
            onWindowEnd.run();
        }
    }

    @Override
    public long getBoltWindowMs() {
        return boltActionTicks * 50L;
    }

    public void requestChamber() {
        if (!boltType.usesChamber()) {
            return;
        }
        if (isBolting()) {
            return;
        }
        ItemStack gun = gun();
        if (AmmoDataHelper.isChambered(gun)) {
            return;
        }
        if (AmmoDataHelper.getAmmoCount(gun) <= 0) {
            return;
        }
        startBoltOrRechamber();
    }

    public void reset() {
        trigger.reset();
    }

    private ShootResult firePrimary(float yaw, float pitch, float damageMult, float speedMult, int muzzleIndex) {
        boolean shouldBoltAfterShot = shouldBoltAfterSuccessfulShot();
        ShootResult result = hooks.fireBullet.fire(yaw, pitch, damageMult, speedMult, muzzleIndex);
        if (result != ShootResult.SUCCESS) {
            return result;
        }
        CooldownSegment.driveShotFired(segments);
        if (hooks.postPrimaryFire != null) {
            hooks.postPrimaryFire.run();
        }
        orchestrateAfterShot(shouldBoltAfterShot);
        return ShootResult.SUCCESS;
    }

    /// > 出膛成功后的编排决策：委托 [AfterShotDecider]（内置默认或脚本驱动）。
    /// >
    /// > 这是开火编排的核心决策点，收口了原先散在 `firePrimary` 尾部与
    /// > `startBoltAfterSequence` / `startBoltOrRechamber` 的分支。脚本编排
    /// > （`fire.plan` 的 `afterShot`）经 [FireAfterShotOps] 在此接入。
    /// >
    /// > @param shouldBoltAfterShot 单发模式下本发是否应拉栓（据 bolt 类型与余弹推导）
    private void orchestrateAfterShot(boolean shouldBoltAfterShot) {
        afterShotDecider.decide(this, shouldBoltAfterShot);
    }

    /// > ---- FireAfterShotOps：出膛后编排的受控原语（供内置默认决策器与脚本决策器复用）----

    @Override
    public void startBurst() {
        addBurstSequence();
    }

    @Override
    public void boltAfterShot() {
        startBoltAfterSequence(true);
    }

    @Override
    public void rechamberNow() {
        if (hooks.reChamber != null) {
            hooks.reChamber.run();
        }
    }

    @Override
    public int burstCount() {
        return mode.getBurstCount();
    }

    @Override
    public boolean usesChamber() {
        return boltType.usesChamber();
    }

    @Override
    public boolean shouldBoltByDefault() {
        return shouldBoltAfterSuccessfulShot();
    }

    @Override
    public ItemStack gun() {
        return gunSupplier.get();
    }

    private void addBurstSequence() {
        int followUpCount = Math.max(mode.getBurstCount() - 1, 0);
        if (followUpCount <= 0) {
            startBoltAfterSequence(shouldBoltAfterSuccessfulShot());
            return;
        }
        TriggerStrategy.FireCallback followUp = clientBurstDriven
                ? (yaw, pitch, damageMult, speedMult, muzzleIndex) -> {
                    hooks.burstShot.run();
                    return ShootResult.SUCCESS;
                }
                : hooks.burstFollowUpFire;
        if (followUp == null) {
            startBoltAfterSequence(shouldBoltAfterSuccessfulShot());
            return;
        }
        segments.add(new BurstSequenceSegment(followUpCount, mode.getBurstInterval(), BURST_TAIL_COOLDOWN_MS,
                clientBurstDriven, followUp, this::startBoltAfterSequence));
    }

    private void startBoltAfterSequence() {
        startBoltAfterSequence(shouldBoltAfterSuccessfulShot());
    }

    private void startBoltAfterSequence(boolean shouldBolt) {
        if (!shouldBolt) {
            return;
        }
        if (boltType == BoltType.MANUAL) {
            startBoltOrRechamber();
            return;
        }
        if (boltActionTicks > 0) {
            segments.add(new BoltSegment(boltActionTicks, null));
            notifyBoltStart();
        }
    }

    /// > 启动拉栓窗口（boltActionTicks>0）并执行补膛，或在无窗口时即时补膛。
    /// > 起窗口时统一触发 [Hooks#onBoltStart] 设自愈屏蔽锁，避免窗口结束后被自愈重复补偿。
    private void startBoltOrRechamber() {
        if (boltActionTicks > 0) {
            segments.add(new BoltSegment(boltActionTicks, hooks.reChamber));
            notifyBoltStart();
        } else if (hooks.reChamber != null) {
            hooks.reChamber.run();
        }
    }

    /// > 通知端侧拉栓窗口已启动（客户端据此起自愈屏蔽锁）。
    private void notifyBoltStart() {
        if (hooks.onBoltStart != null) {
            hooks.onBoltStart.accept(getBoltWindowMs());
        }
    }

    private boolean shouldBoltAfterSuccessfulShot() {
        if (boltActionTicks <= 0) {
            return false;
        }
        ItemStack gun = gun();
        if (boltType.usesChamber()) {
            return AmmoDataHelper.getAmmoCount(gun) > 0;
        }
        return AmmoDataHelper.getAmmoCount(gun) > 1;
    }
}
