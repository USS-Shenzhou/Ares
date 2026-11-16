package cn.ussshenzhou.ares.gun.gameplay.timeline;

/// > 动作许可集：表达「当前允许哪些玩法动作」的不可变位标志集合。
/// > <p>
/// > 命名刻意避开 {@code Capabilities}，以免与 Forge capability 混淆。
/// > <p>
/// > 默认全部允许；每条活跃轨道通过 {@link Builder#constrain} 逐步收紧（按位与语义）。
/// > 由 {@link SegmentSet#gate()} 聚合所有活跃片段的约束后得到当前许可集，
/// > operator 的输入入口据此判断动作是否放行，取代旧的 {@code GunHolderState} 布尔表。
/// @author TACZ, USS_Shenzhou
public class ActionGate {

    /// > 玩法动作枚举。新增行为驱动动作（过热、近战等）时在此登记。
    public enum Action {
        SHOOT,
        RELOAD,
        AIM,
        SPRINT,
        INSPECT,
        SWITCH_MODE
    }

    private final boolean canShoot;
    private final boolean canReload;
    private final boolean canAim;
    private final boolean canSprint;
    private final boolean canInspect;
    private final boolean canSwitchMode;

    private ActionGate(Builder b) {
        this.canShoot = b.canShoot;
        this.canReload = b.canReload;
        this.canAim = b.canAim;
        this.canSprint = b.canSprint;
        this.canInspect = b.canInspect;
        this.canSwitchMode = b.canSwitchMode;
    }

    /// > 查询是否允许某个动作。
    public boolean allows(Action action) {
        return switch (action) {
            case SHOOT -> canShoot;
            case RELOAD -> canReload;
            case AIM -> canAim;
            case SPRINT -> canSprint;
            case INSPECT -> canInspect;
            case SWITCH_MODE -> canSwitchMode;
        };
    }

    public boolean canShoot() {
        return canShoot;
    }

    public boolean canReload() {
        return canReload;
    }

    public boolean canAim() {
        return canAim;
    }

    public boolean canSprint() {
        return canSprint;
    }

    public boolean canInspect() {
        return canInspect;
    }

    public boolean canSwitchMode() {
        return canSwitchMode;
    }

    /// > 新建一个默认全部允许的 builder。
    public static Builder builder() {
        return new Builder();
    }

    /// > 许可集构造器。默认全部允许，调用 {@code deny*} 逐项收紧（按位与，只能更严不能放宽）。
    public static class Builder {
        private boolean canShoot = true;
        private boolean canReload = true;
        private boolean canAim = true;
        private boolean canSprint = true;
        private boolean canInspect = true;
        private boolean canSwitchMode = true;

        public Builder denyShoot() {
            this.canShoot = false;
            return this;
        }

        public Builder denyReload() {
            this.canReload = false;
            return this;
        }

        public Builder denyAim() {
            this.canAim = false;
            return this;
        }

        public Builder denySprint() {
            this.canSprint = false;
            return this;
        }

        public Builder denyInspect() {
            this.canInspect = false;
            return this;
        }

        public Builder denySwitchMode() {
            this.canSwitchMode = false;
            return this;
        }

        public ActionGate build() {
            return new ActionGate(this);
        }
    }
}
