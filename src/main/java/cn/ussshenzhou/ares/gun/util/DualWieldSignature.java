package cn.ussshenzhou.ares.gun.util;

import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

/// @author TACZ, USS_Shenzhou
public record DualWieldSignature(
        boolean active,
        @Nullable Identifier mainGunId,
        @Nullable Identifier offGunId,
        boolean mainOneHanded,
        boolean offOneHanded,
        boolean offhandUsable
) {
    public static DualWieldSignature inactive() {
        return new DualWieldSignature(false, null, null, false, false, false);
    }
}
