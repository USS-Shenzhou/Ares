package cn.ussshenzhou.ares.gun.resource;

import net.minecraft.resources.Identifier;

import java.util.Set;

/// > 数据驱动索引标签的只读查询接口。
/// @author TACZ, USS_Shenzhou
public interface IndexTagLookup {
    IndexTagLookup EMPTY = new IndexTagLookup() {
        @Override
        public boolean contains(Identifier tagId, Identifier memberId) {
            return false;
        }

        @Override
        public boolean isKnown(Identifier tagId) {
            return false;
        }

        @Override
        public boolean isValid(Identifier tagId) {
            return false;
        }

        @Override
        public Set<Identifier> getMembers(Identifier tagId) {
            return Set.of();
        }

        @Override
        public Set<Identifier> getTags(Identifier memberId) {
            return Set.of();
        }
    };

    /// > 指定成员是否属于指定标签。
    boolean contains(Identifier tagId, Identifier memberId);

    /// > 标签是否在当前数据中声明过（包含无效标签）。
    boolean isKnown(Identifier tagId);

    /// > 标签是否有效且可用于匹配。
    boolean isValid(Identifier tagId);

    /// > 获取已展开的成员集合；未知或无效标签返回空集合。
    Set<Identifier> getMembers(Identifier tagId);

    /// > 获取包含指定成员的全部有效标签（包含经由 {@code #tag} 展开的间接归属）。
    Set<Identifier> getTags(Identifier memberId);
}
