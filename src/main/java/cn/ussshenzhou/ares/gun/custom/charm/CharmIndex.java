package cn.ussshenzhou.ares.gun.custom.charm;

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

/// > 挂饰的数据驱动索引（index/charms/*.json）。
/// >
/// > 接入标准 [CustomItemType] 管线：自动数据包加载、服务端→客户端同步、创造栏列出。
/// @author TACZ, USS_Shenzhou
public class CharmIndex extends CustomItemIndex<CharmData> {
    public static final CustomItemType<CharmIndex, CharmData> TYPE = new CustomItemType<>(
            Ares.modLoc("charms"),
            CharmIndex::of,
            GunGsonHelper.GSON
    );

    public static CharmIndex of(
            Gson gson,
            Identifier id,
            JsonElement indexJson,
            Map<Identifier, JsonElement> dataJson,
            HolderLookup.Provider registryAccess
    ) throws JsonParseException, IllegalArgumentException {
        var index = gson.fromJson(indexJson, CharmIndex.class);
        if (index == null) {
            throw new JsonParseException("Failed to parse charm index: " + id);
        }
        if (index.dataLocation == null) {
            throw new JsonParseException("Missing data field: " + id);
        }
        var dataJsonElement = dataJson.get(index.dataLocation);
        if (dataJsonElement == null) {
            throw new JsonParseException("Charm data not found: " + index.dataLocation);
        }
        CharmData data = gson.fromJson(dataJsonElement, CharmData.class);
        if (data == null) {
            throw new JsonParseException("Failed to parse charm data: " + index.dataLocation);
        }

        Item baseItem = getBaseItem(index.itemType, ModItems.CHARM.get());

        index.bind(id, baseItem, Component.translatable(index.nameKey), data, 16);
        return index;
    }
}
