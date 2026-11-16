package cn.ussshenzhou.ares.attachment.entity.gun;

import cn.ussshenzhou.ares.util.GunOperationSessionKind;

/// > 双手持枪形态：能力全开，属性 / 时长恒等（即现状行为）。
/// @author TACZ, USS_Shenzhou
public class TwoHandProfile implements WieldProfile {
    public static final TwoHandProfile INSTANCE = new TwoHandProfile();

    @Override
    public GunOperationSessionKind kind() {
        return GunOperationSessionKind.TWO_HAND;
    }

    @Override
    public boolean canAim() {
        return true;
    }

    @Override
    public boolean canInspect() {
        return true;
    }

    @Override
    public boolean canSwitchModule() {
        return true;
    }
}
