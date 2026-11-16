package cn.ussshenzhou.ares.gun.custom.gun;

import javax.annotation.Nullable;

/// > 枪机/上膛类型。决定开火与弹药记账的方式。
/// > - [#OPEN]：开膛待击。开火动作自带推弹击发，玩家永不拉栓，无膛内 +1。
/// >   AmmoCount 即总持有量，行为等同无 chamber 系统。
/// > - [#CLOSED]：闭膛待击。平时膛内有弹（chambered=true），开火后服务端即时补膛，
/// >   全自动不受网络往返影响。支持膛内 +1（弹匣满 + 膛内 = capacity+1）。
/// > - [#MANUAL]：手动栓动。开火后置 chambered=false，经 bolt 窗口后才补膛。
/// >   支持膛内 +1。无独立拉栓键，未上膛态由客户端驱动自动恢复。
///
/// @author TACZ, USS_Shenzhou
public enum BoltType {
    OPEN,
    CLOSED,
    MANUAL;

    /// > 是否使用 chamber（待击发）状态。OPEN 不使用。
    public boolean usesChamber() {
        return this != OPEN;
    }

    /// > 解析整枪的 bolt 类型。
    /// >
    /// > bolt 类型是武器的物理结构属性（决定弹药记账模型：是否有膛内 chambered 位），
    /// > 必须整枪唯一——不能随开火模式切换而变，否则切换火力模式会破坏 chambered 持久状态的一致性。
    /// > 因此读自 [GunData]，而非 [FireModeEntry]。
    /// > 显式 bolt_type 优先；缺省回退 OPEN（保持现有枪械行为不变）。
    public static BoltType resolve(@Nullable GunData data) {
        if (data == null) {
            return OPEN;
        }
        String type = data.getBoltType();
        if (type == null) {
            return OPEN;
        }
        return switch (type) {
            case "closed" -> CLOSED;
            case "manual" -> MANUAL;
            default -> OPEN;
        };
    }
}
