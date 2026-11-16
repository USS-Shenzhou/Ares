package cn.ussshenzhou.ares.gun.attribute;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW merged with GunAttributePojo: fromJson reads the index file into the record directly.
public record GunAttribute(
        Identifier id,
        Component name,
        double defaultValue,
        double minValue,
        double maxValue,
        double referenceValue,
        boolean positive,
        GunAttributeFormatter formatter
) {
    /// > 由注册项构造属性定义。**id 来自注册路径**（注册项内不再声明 id），
    /// > 展示名缺省按 `attribute.<namespace>.<path>` 推导。
    public static GunAttribute fromJson(Identifier id, JsonObject json, Gson gson) {
        String nameKey = GsonHelper.getAsString(json, "name", "attribute." + id.getNamespace() + "." + id.getPath());
        GunAttributeFormatter formatter = json.has("formatter")
                ? gson.fromJson(json.get("formatter"), GunAttributeFormatter.class)
                : GunAttributeFormatter.NORMAL;
        return new GunAttribute(
                id,
                Component.translatable(nameKey),
                GsonHelper.getAsDouble(json, "default", 0),
                GsonHelper.getAsDouble(json, "min", 0),
                GsonHelper.getAsDouble(json, "max", 0),
                GsonHelper.getAsDouble(json, "reference", 0),
                GsonHelper.getAsBoolean(json, "positive", false),
                formatter
        );
    }

    public double clamp(double value) {
        return Double.isNaN(value) ? this.minValue : Mth.clamp(value, this.minValue, this.maxValue);
    }

    public Component getDescription() {
        return name;
    }

}
