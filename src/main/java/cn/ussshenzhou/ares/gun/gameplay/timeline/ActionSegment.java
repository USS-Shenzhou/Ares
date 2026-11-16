package cn.ussshenzhou.ares.gun.gameplay.timeline;

import cn.ussshenzhou.ares.util.GunHolderState;
import javax.annotation.Nullable;

/// @author TACZ, USS_Shenzhou
public abstract class ActionSegment {

    /// > 片段类型 token，用于按类型安全取回同名片段。
    public abstract SegmentToken<? extends ActionSegment> token();

    /// > 片段唯一标识。
    public String id() {
        return token().id();
    }

    private boolean active = true;

    /// > 所属片段集合的 back-reference，由 [SegmentSet#add(ActionSegment)] 注入。
    /// > 片段可借此在自己的 notify / phase 边界编排兄弟片段（如开火片段在出膛时驱动冷却片段），
    /// > 把链式编排收进 timeline 层，而非散落到 operator 回调。未加入任何集合时为 null。
    @Nullable
    private SegmentSet owner;

    /// > 由 [SegmentSet] 在加入时调用，注入容器 back-reference。
    void attach(@Nullable SegmentSet set) {
        this.owner = set;
    }

    /// > 所属片段集合（可借此编排兄弟片段）。未加入集合时返回 null。
    @Nullable
    protected SegmentSet segments() {
        return owner;
    }

    /// > 推进本片段。
    /// >
    /// > @param deltaMs 本 tick 经过的毫秒数
    /// >
    /// > @return true 表示片段仍活跃；返回 false 时由 [SegmentSet] 移除
    public abstract boolean tick(long deltaMs);

    /// > 声明本片段对动作许可的约束。默认不约束（不收紧任何动作）。
    /// > 由 [SegmentSet#gate()] 在聚合时调用。
    public void constrain(ActionGate.Builder gate) {
    }

    /// > 本片段投影到的对外状态。默认 null 表示不参与投影。
    /// > 由 [SegmentSet#projectStates()] 收集为集合时读取（多状态可并存）。
    @Nullable
    public GunHolderState projectedState() {
        return null;
    }

    /// > 取消本片段。默认置为非活跃，子类可覆写以释放计时件 / 触发 phase 退出副作用。
    public void cancel() {
        this.active = false;
    }

    /// > 本片段是否仍活跃。
    public boolean isActive() {
        return active;
    }

    /// > 调试展示信息（供 debug overlay 渲染）。默认返回空串，
    /// > 持有计时件的子类可覆写以附加剩余时间 / 进度等。
    public String debugInfo() {
        return "";
    }

    /// > 标记片段结束（供子类在时序自然完成时调用）。
    protected void finish() {
        this.active = false;
    }
}
