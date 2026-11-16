package cn.ussshenzhou.ares.gun.gameplay;

import net.minecraft.network.FriendlyByteBuf;

/// > 一次发射所使用的枪口来源。
/// >
/// > @param underBarrel 是否来自当前宿主枪唯一的下挂武器
/// >
/// > @param muzzleIndex display 有序枪口表中的序号
/// @author TACZ, USS_Shenzhou
public record MuzzleReference(boolean underBarrel, int muzzleIndex) {
    public MuzzleReference {
        if (muzzleIndex < 0) {
            throw new IllegalArgumentException("muzzleIndex must be non-negative");
        }
    }

    public static MuzzleReference primary(int muzzleIndex) {
        return new MuzzleReference(false, muzzleIndex);
    }

    public static MuzzleReference underBarrel(int muzzleIndex) {
        return new MuzzleReference(true, muzzleIndex);
    }

    public static MuzzleReference primary() {
        return primary(0);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(underBarrel);
        buffer.writeVarInt(muzzleIndex);
    }

    public static MuzzleReference read(FriendlyByteBuf buffer) {
        return new MuzzleReference(buffer.readBoolean(), buffer.readVarInt());
    }
}
