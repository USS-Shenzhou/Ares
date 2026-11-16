package cn.ussshenzhou.ares.gun.custom;

import com.mojang.logging.LogUtils;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import cn.ussshenzhou.ares.gun.attribute.GunAttribute;
import cn.ussshenzhou.ares.gun.resource.GunGsonHelper;
import cn.ussshenzhou.ares.gun.resource.JsonDataManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import javax.annotation.Nullable;

import java.util.Map;

/// > 用于管理用户定义的自定义属性
/// @author TACZ, USS_Shenzhou
public class GunAttributeManager extends JsonDataManager<GunAttribute> {
    public GunAttributeManager() {
        super(GunAttribute.class, GunGsonHelper.GSON, "index/gun_attributes", "GunAttributeManager");
    }

    public GunAttribute getAttribute(Identifier id) {
        return this.getData(id);
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        ImmutableMap.Builder<Identifier, GunAttribute> builder = ImmutableMap.builder();
        for (var entry : pObject.entrySet()) {
            Identifier id = entry.getKey();
            JsonElement json = entry.getValue();
            try {
                GunAttribute attribute = this.getGson().fromJson(json, GunAttribute.class);
                if (attribute != null) {
                    String error = validateDomain(attribute);
                    if (error != null) {
                        LogUtils.getLogger().error("Invalid GunAttribute {}: {}", id, error);
                        continue;
                    }
                    toAttribute(id, attribute);
                    builder.put(id, attribute);
                }
            } catch (Exception e) {
                LogUtils.getLogger().error("Failed to load GunAttribute: {}", id, e);
            }
        }
        this.dataMap = builder.build();
    }

    /// > 由注册项构造属性定义。**id 来自注册路径**（注册项内不再声明 id），
    /// > 展示名缺省按 `attribute.<namespace>.<path>` 推导。
    private static void toAttribute(Identifier id, GunAttribute attribute) {
        String name = attribute.nameKey() != null ? attribute.nameKey() : "attribute." + id.getNamespace() + "." + id.getPath();
        attribute.bind(id, Component.translatable(name));
    }

    /// > 校验注册项数值域：`min <= max`，且 `default` 落在域内。
    /// >
    /// > @return 错误说明；合法返回 null
    @Nullable
    private static String validateDomain(GunAttribute attribute) {
        if (attribute.minValue() > attribute.maxValue()) {
            return "min > max (" + attribute.minValue() + " > " + attribute.maxValue() + ")";
        }
        if (attribute.defaultValue() < attribute.minValue() || attribute.defaultValue() > attribute.maxValue()) {
            return "default out of range (" + attribute.defaultValue() + " not in ["
                    + attribute.minValue() + ", " + attribute.maxValue() + "])";
        }
        return null;
    }
}
