package cn.ussshenzhou.ares.gun.client.animation.statemachine;

import com.mojang.logging.LogUtils;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.molang.MolangEngineHelper;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.MochaEngine;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.binding.QueryBinding;
import cn.ussshenzhou.ares.util.GunHolderState;
import cn.ussshenzhou.ares.gun.client.animation.context.AnimationEventSink;
import cn.ussshenzhou.ares.gun.client.animation.context.AnimationNotifyEvent;
import cn.ussshenzhou.ares.gun.client.animation.context.WeaponAnimationContext;
import cn.ussshenzhou.ares.gun.util.AttributeDataHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/// > 状态机的 Molang 上下文。按语义拆分命名空间：
/// > - 查询类绑定（`gun.` 命名空间，无副作用，返回 double）：供过渡 condition / 读状态使用，
/// >   如 `gun.movement_run`、`gun.gameplay_reloading`。
/// > - 动作类绑定（`action.` 命名空间，有副作用，返回 double=0）：供 onEnter/onUpdate/afterTrigger 使用，
/// >   如 `action.play_once('primary','run_start')`、`action.consume_request()`。
/// > - 动画 timeline 指令（`action.` 命名空间，有副作用）：供动画文件内嵌 timeline 使用，
/// >   如 `action.notify('mag_out')`。
/// >
/// > 标准 molang 查询（如 `query.anim_time`）仍保留 `query.` 命名空间，不可动。
/// @author TACZ, USS_Shenzhou
public class GunMolangContext extends MolangContext<GunStateContext> {
    /// > 编译期共享引擎。动画 timeline 预编译只需类型信息（扫描 `@QueryBinding`），
    /// > 与求值阶段的 context 实例无关，故全局复用一份，避免重复反射扫描。
    private static volatile MochaEngine<?> sharedCompileEngine;

    /// > timeline 指令执行所需的事件转发目标与定位信息（按播放后端注入）。
    private AnimationEventSink timelineSink = AnimationEventSink.NONE;
    private Identifier timelineMontageId;
    private String timelineTrackName;

    /// > 激光束流已存活时长（秒）。供激光视觉表达式（如 query.beam_time）在脱离状态机时独立驱动，
    /// > 由 LaserVisualResolver 每帧注入。默认 0。
    private double beamTimeSeconds;

    /// > 已告警过的「未注册属性」id（按 id 只告警一次，避免每帧刷屏）。
    /// >
    /// > 仅记录能解析为合法 [Identifier] 的 id：解析失败时日志里不回显原始字符串，
    /// > 避免把数据包里的任意文本写进日志。
    private static final Set<Identifier> WARNED_UNKNOWN_ATTRIBUTES = ConcurrentHashMap.newKeySet();

    public GunMolangContext() {
    }

    public GunMolangContext(GunStateContext context) {
        super(context);
    }

    /// > 获取动画 timeline 预编译用的共享引擎。
    /// >
    /// > 用 [GunMolangContext] 类型构建，使 timeline 中的 `action.notify(...)`
    /// > 等命名空间绑定能在编译期解析为直接方法调用。
    public static MochaEngine<?> sharedCompileEngine() {
        MochaEngine<?> engine = sharedCompileEngine;
        if (engine == null) {
            synchronized (GunMolangContext.class) {
                engine = sharedCompileEngine;
                if (engine == null) {
                    engine = MolangEngineHelper.createEngine(GunMolangContext.class);
                    sharedCompileEngine = engine;
                }
            }
        }
        return engine;
    }

    /// > 注入 timeline 指令执行所需的事件 sink 与定位信息。供播放后端在消费 timeline 关键帧前调用。
    public void configureTimeline(AnimationEventSink sink, Identifier montageId, String trackName) {
        this.timelineSink = sink != null ? sink : AnimationEventSink.NONE;
        this.timelineMontageId = montageId;
        this.timelineTrackName = trackName;
    }

    /// > 注入激光束流已存活时长（秒）。供 `query.beam_time` 读取，驱动激光视觉表达式。
    public void setBeamTimeSeconds(double beamTimeSeconds) {
        this.beamTimeSeconds = beamTimeSeconds;
    }

    /// > ==================== 激光视觉查询绑定（query. 命名空间，脱离状态机可用）====================

    /// > 激光束流已存活时长（秒）。由 [cn.ussshenzhou.ares.gun.client.render.fx.LaserVisualResolver]
    /// > 每帧注入，驱动束流随时间渐变色/变宽等表现。
    /// >
    /// > 预留：后续可按同一模式加入 `query.charge_progress`、`query.heat` 等表现驱动量。
    @QueryBinding(value = "beam_time", namespace = "query")
    public double beamTime() {
        return beamTimeSeconds;
    }

    /// > ==================== 查询绑定（过渡 condition 用，gun. 命名空间）====================

    @QueryBinding(value = "move_speed", namespace = "gun")
    public double moveSpeed() {
        WeaponAnimationContext context = weaponContext();
        return context != null ? context.moveSpeed() : 0.0;
    }

    @QueryBinding(value = "aim_progress", namespace = "gun")
    public double aimProgress() {
        WeaponAnimationContext context = weaponContext();
        return context != null ? context.aimProgress() : 0.0;
    }

    /// > 目标瞄准视图位是否为战术拒枪姿态（0/1）。Alt+V 切换请求即翻转，不等过渡收敛，
    /// > 便于状态机立刻起姿态；平滑混合用 [#aimViewSwitchProgress()]。
    @QueryBinding(value = "tactical_stance", namespace = "gun")
    public double tacticalStance() {
        WeaponAnimationContext context = weaponContext();
        return context != null && context.tacticalStance() ? 1.0 : 0.0;
    }

    /// > 瞄准视图位切换过渡进度（0~1，未在切换时恒为 1）。
    @QueryBinding(value = "aim_view_switch_progress", namespace = "gun")
    public double aimViewSwitchProgress() {
        WeaponAnimationContext context = weaponContext();
        return context != null ? context.aimViewSwitchProgress() : 1.0;
    }

    @QueryBinding(value = "sprinting", namespace = "gun")
    public double sprinting() {
        WeaponAnimationContext context = weaponContext();
        return context != null && context.sprinting() ? 1.0 : 0.0;
    }

    @QueryBinding(value = "on_ground", namespace = "gun")
    public double onGround() {
        WeaponAnimationContext context = weaponContext();
        return context != null && context.onGround() ? 1.0 : 0.0;
    }

    /// > 指定轨道的动画是否播放结束（用于 once 轨道驱动的过渡，如 run_start -> run）。
    @QueryBinding(value = "track_end", namespace = "gun")
    public double trackEnd(String track) {
        GunStateContext context = getEntity();
        return context != null && context.tracks().isEnd(track) ? 1.0 : 0.0;
    }

    @QueryBinding(value = "movement_idle", namespace = "gun")
    public double movementIdle() {
        return movementState("idle");
    }

    @QueryBinding(value = "movement_walk_forward", namespace = "gun")
    public double movementWalkForward() {
        return movementState("walk_forward");
    }

    @QueryBinding(value = "movement_walk_backward", namespace = "gun")
    public double movementWalkBackward() {
        return movementState("walk_backward");
    }

    @QueryBinding(value = "movement_walk_sideway", namespace = "gun")
    public double movementWalkSideway() {
        return movementState("walk_sideway");
    }

    @QueryBinding(value = "movement_walk_aiming", namespace = "gun")
    public double movementWalkAiming() {
        return movementState("walk_aiming");
    }

    @QueryBinding(value = "movement_run", namespace = "gun")
    public double movementRun() {
        return movementState("run");
    }

    @QueryBinding(value = "movement_run_hold", namespace = "gun")
    public double movementRunHold() {
        return movementState("run_hold");
    }

    @QueryBinding(value = "running_like", namespace = "gun")
    public double runningLike() {
        WeaponAnimationContext context = weaponContext();
        if (context == null) {
            return 0.0;
        }
        String state = context.movementStateId();
        return "run".equals(state) || "run_hold".equals(state) ? 1.0 : 0.0;
    }

    /// > ==================== base 主状态机查询绑定（gun. 命名空间）====================

    /// > 是否存在指定请求信号（reload_empty / reload_tactical / inspect / holster）。
    @QueryBinding(value = "request", namespace = "gun")
    public double request(String request) {
        WeaponAnimationContext context = weaponContext();
        return context != null && context.hasRequest(request) ? 1.0 : 0.0;
    }

    /// > 当前帧是否有 fire 脉冲（用于打断 reload/inspect）。
    @QueryBinding(value = "fire_pulse", namespace = "gun")
    public double firePulse() {
        WeaponAnimationContext context = weaponContext();
        return context != null && context.firePulse() ? 1.0 : 0.0;
    }

    /// > 是否存在尚未被状态机消费的真实出膛事件。
    @QueryBinding(value = "has_unconsumed_fire", namespace = "gun")
    public double hasUnconsumedFire() {
        WeaponAnimationContext context = weaponContext();
        return context != null && context.hasUnconsumedFire() ? 1.0 : 0.0;
    }

    /// > 累计出膛事件序号，供内容包调试或高级条件使用。
    @QueryBinding(value = "fire_serial", namespace = "gun")
    public double fireSerial() {
        WeaponAnimationContext context = weaponContext();
        return context != null ? context.fireSerial() : 0.0;
    }

    /// > gameplay 权威是否处于换弹中。
    @QueryBinding(value = "gameplay_reloading", namespace = "gun")
    public double gameplayReloading() {
        WeaponAnimationContext context = weaponContext();
        return context != null && context.hasGunHolderState(GunHolderState.RELOADING) ? 1.0 : 0.0;
    }

    /// > gameplay 权威是否处于检视中。
    @QueryBinding(value = "gameplay_inspecting", namespace = "gun")
    public double gameplayInspecting() {
        WeaponAnimationContext context = weaponContext();
        return context != null && context.hasGunHolderState(GunHolderState.INSPECT) ? 1.0 : 0.0;
    }

    /// > gameplay 权威是否处于拉栓上膛中（栓动武器射击后的 bolt 冷却窗口）。
    @QueryBinding(value = "gameplay_bolting", namespace = "gun")
    public double gameplayBolting() {
        WeaponAnimationContext context = weaponContext();
        return context != null && context.hasGunHolderState(GunHolderState.BOLT_ACTION) ? 1.0 : 0.0;
    }

    /// > 当前拉栓期望动画时长（秒），供 play_montage / play_scaled 变速对齐 gameplay bolt 窗口。
    @QueryBinding(value = "bolt_expected_seconds", namespace = "gun")
    public double boltExpectedSeconds() {
        WeaponAnimationContext context = weaponContext();
        return context != null ? context.boltExpectedSeconds() : 0.0;
    }

    /// > 是否处于空挂（弹匣空 + 膛内空）状态。供 bolt_caught 静态姿势条件显示。
    @QueryBinding(value = "bolt_caught", namespace = "gun")
    public double boltCaught() {
        WeaponAnimationContext context = weaponContext();
        return context != null && context.boltCaught() ? 1.0 : 0.0;
    }

    /// > 当前开火模式索引（NBT FireModeIndex），供快慢机切换动画按目标模式分流播放。
    @QueryBinding(value = "fire_mode_index", namespace = "gun")
    public double fireModeIndex() {
        WeaponAnimationContext context = weaponContext();
        return context != null ? context.fireModeIndex() : 0.0;
    }

    /// > 指定直接槽位是否安装了配件（如 `gun.has_attachment('tacz:scope')`）。
    /// > 供状态机按配件切换动画表现（镜/无镜开火、拉栓、桥夹换弹分流）。
    @QueryBinding(value = "has_attachment", namespace = "gun")
    public double hasAttachment(String slotId) {
        WeaponAnimationContext context = weaponContext();
        return context != null && context.hasAttachment(slotId) ? 1.0 : 0.0;
    }

    /// > 指定属性的最终值（含配件 / 弹种 / 开火模式修饰）。属性 id 缺省 namespace 补 `tacz:`。
    /// >
    /// > Molang 没有 null：未注册的 id 视为作者错误，返回 0 并按 id 限流告警一次。
    /// > 判定「本枪是否有该属性」请用 [#hasAttribute(String)]。
    @QueryBinding(value = "attribute", namespace = "gun")
    public double attribute(String attrId) {
        WeaponAnimationContext context = weaponContext();
        ItemStack stack = context != null ? context.itemStack() : null;
        if (stack == null || stack.isEmpty()) {
            return 0.0;
        }
        Double value = AttributeDataHelper.getAttribute(stack, attrId);
        if (value == null) {
            warnUnknownAttribute(attrId);
            return 0.0;
        }
        return value;
    }

    /// > 指定属性在本枪上是否被显式赋值（枪械基础属性声明，或任一修饰来源）。
    @QueryBinding(value = "has_attribute", namespace = "gun")
    public double hasAttribute(String attrId) {
        WeaponAnimationContext context = weaponContext();
        return context != null && AttributeDataHelper.hasAttribute(context.itemStack(), attrId) ? 1.0 : 0.0;
    }

    private static void warnUnknownAttribute(String attrId) {
        Identifier id = AttributeDataHelper.parseId(attrId);
        if (id == null) {
            return;
        }
        if (WARNED_UNKNOWN_ATTRIBUTES.add(id)) {
            LogUtils.getLogger().warn("Unknown gun attribute referenced from Molang, returns 0: {}", id);
        }
    }

    /// > 当前 reload 期望动画时长（秒），供 play_montage 变速对齐 gameplayTicks。
    @QueryBinding(value = "reload_expected_seconds", namespace = "gun")
    public double reloadExpectedSeconds() {
        WeaponAnimationContext context = weaponContext();
        return context != null ? context.reloadExpectedSeconds() : 0.0;
    }

    /// > 逐发换弹本次目标循环数，由客户端按玩法规则预测。
    @QueryBinding(value = "shell_target_loops", namespace = "gun")
    public double shellTargetLoops() {
        WeaponAnimationContext context = weaponContext();
        return context != null ? context.shellReloadTargetLoops() : 0.0;
    }

    /// > 逐发换弹已完成循环数，由当前弹数与 reload 开始弹数快照推导。
    @QueryBinding(value = "shell_loops_done", namespace = "gun")
    public double shellLoopsDone() {
        WeaponAnimationContext context = weaponContext();
        return context != null ? context.shellReloadLoopsDone() : 0.0;
    }

    /// > 逐发换弹已启动播放的 loop 次数，避免等待弹数同步时重复重启同一发动画。
    @QueryBinding(value = "shell_played_loops", namespace = "gun")
    public double shellPlayedLoops() {
        WeaponAnimationContext context = weaponContext();
        return context != null ? context.shellReloadPlayedLoops() : 0.0;
    }

    /// > ==================== 动作绑定（onEnter/onUpdate/afterTrigger 用，action. 命名空间）====================

    /// > 在指定轨道循环播放动画。
    @QueryBinding(value = "play_loop", namespace = "action")
    public double playLoop(String track, String animation) {
        GunStateContext context = getEntity();
        if (context != null) {
            context.tracks().playLoop(track, animation);
        }
        return 0.0;
    }

    /// > 幂等循环播放：已在播同一动画则跳过（不重置相位）。
    /// > 供条件性常驻轨道（如空挂静态姿势）在 onUpdate 中每帧调用，例如
    /// > `action.ensure_loop('bolt_caught', 'static_bolt_caught')`。
    @QueryBinding(value = "ensure_loop", namespace = "action")
    public double ensureLoop(String track, String animation) {
        GunStateContext context = getEntity();
        if (context != null) {
            context.tracks().ensureLoop(track, animation);
        }
        return 0.0;
    }

    /// > 幂等停止指定轨道。与 stop_track 等价，命名对称供条件轨道在 onUpdate 中使用。
    @QueryBinding(value = "ensure_stop", namespace = "action")
    public double ensureStop(String track) {
        GunStateContext context = getEntity();
        if (context != null) {
            context.tracks().ensureStop(track);
        }
        return 0.0;
    }

    /// > 在指定轨道播放一次动画，播完停止。
    @QueryBinding(value = "play_once", namespace = "action")
    public double playOnce(String track, String animation) {
        GunStateContext context = getEntity();
        if (context != null) {
            context.tracks().playOnce(track, animation);
        }
        return 0.0;
    }

    /// > 在指定轨道播放一次动画并按期望时长变速对齐玩法时长（如 reload）。播完停止。
    /// >
    /// > 第三参 expectedSeconds 由调用方传玩法权威时长（<=0 表示原速），如
    /// > `action.play_scaled('base','reload_empty', gun.reload_expected_seconds)`。
    /// > 时长 / loop / notify 全部读自动画文件，无需 montage。
    @QueryBinding(value = "play_scaled", namespace = "action")
    public double playScaled(String track, String animation, double expectedSeconds) {
        GunStateContext context = getEntity();
        if (context != null) {
            context.tracks().playScaled(track, animation, (float) expectedSeconds);
        }
        return 0.0;
    }

    /// > 在指定轨道播放手动相位动画，相位由 set_locomotion_phase 推进。
    @QueryBinding(value = "play_manual", namespace = "action")
    public double playManual(String track, String animation) {
        GunStateContext context = getEntity();
        if (context != null) {
            context.tracks().playManual(track, animation);
        }
        return 0.0;
    }

    /// > 在指定轨道播放 montage 驱动的一次性动作。
    /// >
    /// > 第四参 expectedDurationSeconds 用于按期望时长变速对齐 gameplayTicks；省略或 <=0 表示原速。
    /// > 内容包可写 `action.play_montage('reload','tacz:m16/reload_empty','main')`
    /// > 或 `action.play_montage('reload','tacz:m16/reload_empty','main', gun.reload_expected_seconds)`。
    @QueryBinding(value = "play_montage", namespace = "action")
    public double playMontage(String track, String montageId, String section, double expectedDurationSeconds) {
        GunStateContext context = getEntity();
        if (context != null) {
            context.tracks().playMontage(track, Identifier.tryParse(montageId), section, (float) expectedDurationSeconds);
        }
        return 0.0;
    }

    /// > 设置 manual 轨道的 locomotion 相位参数（每 tick 据此推进）。
    @QueryBinding(value = "set_locomotion_phase", namespace = "action")
    public double setLocomotionPhase(String track, double cycleDistance, double scale) {
        GunStateContext context = getEntity();
        if (context != null) {
            context.tracks().setLocomotionPhase(track, (float) cycleDistance, (float) scale);
        }
        return 0.0;
    }

    /// > 停止并移除指定轨道。
    @QueryBinding(value = "stop_track", namespace = "action")
    public double stopTrack(String track) {
        GunStateContext context = getEntity();
        if (context != null) {
            context.tracks().stop(track);
        }
        return 0.0;
    }

    /// > 消费（清除）当前请求信号。供过渡 afterTrigger 调用，避免请求被重复触发。
    @QueryBinding(value = "consume_request", namespace = "action")
    public double consumeRequest() {
        WeaponAnimationContext context = weaponContext();
        if (context != null) {
            context.consumeRequest();
        }
        return 0.0;
    }

    /// > 标记逐发换弹已启动播放一次 loop 动画。
    @QueryBinding(value = "mark_shell_loop_played", namespace = "action")
    public double markShellLoopPlayed() {
        WeaponAnimationContext context = weaponContext();
        if (context != null) {
            context.markShellReloadLoopPlayed();
        }
        return 0.0;
    }

    /// > 消费当前待处理出膛事件，供 fire 状态机防止重复触发。
    @QueryBinding(value = "consume_fire", namespace = "action")
    public double consumeFire() {
        WeaponAnimationContext context = weaponContext();
        if (context != null) {
            context.consumeFire();
        }
        return 0.0;
    }

    @QueryBinding(value = "play_host_overlay_once", namespace = "action")
    public double playHostOverlayOnce(String animation) {
        WeaponAnimationContext context = weaponContext();
        if (context != null) {
            context.hostOverlayBridge().playHostOverlayOnce(animation);
        }
        return 0.0;
    }

    @QueryBinding(value = "play_host_overlay_scaled", namespace = "action")
    public double playHostOverlayScaled(String animation, double expectedSeconds) {
        WeaponAnimationContext context = weaponContext();
        if (context != null) {
            context.hostOverlayBridge().playHostOverlayScaled(animation, (float) expectedSeconds);
        }
        return 0.0;
    }

    @QueryBinding(value = "stop_host_overlay", namespace = "action")
    public double stopHostOverlay(String actionName) {
        WeaponAnimationContext context = weaponContext();
        if (context != null) {
            context.hostOverlayBridge().stopHostOverlay(actionName);
        }
        return 0.0;
    }

    /// > ==================== 动画 timeline 指令（动画文件内嵌 timeline 用）====================

    /// > 触发一个具名动画事件（如 `action.notify('mag_out')`）。
    /// >
    /// > 由动画文件内嵌 timeline 关键帧驱动，转发到注入的事件 sink。事件定位信息（montage/track）
    /// > 由播放后端通过 [#configureTimeline] 预先注入。
    @QueryBinding(value = "notify", namespace = "action")
    public double notifyAction(String event) {
        if (event != null && !event.isBlank()) {
            timelineSink.onNotify(new AnimationNotifyEvent(timelineMontageId, null, timelineTrackName, event, 0.0f));
        }
        return 0.0;
    }

    /// > ==================== 内部 ====================

    private double movementState(String expected) {
        WeaponAnimationContext context = weaponContext();
        return context != null && expected.equals(context.movementStateId()) ? 1.0 : 0.0;
    }

    private WeaponAnimationContext weaponContext() {
        GunStateContext context = getEntity();
        return context != null ? context.weaponContext() : null;
    }
}
