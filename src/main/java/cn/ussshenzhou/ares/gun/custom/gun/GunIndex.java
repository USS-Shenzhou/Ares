package cn.ussshenzhou.ares.gun.custom.gun;

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
public class GunIndex extends CustomItemIndex<GunData> {
    public static final CustomItemType<GunIndex, GunData> TYPE = new CustomItemType<>(
            Ares.modLoc("guns"),
            GunIndex::of,
            GunGsonHelper.GSON
    );

    // FIXME REVIEW of() reads the index JSON directly into this class and then calls bind(); AmmoIndex, AttachmentIndex,
    // CharmIndex, UnderBarrelGunIndex, VariantIndex and SlotAdapterIndex do the same. Validation and error messages are
    // unchanged.
    public static GunIndex of(
            Gson gson,
            Identifier id,
            JsonElement indexJson,
            Map<Identifier, JsonElement> dataJson,
            HolderLookup.Provider registryAccess
    ) throws JsonParseException, IllegalArgumentException {
        var index = gson.fromJson(indexJson, GunIndex.class);
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
        GunData data = gson.fromJson(dataJsonElement, GunData.class);
        if (data == null) {
            throw new JsonParseException("Failed to parse gun data: " + index.dataLocation);
        }
        if (index.toggle != null) {
            throw new JsonParseException("toggle is not allowed on gun index: " + id);
        }

        Item baseItem = getBaseItem(index.itemType, ModItems.GUN.get());

        index.bind(id, baseItem, Component.translatable(index.nameKey), data, 1);
        return index;
    }
}
