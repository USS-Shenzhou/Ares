package cn.ussshenzhou.ares.gun.item;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/// > 配件槽位路径。
/// >
/// > 从枪械顶层槽位 id 开始，逐层指向多层配件树中的目标槽位。
/// > 例如 `[tacz:kit_barrel, tacz:muzzle]` 表示「枪的 kit_barrel 槽 → 该配件的 muzzle 子槽」。
/// >
/// > 路径长度即为目标槽位所处的深度，受 [IAttachmentHolder#MAX_ATTACHMENT_DEPTH] 约束。
/// @author TACZ, USS_Shenzhou
public record AttachmentPath(List<Identifier> slots) {
    public static final AttachmentPath EMPTY = new AttachmentPath(List.of());

    public AttachmentPath {
        slots = List.copyOf(slots);
    }

    public static AttachmentPath of(Identifier... slots) {
        return new AttachmentPath(List.of(slots));
    }

    public boolean isEmpty() {
        return slots.isEmpty();
    }

    public int depth() {
        return slots.size();
    }

    /// > 返回末端槽位 id；空路径返回 null。
    public Identifier last() {
        return slots.isEmpty() ? null : slots.get(slots.size() - 1);
    }

    /// > 在末端追加一个槽位，返回新路径（不修改原对象）。
    @Nonnull
    public AttachmentPath append(Identifier slotId) {
        List<Identifier> next = new ArrayList<>(slots);
        next.add(slotId);
        return new AttachmentPath(next);
    }

    /// > 去掉末端槽位，返回父路径；空路径返回自身。
    @Nonnull
    public AttachmentPath parent() {
        if (slots.isEmpty()) {
            return this;
        }
        return new AttachmentPath(slots.subList(0, slots.size() - 1));
    }

    @Nonnull
    public List<Identifier> slots() {
        return Collections.unmodifiableList(slots);
    }

    // FIXME REVIEW 26.3 FriendlyByteBuf has no writeCollection/readList; encode and decode write the size and then each
    // identifier.
    public static void encode(AttachmentPath path, FriendlyByteBuf buf) {
        buf.writeVarInt(path.slots.size());
        for (Identifier slot : path.slots) {
            buf.writeIdentifier(slot);
        }
    }

    public static AttachmentPath decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Identifier> slots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            slots.add(buf.readIdentifier());
        }
        return new AttachmentPath(slots);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AttachmentPath that)) {
            return false;
        }
        return slots.equals(that.slots);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(slots);
    }
}
