package cn.ussshenzhou.ares.attachment.entity.gun;

import cn.ussshenzhou.ares.util.GunOperationSessionKind;

/// > 单手双持形态：禁用瞄准 / 检视 / 模块模式切换。
/// >
/// > 形态相关的属性修饰与时长调整（如单手降低部分属性、改变掏枪 / 换弹时长）后续在此填充，
/// > 当前先恒等（沿用基础值），结构就位。
/// @author TACZ, USS_Shenzhou
public class OneHandProfile implements WieldProfile {
    public static final OneHandProfile INSTANCE = new OneHandProfile();

    @Override
    public GunOperationSessionKind kind() {
        return GunOperationSessionKind.ONE_HAND;
    }

    @Override
    public boolean canAim() {
        return false;
    }

    @Override
    public boolean canInspect() {
        return false;
    }

    @Override
    public boolean canSwitchModule() {
        return false;
    }
}
