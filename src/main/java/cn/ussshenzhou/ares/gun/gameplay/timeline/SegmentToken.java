package cn.ussshenzhou.ares.gun.gameplay.timeline;

import javax.annotation.Nullable;

/// > 片段类型 token：同时承载稳定 id 与运行时类型信息，避免按 String 取回后再手写 instanceof。
/// @author TACZ, USS_Shenzhou
public class SegmentToken<T extends ActionSegment> {

    private final String id;
    private final Class<T> type;

    private SegmentToken(String id, Class<T> type) {
        this.id = id;
        this.type = type;
    }

    public static <T extends ActionSegment> SegmentToken<T> of(String id, Class<T> type) {
        return new SegmentToken<>(id, type);
    }

    public String id() {
        return id;
    }

    public Class<T> type() {
        return type;
    }

    @Nullable
    public T cast(@Nullable ActionSegment segment) {
        return type.isInstance(segment) ? type.cast(segment) : null;
    }
}
