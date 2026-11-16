package cn.ussshenzhou.ares.gun.attribute;

import cn.ussshenzhou.ares.Ares;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

/// > 属性 id 解析（数据侧与脚本侧共用）。
/// >
/// > 缺省 namespace 补 `tacz:`：数据包与脚本里的属性键通常写作短名（如 `damage`），
/// > 与 `gun.has_attachment('tacz:scope')` 等既有约定保持一致。
/// @author TACZ, USS_Shenzhou
public class AttributeIdHelper {
    public static final String DEFAULT_NAMESPACE = Ares.MOD_ID;

    /// > 解析属性 id。空、空白或非法返回 null。
    @Nullable
    public static Identifier tryParse(@Nullable String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        String normalized = rawId.contains(":") ? rawId : DEFAULT_NAMESPACE + ":" + rawId;
        return Identifier.tryParse(normalized);
    }
}
