package cn.ussshenzhou.ares.gun.gameplay.timeline;

import cn.ussshenzhou.ares.attachment.entity.GunHolderState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/// > 片段集合：operator 持有一份，统一管理所有活跃动作片段。
/// > <p>
/// > 多个 {@link ActionSegment} 并行推进（非互斥、可叠加，如冷却叠在拉栓上、瞄准叠在换弹上）。
/// > <ul>
/// >   <li>{@link #tick(long)} 推进全部片段并移除结束的；</li>
/// >   <li>{@link #gate()} 聚合所有活跃片段的约束，得到当前动作许可集，取代旧的枚举布尔表门禁；</li>
/// >   <li>{@link #projectStates()} 收集所有活跃片段投影出的 {@link GunHolderState} 为一个集合
/// >       （多状态可并存，如 bolt 窗口期同时有 BOLT_ACTION 与 FIRE_COOLDOWN），供动画 Molang /
/// >       HUD / 网络同步以「成员查询」方式读取。不再压成单值、不再需要优先级全序表。</li>
/// > </ul>
/// >
/// > <h3>可重入安全</h3>
/// > 片段可在自己的 tick / notify / phase 回调里编排兄弟片段（{@link #add}/{@link #cancel}）。
/// > 为避免遍历期间结构变更抛 {@link java.util.ConcurrentModificationException}，{@link #tick(long)}
/// > 对当前片段做快照后再遍历；遍历期间产生的增删先入队，遍历结束后统一应用。
/// @author TACZ, USS_Shenzhou
public class SegmentSet {

    /// > 按 id 维护的活跃片段。LinkedHashMap 保证迭代顺序稳定（便于调试与确定性）。
    private final Map<String, ActionSegment> segments = new LinkedHashMap<>();

    /// > 是否正在遍历推进（tick 期间为 true）。遍历期间的结构变更延迟到遍历结束后应用。
    private boolean iterating = false;
    /// > 遍历期间挂起的待加入片段（id -> segment，后者覆盖前者，与即时 add 的「重复替换」语义一致）。
    private final Map<String, ActionSegment> pendingAdd = new LinkedHashMap<>();
    /// > 遍历期间挂起的待移除片段 id。
    private final List<String> pendingRemove = new ArrayList<>();

    /// > 加入一个片段。重复 id 替换旧片段（先 cancel 旧片段），实现「重新触发」语义。
    /// > 遍历期间调用则延迟到本轮 tick 结束后应用。
    public void add(ActionSegment segment) {
        if (iterating) {
            pendingRemove.remove(segment.id());
            ActionSegment prevPending = pendingAdd.put(segment.id(), segment);
            if (prevPending != null && prevPending != segment) {
                prevPending.cancel();
                prevPending.attach(null);
            }
            return;
        }
        ActionSegment old = segments.put(segment.id(), segment);
        segment.attach(this);
        if (old != null && old != segment) {
            old.cancel();
            old.attach(null);
        }
    }

    /// > 推进全部片段，移除结束（tick 返回 false 或 isActive() 为 false）的片段。
    public void tick(long deltaMs) {
        if (segments.isEmpty() && pendingAdd.isEmpty()) {
            return;
        }
        /// > 快照当前片段引用，遍历期间产生的增删走 pending 队列
        List<ActionSegment> snapshot = new ArrayList<>(segments.values());
        iterating = true;
        List<String> finished = null;
        try {
            for (ActionSegment segment : snapshot) {
                /// > 可能已被本轮其它片段取消并移除
                if (segments.get(segment.id()) != segment) {
                    continue;
                }
                boolean stillActive = segment.tick(deltaMs) && segment.isActive();
                if (!stillActive) {
                    if (finished == null) {
                        finished = new ArrayList<>();
                    }
                    finished.add(segment.id());
                }
            }
        } finally {
            iterating = false;
        }
        if (finished != null) {
            for (String id : finished) {
                ActionSegment removed = segments.remove(id);
                if (removed != null) {
                    removed.attach(null);
                }
            }
        }
        flushPending();
    }

    /// > 应用遍历期间挂起的增删。
    private void flushPending() {
        if (!pendingRemove.isEmpty()) {
            for (String id : pendingRemove) {
                ActionSegment removed = segments.remove(id);
                if (removed != null) {
                    removed.cancel();
                    removed.attach(null);
                }
            }
            pendingRemove.clear();
        }
        if (!pendingAdd.isEmpty()) {
            for (ActionSegment segment : pendingAdd.values()) {
                add(segment);
            }
            pendingAdd.clear();
        }
    }

    /// > 聚合所有活跃片段的约束，得到当前动作许可集。
    public ActionGate gate() {
        ActionGate.Builder builder = ActionGate.builder();
        for (ActionSegment segment : segments.values()) {
            segment.constrain(builder);
        }
        return builder.build();
    }

    /// > 收集所有活跃片段投影出的状态为集合（可并存）。空集表示「空闲」（等价于旧的 NORMAL）。
    public EnumSet<GunHolderState> projectStates() {
        EnumSet<GunHolderState> states = EnumSet.noneOf(GunHolderState.class);
        for (ActionSegment segment : segments.values()) {
            GunHolderState projected = segment.projectedState();
            if (projected != null) {
                states.add(projected);
            }
        }
        return states;
    }

    /// > 是否空闲：没有任何片段投影出状态（等价于旧的 currentState == NORMAL）。
    public boolean isIdle() {
        for (ActionSegment segment : segments.values()) {
            if (segment.projectedState() != null) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public ActionSegment get(String id) {
        ActionSegment pending = pendingAdd.get(id);
        return pending != null ? pending : segments.get(id);
    }

    @Nullable
    public <T extends ActionSegment> T get(SegmentToken<T> token) {
        return token.cast(get(token.id()));
    }

    public boolean has(String id) {
        return segments.containsKey(id) || pendingAdd.containsKey(id);
    }

    public boolean has(SegmentToken<?> token) {
        return has(token.id());
    }

    /// > 当前全部活跃片段（迭代顺序稳定），供调试 overlay 渲染。
    public Collection<ActionSegment> activeSegments() {
        return Collections.unmodifiableCollection(segments.values());
    }

    /// > 调试展示行：每行形如 "id [状态]: debugInfo"。供 debug overlay 渲染。
    public List<String> debugLines() {
        List<String> lines = new ArrayList<>(segments.size());
        for (ActionSegment segment : segments.values()) {
            GunHolderState projected = segment.projectedState();
            String stateTag = projected != null ? " <" + projected.name() + ">" : "";
            lines.add(segment.id() + stateTag + ": " + segment.debugInfo());
        }
        return lines;
    }

    /// > 取消并移除指定片段。遍历期间调用则延迟到本轮 tick 结束后应用。
    public void cancel(SegmentToken<?> token) {
        cancel(token.id());
    }

    public void cancel(String id) {
        if (iterating) {
            pendingAdd.remove(id);
            if (!pendingRemove.contains(id)) {
                pendingRemove.add(id);
            }
            return;
        }
        ActionSegment segment = segments.remove(id);
        if (segment != null) {
            segment.cancel();
            segment.attach(null);
        }
    }

    /// > 取消并清空全部片段。
    public void clear() {
        for (ActionSegment segment : segments.values()) {
            segment.cancel();
            segment.attach(null);
        }
        segments.clear();
        for (ActionSegment segment : pendingAdd.values()) {
            segment.cancel();
            segment.attach(null);
        }
        pendingAdd.clear();
        pendingRemove.clear();
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }
}
