package cn.ussshenzhou.ares.attachment.entity;

import cn.ussshenzhou.ares.gun.attribute.GunAttribute;
import cn.ussshenzhou.ares.gun.attribute.IGunProperties;
import cn.ussshenzhou.ares.util.GunOperationSessionKind;

/// > 持枪形态策略：承载「单手双持」与「双手」之间的真实差异。
/// >
/// > 形态差异与端差异（客户端预测 / 服务端权威）是正交的两条轴。端差异由
/// > `Client/ServerHandGunSession` 继承表达；形态差异则由本策略对象承载，
/// > 在 operator 创建会话时按 [GunOperationSessionKind] 注入，避免两轴继承产生笛卡尔积。
/// >
/// > 形态不是物品自身状态（同一把枪在主手 / 副手、单持 / 双持下形态可能不同，物品却没变），
/// > 因此与形态相关的属性修饰、时长调整必须在会话 / 策略层做二次处理，
/// > 不能进入以 `ItemStack` 为键的 [IGunProperties] 实现。
/// @author TACZ, USS_Shenzhou
public interface WieldProfile {
    /// > 操作 / 动画时长种类，供 [#adjustDurationTicks] 区分调整目标。
    enum DurationKind {
        DRAW,
        PUT_AWAY,
        RELOAD
    }

    GunOperationSessionKind kind();

    /// > 是否允许瞄准 / 开镜。双持时禁用。
    boolean canAim();

    /// > 是否允许检视。单手双持时禁用（验证期仅主手开放）。
    boolean canInspect();

    /// > 是否允许切换模块模式（快慢机 / 下挂）。单手双持时禁用。
    boolean canSwitchModule();

    /// > 在 [IGunProperties] 结果之上叠加形态相关修饰。
    /// > 例如单手双持降低部分属性。默认恒等返回，待具体形态实现填充。
    /// >
    /// > @param attr   属性
    /// >
    /// > @param cached 缓存计算出的属性值
    /// >
    /// > @return 形态修饰后的属性值
    default double modifyAttribute(GunAttribute attr, double cached) {
        return cached;
    }

    /// > 调整操作 / 动画时长（tick）。例如单手双持的掏枪 / 换弹时长不同。
    /// > 默认恒等返回，待具体形态实现填充。
    /// >
    /// > @param which     时长种类
    /// >
    /// > @param baseTicks 基础时长（tick）
    /// >
    /// > @return 形态调整后的时长（tick）
    default int adjustDurationTicks(DurationKind which, int baseTicks) {
        return baseTicks;
    }
}
