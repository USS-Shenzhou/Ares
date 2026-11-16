package cn.ussshenzhou.ares.gun.item;

import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

/// > 配件兼容性结果；失败原因供服务端拒绝、客户端筛选和后续提示复用。
/// @author TACZ, USS_Shenzhou
public record AttachmentCompatibilityResult(
        Failure failure,
        @Nullable Identifier rootTag
) {
    public enum Failure {
        NONE,
        INVALID_CANDIDATE,
        INVALID_ADAPTER,
        STRUCTURE_NOT_ALLOWED,
        TYPE_NOT_ALLOWED,
        ROOT_TAG_INVALID,
        ROOT_TAG_NOT_MATCHED,
        DEPTH_EXCEEDED
    }

    public boolean compatible() {
        return failure == Failure.NONE;
    }

    public static AttachmentCompatibilityResult success(Identifier rootTag) {
        return new AttachmentCompatibilityResult(Failure.NONE, rootTag);
    }

    public static AttachmentCompatibilityResult failure(Failure failure, @Nullable Identifier rootTag) {
        return new AttachmentCompatibilityResult(failure, rootTag);
    }
}
