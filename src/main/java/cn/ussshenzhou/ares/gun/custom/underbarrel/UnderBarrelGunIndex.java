package cn.ussshenzhou.ares.gun.custom.underbarrel;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.resource.GunGsonHelper;
import cn.ussshenzhou.ares.gun.custom.CustomItemIndex;
import cn.ussshenzhou.ares.gun.custom.CustomItemType;
import cn.ussshenzhou.ares.gun.init.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.Map;

/// @author TACZ, USS_Shenzhou
public class UnderBarrelGunIndex extends CustomItemIndex<UnderBarrelGunData> {
    public static final CustomItemType<UnderBarrelGunIndex, UnderBarrelGunData> TYPE = new CustomItemType<>(
            Ares.modLoc("under_barrels"),
            UnderBarrelGunIndex::of,
            GunGsonHelper.GSON
    );

    public static UnderBarrelGunIndex of(
            Gson gson,
            Identifier id,
            JsonElement indexJson,
            Map<Identifier, JsonElement> dataJson,
            HolderLookup.Provider registryAccess
    ) throws JsonParseException, IllegalArgumentException {
        var index = gson.fromJson(indexJson, UnderBarrelGunIndex.class);
        if (index == null) {
            throw new JsonParseException("Failed to parse gun index: " + id);
        }
        if (index.dataLocation == null) {
            throw new JsonParseException("Missing data field: " + id);
        }
        var dataJsonElement = dataJson.get(index.dataLocation);
        if (dataJsonElement == null) {
            throw new JsonParseException("Gun data not found: " + index.dataLocation);
        }
        UnderBarrelGunData data = gson.fromJson(dataJsonElement, UnderBarrelGunData.class);
        if (data == null) {
            throw new JsonParseException("Failed to parse gun data: " + index.dataLocation);
        }
        if (index.toggle != null) {
            throw new JsonParseException("toggle is not allowed on under-barrel index: " + id);
        }

        Item baseItem = getBaseItem(index.itemType, ModItems.UNDER_BARREL.get());

        index.bind(id, baseItem, Component.translatable(index.nameKey), data, 1);
        return index;
    }
}
