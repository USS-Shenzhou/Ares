package cn.ussshenzhou.ares.gun.gameplay.reload;

import cn.ussshenzhou.ares.attachment.entity.ServerHandGunSession;
import cn.ussshenzhou.ares.gun.custom.attribute.GunPropertiesCaches;
import cn.ussshenzhou.ares.gun.custom.gun.BoltType;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import cn.ussshenzhou.ares.gun.gameplay.ShootHelper;
import cn.ussshenzhou.ares.gun.gameplay.timeline.StepSequence;
import cn.ussshenzhou.ares.gun.util.AmmoDataHelper;
import cn.ussshenzhou.ares.gun.util.AmmoInventoryHelper;
import cn.ussshenzhou.ares.gun.util.CustomItemDataHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

import java.util.List;
import java.util.function.Supplier;

/// > 服务端换弹的副作用任务链（{@link StepSequence.Step} 实现）。
/// > <p>
/// > 编排层（{@link ServerHandGunSession}）据 {@link GunData}
/// > 构建初始 Step，交给 {@link cn.ussshenzhou.ares.gun.gameplay.timeline.segment.ReloadSegment} 用
/// > {@link StepSequence} 驱动。时间线只负责计时与流转；本类承载全部权威副作用：
/// > 扣背包、写 AmmoCount/AmmoId、空仓自带补膛、{@code ON_RELOAD_COMPLETE} 钩子与
/// > {@link GunReloadEvent.Complete} 事件。
/// > <p>
/// > 收编原 {@code MagazineReloadBehavior} / {@code ShellByShellReloadBehavior} 的副作用部分；
/// > 相位流转改由 Step 链表达，计时交给时间线。
/// > <p>
/// > 目标物品以<b>实时活引用</b>（{@link Supplier}）持有而非构造时捕获实例：服务端物品实例可能被
/// > 强同步边界重建（会话静默重绑、换弹续跑），每步读写必须落到当前实例上，避免写进已不在背包的旧实例。
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW hooks and Forge events are removed.
public class ServerReloadSteps {

    private final ServerPlayer player;
    private final Supplier<ItemStack> gun;
    private final GunData data;

    private ServerReloadSteps(ServerPlayer player, Supplier<ItemStack> gun, GunData data) {
        this.player = player;
        this.gun = gun;
        this.data = data;
    }

    /// > 当前实时目标物品堆（每次读写现取，反映实例重建 / 换弹副作用落地后的最新状态）。
    private ItemStack currentGun() {
        return gun.get();
    }

    /// > 构建服务端换弹初始 Step。返回 null 表示无法换弹（容量已满 / 无弹），编排层不应启动序列。
    @Nullable
    public static StepSequence.Step build(ServerPlayer player, Supplier<ItemStack> gun, GunData data) {
        ServerReloadSteps steps = new ServerReloadSteps(player, gun, data);
        ItemStack target = gun.get();
        int currentAmmo = AmmoDataHelper.getAmmoCount(target);
        if (currentAmmo >= data.getMagazineCapacity()) {
            return null;
        }
        if (!AmmoInventoryHelper.hasAmmoInInventory(player, steps.reloadAmmoIds())) {
            return null;
        }
        if ("shell_by_shell".equals(data.getReloadType())) {
            return steps.new ShellIntroStep(currentAmmo <= 0);
        }
        boolean startedEmpty = currentAmmo <= 0 && !AmmoDataHelper.isChambered(target);
        return steps.new MagazineStep(startedEmpty);
    }

    /// > 本次换弹应当查找的弹种列表：枪已设定弹种则只认该弹种（禁止混装），
    /// > 否则回退到完整兼容列表（首次装填自动适配背包现有弹种）。
    private List<Identifier> reloadAmmoIds() {
        return AmmoInventoryHelper.resolveReloadAmmoIds(AmmoDataHelper.getAmmoId(currentGun()), data.getAcceptedAmmoIds());
    }

    /// > ---- 弹匣换弹：单步 ----

    private class MagazineStep implements StepSequence.Step {
        private final boolean startedEmpty;

        private MagazineStep(boolean startedEmpty) {
            this.startedEmpty = startedEmpty;
        }

        @Override
        public long durationMs() {
            return data.getReloadDurationForLevel(GunPropertiesCaches.of(currentGun()).getMagLevel()) * 50L;
        }

        @Override
        @Nullable
        public StepSequence.Step advance() {
            ItemStack target = currentGun();
            int currentAmmo = AmmoDataHelper.getAmmoCount(target);
            int maxCapacity = data.getMagazineCapacity();
            int needed = maxCapacity - currentAmmo;
            if (needed <= 0) {
                return null;
            }
            int slot = AmmoInventoryHelper.findAmmoSlot(player, reloadAmmoIds());
            if (slot < 0) {
                return null;
            }
            ItemStack ammoStack = player.getInventory().getItem(slot);
            int actualLoad = Math.min(needed, ammoStack.getCount());
            if (actualLoad <= 0) {
                return null;
            }

            /// > 先读弹种再扣弹：shrink 扣完整组后 stack 置空，getCustomId 会返回 EMPTY_ID 导致 AmmoId 写不上
            Identifier ammoId = CustomItemDataHelper.getCustomId(ammoStack);
            ammoStack.shrink(actualLoad);
            AmmoDataHelper.setAmmoCount(target, currentAmmo + actualLoad);

            if (ammoId != null && !CustomItemDataHelper.EMPTY_ID.equals(ammoId)) {
                AmmoDataHelper.setAmmoId(target, ammoId);
            }

            if (startedEmpty && actualLoad > 0) {
                reChamberIfEmptyBolt();
            }
            return null;
        }
    }

    /// > ---- 逐发换弹：intro → loop* → end ----

    private class ShellIntroStep implements StepSequence.Step {
        private final boolean introEmpty;

        private ShellIntroStep(boolean introEmpty) {
            this.introEmpty = introEmpty;
        }

        @Override
        public long durationMs() {
            GunData.ShellReloadTiming t = data.getShellReload();
            int ticks = t != null ? (introEmpty ? t.getIntroEmpty() : t.getIntro()) : 10;
            return ticks * 50L;
        }

        @Override
        @Nullable
        public StepSequence.Step advance() {
            /// > 空仓 intro 包含装弹动作，非空仓不包含
            if (introEmpty) {
                loadOneShell();
            }
            return nextLoopOrEnd(introEmpty);
        }
    }

    private class ShellLoopStep implements StepSequence.Step {
        private final boolean introEmpty;

        private ShellLoopStep(boolean introEmpty) {
            this.introEmpty = introEmpty;
        }

        @Override
        public long durationMs() {
            GunData.ShellReloadTiming t = data.getShellReload();
            return (t != null ? t.getLoop() : 8) * 50L;
        }

        @Override
        @Nullable
        public StepSequence.Step advance() {
            loadOneShell();
            return nextLoopOrEnd(introEmpty);
        }
    }

    private class ShellEndStep implements StepSequence.Step {
        private final boolean introEmpty;
        private final int loadedCount;

        private ShellEndStep(boolean introEmpty, int loadedCount) {
            this.introEmpty = introEmpty;
            this.loadedCount = loadedCount;
        }

        @Override
        public long durationMs() {
            GunData.ShellReloadTiming t = data.getShellReload();
            return (t != null ? t.getEnd() : 6) * 50L;
        }

        @Override
        @Nullable
        public StepSequence.Step advance() {
            if (loadedCount > 0) {
                /// > 空仓逐发的 intro/end 动作通常已包含上膛，完成时直接写 chamber
                if (introEmpty) {
                    reChamberIfEmptyBolt();
                }
            }
            return null;
        }
    }

    /// > 逐发计数：跨多个 loop 步累加（实例字段，序列内单线程推进）。
    private int shellLoaded = 0;

    /// > 装一发：扣背包、写 AmmoCount/AmmoId。
    private void loadOneShell() {
        int slot = AmmoInventoryHelper.findAmmoSlot(player, reloadAmmoIds());
        if (slot == -1) {
            return;
        }
        ItemStack ammoStack = player.getInventory().getItem(slot);
        /// > 先读弹种再扣弹：shrink 扣完整组后 stack 置空，getCustomId 会返回 EMPTY_ID 导致 AmmoId 写不上
        Identifier ammoId = CustomItemDataHelper.getCustomId(ammoStack);
        ammoStack.shrink(1);
        ItemStack target = currentGun();
        int currentAmmo = AmmoDataHelper.getAmmoCount(target);
        AmmoDataHelper.setAmmoCount(target, currentAmmo + 1);
        shellLoaded++;

        if (ammoId != null && !CustomItemDataHelper.EMPTY_ID.equals(ammoId)) {
            AmmoDataHelper.setAmmoId(target, ammoId);
        }
    }

    /// > 装完一发后据数据决定：继续 loop 还是进 end。
    private StepSequence.Step nextLoopOrEnd(boolean introEmpty) {
        if (AmmoDataHelper.getAmmoCount(currentGun()) >= data.getMagazineCapacity()
                || !AmmoInventoryHelper.hasAmmoInInventory(player, reloadAmmoIds())) {
            return new ShellEndStep(introEmpty, shellLoaded);
        }
        return new ShellLoopStep(introEmpty);
    }

    /// > ---- 公共副作用 ----

    /// > 空仓换弹自带上膛：动画通常已包含拉栓动作，完成时服务端直接写 chamber，
    /// > 避免客户端随后因 chambered=false 再发起一次独立 BoltSegment 自愈拉栓。
    private void reChamberIfEmptyBolt() {
        ItemStack target = currentGun();
        if (BoltType.resolve(data).usesChamber() && !AmmoDataHelper.isChambered(target)) {
            ShootHelper.reChamber(target);
        }
    }
}
