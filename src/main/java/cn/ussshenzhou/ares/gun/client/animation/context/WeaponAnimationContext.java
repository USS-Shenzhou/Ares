package cn.ussshenzhou.ares.gun.client.animation.context;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.capability.GunHolderState;
import cn.ussshenzhou.ares.gun.capability.GunModuleMode;
import cn.ussshenzhou.ares.gun.capability.IGunOperator;
import cn.ussshenzhou.ares.gun.item.AttachmentSlot;
import cn.ussshenzhou.ares.gun.item.IGun;

import cn.ussshenzhou.ares.gun.capability.LocalGunOperator;
import cn.ussshenzhou.ares.gun.client.animation.mounted.HostOverlayBridge;
import cn.ussshenzhou.ares.gun.client.resource.display.GunDisplayInstance;
import cn.ussshenzhou.ares.gun.custom.gun.BoltType;
import cn.ussshenzhou.ares.gun.custom.gun.GunData;
import cn.ussshenzhou.ares.gun.custom.gun.GunIndex;
import cn.ussshenzhou.ares.gun.util.AmmoDataHelper;
import cn.ussshenzhou.ares.gun.util.AmmoInventoryHelper;
import cn.ussshenzhou.ares.gun.util.FireModeHelper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/// @author TACZ, USS_Shenzhou
public class WeaponAnimationContext {
    public enum MovementState {
        IDLE("idle"),
        WALK_FORWARD("walk_forward"),
        WALK_BACKWARD("walk_backward"),
        WALK_SIDEWAY("walk_sideway"),
        WALK_AIMING("walk_aiming"),
        RUN("run"),
        RUN_HOLD("run_hold");

        private final String id;

        MovementState(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    private final LocalPlayer player;
    private final GunDisplayInstance display;
    /// > 本上下文所属的手（主/副）。双持时据此向 operator 取对应手的 gameplay 状态，
    /// > 保证左右手状态机各读各的（reload/inspect/bolt/aim/模式 镜像不串）。
    private final InteractionHand hand;
    private ItemStack itemStack;
    private float partialTicks;
    private float aimProgress;
    /// > 瞄准视图位：战术拒枪姿态开关（目标姿态，切换请求即翻转）与切换过渡进度。
    /// > 供状态机做姿态混合 / 常驻姿态，代码侧只提供开镜视点，不写死枪体姿态。
    private boolean tacticalStance;
    private float aimViewSwitchProgress = 1.0f;
    private float moveSpeed;
    private boolean sprinting;
    private boolean onGround;
    private boolean inputUp;
    private boolean inputDown;
    private boolean inputLeft;
    private boolean inputRight;
    private MovementState movementState = MovementState.IDLE;

    /// > ==================== base 主状态机驱动信号 ====================
    /// > 持久请求信号：由 controller 设置，状态机过渡条件读取，afterTrigger 消费后清除（latest-wins，互斥）。
    private String pendingRequest = null;
    /// > fire 脉冲：瞬时信号，用于打断 reload/inspect；每个 runtime tick 末尾清除。
    private boolean firePulse = false;
    /// > fire 事件序号：状态机可消费真实出膛事件来驱动独立 fire 层。
    private int fireSerial = 0;
    private int consumedFireSerial = 0;
    /// > gameplay 权威状态镜像（集合，可并存），每 tick 从 operator 同步，供状态机过渡条件查询。
    private EnumSet<GunHolderState> gunHolderStates = EnumSet.noneOf(GunHolderState.class);
    /// > 模块模式镜像（PRIMARY / UNDER_BARREL），每 tick 从 operator 同步，供 host_overlay 门控。
    private GunModuleMode moduleMode = GunModuleMode.PRIMARY;
    /// > 模式切换过渡镜像，每 tick 从 operator 同步，供 host_overlay 持枪姿态权重淡入淡出。
    private boolean modeSwitching = false;
    private float modeTransitionProgress = 1.0f;
    /// > 当前 reload 期望动画时长（秒），由 controller.reload 写入，play_montage 变速对齐时读取。
    private float reloadExpectedSeconds = 0.0f;
    /// > 逐发换弹会话镜像：target 由客户端预测，done 由当前弹数 - 起始弹数推导。
    private int shellReloadTargetLoops = 0;
    private int shellReloadStartAmmoCount = 0;
    private int shellReloadPlayedLoops = 0;
    private HostOverlayBridge hostOverlayBridge = HostOverlayBridge.NONE;

    public WeaponAnimationContext(LocalPlayer player, ItemStack itemStack, GunDisplayInstance display) {
        this(player, itemStack, display, InteractionHand.MAIN_HAND);
    }

    public WeaponAnimationContext(LocalPlayer player, ItemStack itemStack, GunDisplayInstance display,
                                  InteractionHand hand) {
        this.player = player;
        this.itemStack = itemStack;
        this.display = display;
        this.hand = hand;
    }

    public void update(float partialTicks) {
        this.partialTicks = partialTicks;
        if (player == null) {
            resetMovement();
            gunHolderStates = EnumSet.noneOf(GunHolderState.class);
            moduleMode = GunModuleMode.PRIMARY;
            modeSwitching = false;
            modeTransitionProgress = 1.0f;
            return;
        }
        /// > 默认值先复位，operator 存在时再覆盖。
        aimProgress = 0.0f;
        tacticalStance = false;
        aimViewSwitchProgress = 1.0f;
        gunHolderStates = EnumSet.noneOf(GunHolderState.class);
        moduleMode = GunModuleMode.PRIMARY;
        modeSwitching = false;
        modeTransitionProgress = 1.0f;
        IGunOperator.asGunHolder(player).ifPresent(operator -> {
            aimProgress = operator.aimingProgress(hand, partialTicks);
            tacticalStance = operator.isTacticalStance(hand);
            gunHolderStates = operator.getActiveStates(hand);
            moduleMode = operator.getModuleMode(hand);
            if (operator instanceof LocalGunOperator localOp) {
                modeSwitching = localOp.isModeSwitching(hand);
                modeTransitionProgress = localOp.modeTransitionProgress(hand);
                LocalGunOperator.AimViewState viewState = localOp.aimViewState(hand);
                if (viewState != null) {
                    aimViewSwitchProgress = viewState.progress();
                }
            }
        });
        Vec3 movement = player.getDeltaMovement();
        moveSpeed = (float) Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        sprinting = player.isSprinting();
        onGround = player.onGround();
        inputUp = player.input != null && player.input.keyPresses.forward();
        inputDown = player.input != null && player.input.keyPresses.backward();
        inputLeft = player.input != null && player.input.keyPresses.left();
        inputRight = player.input != null && player.input.keyPresses.right();
        movementState = resolveMovementState();
    }

    private void resetMovement() {
        aimProgress = 0.0f;
        tacticalStance = false;
        aimViewSwitchProgress = 1.0f;
        moveSpeed = 0.0f;
        sprinting = false;
        onGround = false;
        inputUp = false;
        inputDown = false;
        inputLeft = false;
        inputRight = false;
        movementState = MovementState.IDLE;
    }

    private MovementState resolveMovementState() {
        if (sprinting) {
            return onGround ? MovementState.RUN : MovementState.RUN_HOLD;
        }
        if (player.input == null || player.input.getMoveVector().length() <= 0.01f || player.isMovingSlowly()) {
            return MovementState.IDLE;
        }
        if (!onGround) {
            return MovementState.IDLE;
        }
        if (aimProgress > 0.5f) {
            return MovementState.WALK_AIMING;
        }
        if (inputUp) {
            return MovementState.WALK_FORWARD;
        }
        if (inputDown) {
            return MovementState.WALK_BACKWARD;
        }
        if (inputLeft || inputRight) {
            return MovementState.WALK_SIDEWAY;
        }
        return MovementState.IDLE;
    }

    public float locomotionProgress(float cycleDistance, float phaseScale) {
        if (player == null || cycleDistance <= 0.0f) {
            return 0.0f;
        }
        // FIXME REVIEW 26.x moved walkDist/walkDistO from Entity to ClientAvatarState; the accessor name below is not verified.
        /*
        float walkDist = player.walkDist + (player.walkDist - player.walkDistO) * partialTicks;
        */
        float walkDist = player.avatarState().getInterpolatedWalkDistance(partialTicks);
        float phase = walkDist / cycleDistance * phaseScale;
        return phase - (float) Math.floor(phase);
    }

    public LocalPlayer player() {
        return player;
    }

    public ItemStack itemStack() {
        return itemStack;
    }

    public void updateItem(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public GunDisplayInstance display() {
        return display;
    }

    public float partialTicks() {
        return partialTicks;
    }

    public float aimProgress() {
        return aimProgress;
    }

    /// > 目标瞄准视图位是否为战术拒枪姿态。
    public boolean tacticalStance() {
        return tacticalStance;
    }

    /// > 瞄准视图位切换过渡进度（0~1，未在切换时恒为 1）。
    public float aimViewSwitchProgress() {
        return aimViewSwitchProgress;
    }

    public float moveSpeed() {
        return moveSpeed;
    }

    public boolean sprinting() {
        return sprinting;
    }

    public boolean onGround() {
        return onGround;
    }

    public boolean inputUp() {
        return inputUp;
    }

    public boolean inputDown() {
        return inputDown;
    }

    public boolean inputLeft() {
        return inputLeft;
    }

    public boolean inputRight() {
        return inputRight;
    }

    public MovementState movementState() {
        return movementState;
    }

    public String movementStateId() {
        return movementState.id();
    }

    public boolean isMoving() {
        return movementState != MovementState.IDLE;
    }

    /// > ==================== base 主状态机信号读写 ====================

    /// > 设置一个持久请求信号（latest-wins，互斥）。由 controller 在分发 reload/inspect/holster 时调用。
    public void requestAction(String request) {
        this.pendingRequest = request;
    }

    /// > 当前是否存在指定请求信号。供状态机过渡条件查询。
    public boolean hasRequest(String request) {
        return request != null && request.equals(pendingRequest);
    }

    /// > 当前待处理的请求信号 id，无则返回 null。
    public String pendingRequest() {
        return pendingRequest;
    }

    /// > 消费（清除）当前请求信号。供状态机过渡 afterTrigger 调用，避免重复触发。
    public void consumeRequest() {
        this.pendingRequest = null;
    }

    /// > 触发一次真实出膛事件。pulse 用于瞬时打断，serial 用于独立 fire 状态机消费。
    public void pulseFire() {
        this.firePulse = true;
        this.fireSerial++;
    }

    /// > 当前帧是否有 fire 脉冲。供状态机过渡条件查询。
    public boolean firePulse() {
        return firePulse;
    }

    /// > 当前是否存在尚未被动画状态机消费的出膛事件。
    public boolean hasUnconsumedFire() {
        return fireSerial > consumedFireSerial;
    }

    /// > 消费所有待处理出膛事件，避免同一发重复触发 fire 状态机。
    public void consumeFire() {
        consumedFireSerial = fireSerial;
    }

    /// > 累计出膛事件序号，供内容包调试或高级条件使用。
    public int fireSerial() {
        return fireSerial;
    }

    /// > 清除 fire 脉冲。由 runtime 在每 tick 末尾调用。
    public void clearFirePulse() {
        this.firePulse = false;
    }

    /// > gameplay 权威状态镜像是否包含指定状态（每 tick 从 operator 同步）。
    /// > 多状态可并存，故以成员查询方式读取。
    public boolean hasGunHolderState(GunHolderState state) {
        return gunHolderStates.contains(state);
    }

    /// > 模块模式镜像（PRIMARY / UNDER_BARREL）。
    public GunModuleMode moduleMode() {
        return moduleMode;
    }

    public boolean isUnderBarrelMode() {
        return moduleMode == GunModuleMode.UNDER_BARREL;
    }

    /// > 是否正处于模式切换过渡（镜像 operator）。
    public boolean isModeSwitching() {
        return modeSwitching;
    }

    /// > 模式切换过渡进度 0~1（无过渡时为 1）。host_overlay 持枪姿态权重据此淡入淡出。
    public float modeTransitionProgress() {
        return modeTransitionProgress;
    }

    /// > 当前 reload 期望动画时长（秒）。
    public float reloadExpectedSeconds() {
        return reloadExpectedSeconds;
    }

    /// > 当前拉栓上膛期望动画时长（秒），由整枪 bolt_action_time（tick）推导。
    /// > 内容包可用于 play_montage / play_scaled 变速对齐 gameplay bolt 窗口；&lt;=0 表示原速。
    public float boltExpectedSeconds() {
        if (!(itemStack.getItem() instanceof IGun gun)) {
            return 0.0f;
        }
        GunData data = gun.getIndex(itemStack).map(GunIndex::getData).orElse(null);
        if (data == null || data.getBoltActionTime() <= 0) {
            return 0.0f;
        }
        return data.getBoltActionTime() / 20.0f;
    }

    /// > 是否处于空挂（弹匣空 + 膛内空）状态。
    /// > 纯客户端派生：仅 closed/manual（usesChamber）枪有意义，open 永远不空挂。
    /// > 读 NBT chambered（服务端权威同步值）+ 弹匣计数，预测错误靠同步自愈。
    public boolean boltCaught() {
        if (!(itemStack.getItem() instanceof IGun gun)) {
            return false;
        }
        GunData data = gun.getIndex(itemStack).map(GunIndex::getData).orElse(null);
        if (data == null) {
            return false;
        }
        if (!BoltType.resolve(data).usesChamber()) {
            return false;
        }
        return !AmmoDataHelper.isChambered(itemStack) && AmmoDataHelper.getAmmoCount(itemStack) <= 0;
    }

    /// > 当前开火模式索引（NBT FireModeIndex）。供快慢机切换动画按目标模式分流。
    public int fireModeIndex() {
        return FireModeHelper.getFireModeIndex(itemStack);
    }

    /// > 指定直接槽位是否安装了配件（不递归子槽位）。供状态机按配件切换动画表现，
    /// > 如 kar98 装镜时播 {@code shoot_scope}/{@code bolt_scope}、挡桥夹导轨只能逐发换弹。
    /// > <p>
    /// > {@code slotId} 缺省 namespace 时补 {@code tacz:}。纯客户端读当前物品配件树，无副作用。
    public boolean hasAttachment(String slotId) {
        if (slotId == null || slotId.isBlank() || !(itemStack.getItem() instanceof IGun iGun)) {
            return false;
        }
        String normalized = slotId.contains(":") ? slotId : Ares.MOD_ID + ":" + slotId;
        Identifier slotRl = Identifier.tryParse(normalized);
        if (slotRl == null) {
            return false;
        }
        AttachmentSlot slot = iGun.getAttachmentSlots(itemStack).get(slotRl);
        if (slot == null) {
            return false;
        }
        ItemStack attachment = iGun.getAttachment(itemStack, slot);
        return attachment != null && !attachment.isEmpty();
    }

    /// > 设置 reload 期望动画时长（秒）。由 controller.reload 写入。
    public void setReloadExpectedSeconds(float seconds) {
        this.reloadExpectedSeconds = seconds;
    }

    /// > 开始逐发换弹镜像会话：记录起始弹数，并复用玩法侧预测规则计算本次目标循环数。
    public void beginShellReload() {
        beginShellReload(-1);
    }

    /// > 开始逐发换弹镜像会话（带脚本覆盖）：记录起始弹数并确定本次目标循环数。
    /// > <p>
    /// > {@code targetLoopsOverride >= 0} 时直接采用（脚本编排换弹路径由共享脚本给定，见 {@code reloadShellLoops}）；
    /// > 否则回退按 {@code shell_by_shell} 数据规则预测（标准逐发枪不受影响）。
    /// >
    /// > @param targetLoopsOverride 脚本给定的目标循环数；&lt; 0 表示无覆盖、走数据推导
    public void beginShellReload(int targetLoopsOverride) {
        shellReloadStartAmmoCount = currentAmmoCount();
        shellReloadTargetLoops = targetLoopsOverride >= 0
                ? targetLoopsOverride
                : predictShellReloadLoops(shellReloadStartAmmoCount);
        shellReloadPlayedLoops = 0;
    }

    public int shellReloadTargetLoops() {
        return shellReloadTargetLoops;
    }

    public int shellReloadLoopsDone() {
        return Math.max(0, currentAmmoCount() - shellReloadStartAmmoCount);
    }

    public int shellReloadPlayedLoops() {
        return shellReloadPlayedLoops;
    }

    public void markShellReloadLoopPlayed() {
        shellReloadPlayedLoops = Math.max(shellReloadPlayedLoops + 1, shellReloadLoopsDone() + 1);
    }

    public HostOverlayBridge hostOverlayBridge() {
        return hostOverlayBridge;
    }

    public void setHostOverlayBridge(HostOverlayBridge hostOverlayBridge) {
        this.hostOverlayBridge = hostOverlayBridge != null ? hostOverlayBridge : HostOverlayBridge.NONE;
    }

    private int predictShellReloadLoops(int currentAmmo) {
        if (player == null || !(itemStack.getItem() instanceof IGun gun)) {
            return 0;
        }
        GunData data = gun.getIndex(itemStack).map(GunIndex::getData).orElse(null);
        if (data == null || !"shell_by_shell".equals(data.getReloadType()) || data.getShellReload() == null) {
            return 0;
        }
        int needed = Math.max(0, data.getMagazineCapacity() - currentAmmo);
        int available = AmmoInventoryHelper.countAmmoInInventory(player, data.getAcceptedAmmoIds());
        return Math.min(needed, available);
    }

    private int currentAmmoCount() {
        return AmmoDataHelper.getAmmoCount(itemStack);
    }
}
