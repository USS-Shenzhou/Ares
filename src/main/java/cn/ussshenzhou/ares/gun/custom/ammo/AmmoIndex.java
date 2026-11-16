package cn.ussshenzhou.ares.gun.custom.ammo;

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
public class AmmoIndex extends CustomItemIndex<AmmoData> {
    public static final CustomItemType<AmmoIndex, AmmoData> TYPE = new CustomItemType<>(
            Ares.modLoc("ammo"),
            AmmoIndex::of,
            GunGsonHelper.GSON
    );

    public static AmmoIndex of(
            Gson gson,
            Identifier id,
            JsonElement indexJson,
            Map<Identifier, JsonElement> dataJson,
            HolderLookup.Provider registryAccess
    ) throws JsonParseException, IllegalArgumentException {
        var index = gson.fromJson(indexJson, AmmoIndex.class);
        if (index == null) {
            throw new JsonParseException("Failed to parse ammo index: " + id);
        }
        if (index.dataLocation == null) {
            throw new JsonParseException("Missing data field: " + id);
        }
        var dataJsonElement = dataJson.get(index.dataLocation);
        if (dataJsonElement == null) {
            throw new JsonParseException("Ammo data not found: " + index.dataLocation);
        }
        AmmoData data = gson.fromJson(dataJsonElement, AmmoData.class);
        if (data == null) {
            throw new JsonParseException("Failed to parse ammo data: " + index.dataLocation);
        }

        Item baseItem = getBaseItem(index.itemType, ModItems.AMMO.get());
        int stackSize = data.getStackSize();

        index.bind(id, baseItem, Component.translatable(index.nameKey), data, stackSize);
        return index;
    }
}
