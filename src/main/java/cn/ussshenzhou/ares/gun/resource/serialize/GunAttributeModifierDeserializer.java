package cn.ussshenzhou.ares.gun.resource.serialize;

import com.google.gson.*;
import cn.ussshenzhou.ares.gun.attribute.GunAttributeModifier;
import net.minecraft.util.GsonHelper;

import java.lang.reflect.Type;

/// @author TACZ, USS_Shenzhou
public class GunAttributeModifierDeserializer implements JsonDeserializer<GunAttributeModifier> {
        @Override
        public GunAttributeModifier deserialize(JsonElement ele, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            /// > 仍然支持完整的 JSON object 形式
            if (ele.isJsonObject()) {
                 JsonObject obj = ele.getAsJsonObject();
                GunAttributeModifier.Type modType = GunAttributeModifier.Type.valueOf(
                        GsonHelper.getAsString(obj, "type").toUpperCase()
                );
                double amount = GsonHelper.getAsDouble(obj, "amount");
                if (modType == GunAttributeModifier.Type.MULTIPLIER && amount < 0) {
                    throw new JsonParseException("multiply_total cannot have negative value: " + amount);
                }
                return new GunAttributeModifier(modType, amount);
            }
            
            if (ele.isJsonPrimitive() && ele.getAsJsonPrimitive().isString()) {
                String s = ele.getAsString().trim();
                if (s.isEmpty()) {
                    throw new JsonParseException("Empty attribute modifier string");
                }

                GunAttributeModifier.Type modType;
                double amount;

                char first = s.charAt(0);
                if (first == 'x') {
                    String num = s.substring(1).trim();
                    try {
                        amount = Double.parseDouble(num);
                    } catch (NumberFormatException ex) {
                        throw new JsonParseException("Invalid multiply_total value: " + s);
                    }
                    if (amount < 0) {
                        throw new JsonParseException("multiply_total cannot be negative: " + s);
                    }
                    modType = GunAttributeModifier.Type.MULTIPLIER;
                } else {
                    /// > addition 或 multiply_base（百分比）
                    boolean percent = s.endsWith("%");
                    double val = getVal(percent, s);
                    amount = percent ? val / 100.0 : val;
                    modType = percent ? GunAttributeModifier.Type.PERCENT : GunAttributeModifier.Type.ADDEND;
                }

                return new GunAttributeModifier(modType, amount);
            }

            throw new JsonParseException("Attribute modifier must be a JSON object or a shorthand string");
        }

        private double getVal(boolean percent, String s) {
            String num = percent ? s.substring(0, s.length() - 1).trim() : s;
            if (num.isEmpty()) {
                throw new JsonParseException("Invalid attribute modifier value: " + s);
            }
            char sign = num.charAt(0);
            if (sign != '+' && sign != '-') {
                throw new JsonParseException("String modifier must start with + or -: " + s);
            }
            double val;
            try {
                val = Double.parseDouble(num);
            } catch (NumberFormatException ex) {
                throw new JsonParseException("Invalid numeric value: " + s);
            }
            return val;
        }
    }