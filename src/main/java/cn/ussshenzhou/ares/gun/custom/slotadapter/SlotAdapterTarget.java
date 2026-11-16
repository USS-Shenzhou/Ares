package cn.ussshenzhou.ares.gun.custom.slotadapter;

import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

/// > 槽位适配器的定向目标：宿主 id（枪或父配件）+ 槽位 id。
/// > <p>
/// > 例如 {@code {host: tacz:m4a1, slot: tacz:scope}} 表示「m4a1 的 scope 槽可用本适配器」。
/// @author TACZ, USS_Shenzhou
public record SlotAdapterTarget(
        Identifier host,
        Identifier slot,
        @Nullable Identifier allowAttachmentTag
) {
}
