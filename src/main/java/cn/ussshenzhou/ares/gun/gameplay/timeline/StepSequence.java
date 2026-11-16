package cn.ussshenzhou.ares.gun.gameplay.timeline;

import javax.annotation.Nullable;

/// > 顺序复合行为的分步描述驱动器（与 [Timer] 并列的纯计时件）。
/// >
/// > 用于编排「相位 A 持续 t1 → 相位 B 持续 t2 → …」这类顺序时间线（如逐发换弹的
/// > intro → loop* → end）。本类**只负责计时与流转**：推进当前 [Step] 的计时，
/// > 到点调用 [Step#advance()] 取下一步并起表。
/// >
/// > **时间线零副作用**：副作用与「下一步是什么」的数据决策全部封装在 [Step]
/// > 实现体里，由编排层提供。`StepSequence` 不读 NBT、不做硬编码分支。
/// >
/// > 计时单位统一为毫秒，推进由 tick 驱动（每 tick 累加 `deltaMs`）。
/// @author TACZ, USS_Shenzhou
public class StepSequence {

    /// > 一个时序步骤：声明自身时长，并在计时结束时执行副作用任务、做数据决策、返回下一步。
    public interface Step {
        /// > 本步时长（毫秒）。<= 0 时下一次 tick 即结束本步。
        long durationMs();

        /// > 进入本步时调用（起表前）。默认无操作。
        default void onEnter() {
        }

        /// > 本步计时结束时调用：执行本步的副作用任务（扣弹 / 写 NBT / 钩子 / 事件等），
        /// > 并据数据决策返回下一步；返回 `null` 表示整条序列结束。
        @Nullable
        Step advance();
    }

    private final Timer timer = new Timer();
    @Nullable
    private Step current;

    /// > 启动序列：进入初始步。initial 为 null 时序列直接视为已结束。
    public void start(@Nullable Step initial) {
        enter(initial);
    }

    private void enter(@Nullable Step step) {
        this.current = step;
        if (step != null) {
            step.onEnter();
            timer.start(Math.max(step.durationMs(), 0));
        } else {
            timer.stop();
        }
    }

    /// > 推进序列。可能在一拍内连续跨越多个零时长步骤。
    /// >
    /// > @return true 表示序列仍在运行；false 表示已结束（当前步为 null）
    public boolean tick(long deltaMs) {
        /// > 单拍内可能跨多个步骤：当前步到点 → advance 取下一步 → 若下一步零时长则继续
        while (current != null && timer.tick(deltaMs)) {
            enter(current.advance());
            /// > 后续步骤在同一拍内不再额外消耗 deltaMs，仅处理零时长链
            deltaMs = 0;
        }
        return current != null;
    }

    /// > 是否仍在运行（尚有未完成的步骤）。
    public boolean isRunning() {
        return current != null;
    }

    /// > 当前步剩余毫秒数（未运行时为 0）。
    public long remainingMs() {
        return timer.remainingMs();
    }

    /// > 硬停：立即终止序列，**不**执行剩余步骤的副作用任务。
    /// > 用于输入打断（如换弹被开火 / 疾跑打断），已产生的副作用保留，未到点的不触发。
    public void cancel() {
        this.current = null;
        timer.stop();
    }
}
