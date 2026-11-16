package cn.ussshenzhou.ares.gun.custom.slotadapter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.resource.GunGsonHelper;
import cn.ussshenzhou.ares.gun.custom.CustomItemIndex;
import cn.ussshenzhou.ares.gun.custom.CustomItemType;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Items;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// > 槽位适配器的数据驱动 index。
/// > <p>
/// > 适配器不是独立物品（不会被 createStack），其作用是为已存在的宿主槽位提供可切换的中间层。
/// > 因此 baseItem 仅占位，{@code targets} 决定本适配器挂载到哪些「宿主 + 槽位」上。
/// @author TACZ, USS_Shenzhou
public class SlotAdapterIndex extends CustomItemIndex<SlotAdapterData> {
    public static final CustomItemType<SlotAdapterIndex, SlotAdapterData> TYPE = new CustomItemType<>(
            Ares.modLoc("slot_adapters"),
            SlotAdapterIndex::of,
            GunGsonHelper.GSON
    );

    /// > {@code targets} 声明本适配器适用于哪些「宿主 + 槽位」组合，支持跨包扩展：
    /// > 第三方包无需改动原枪/配件 data，只需新增一个适配器 index 文件即可为某槽位挂载适配器。
    // FIXME REVIEW merged SlotAdapterIndexPojo. targets are still parsed by parseTargets, so the field is transient to keep Gson
    // from reading it again.
    private transient List<SlotAdapterTarget> targets;

    public static SlotAdapterIndex of(
            Gson gson,
            Identifier id,
            JsonElement indexJson,
            Map<Identifier, JsonElement> dataJson,
            HolderLookup.Provider registryAccess
    ) throws JsonParseException, IllegalArgumentException {
        if (!indexJson.isJsonObject()) {
            throw new JsonParseException("Invalid slot adapter index: " + id);
        }
        JsonObject indexObj = indexJson.getAsJsonObject();
        var index = gson.fromJson(indexJson, SlotAdapterIndex.class);
        if (index == null) {
            throw new JsonParseException("Failed to parse slot adapter index: " + id);
        }
        if (index.dataLocation == null) {
            throw new JsonParseException("Missing data field: " + id);
        }
        var dataJsonElement = dataJson.get(index.dataLocation);
        if (dataJsonElement == null) {
            throw new JsonParseException("Slot adapter data not found: " + index.dataLocation);
        }
        SlotAdapterData data = gson.fromJson(dataJsonElement, SlotAdapterData.class);
        if (data == null) {
            throw new JsonParseException("Failed to parse slot adapter data: " + index.dataLocation);
        }

        List<SlotAdapterTarget> targets = parseTargets(indexObj);

        index.bind(
                id,
                Items.AIR,
                index.nameKey == null ? Component.empty() : Component.translatable(index.nameKey),
                data,
                1
        );
        index.targets = targets;
        return index;
    }

    /// > 解析 {@code targets} 数组，每项形如 {@code {"host": "tacz:m4a1", "slot": "tacz:scope"}}。
    /// > host/slot 省略 namespace 时默认补 {@code tacz:}。
    private static List<SlotAdapterTarget> parseTargets(JsonObject indexObj) {
        List<SlotAdapterTarget> result = new ArrayList<>();
        if (!indexObj.has("targets") || !indexObj.get("targets").isJsonArray()) {
            return result;
        }
        JsonArray arr = indexObj.getAsJsonArray("targets");
        for (JsonElement ele : arr) {
            if (!ele.isJsonObject()) {
                throw new JsonParseException("Invalid slot adapter target: " + ele);
            }
            JsonObject obj = ele.getAsJsonObject();
            Identifier host = parseId(GsonHelper.getAsString(obj, "host"));
            Identifier slot = parseId(GsonHelper.getAsString(obj, "slot"));
            Identifier allowAttachmentTag = parseAllowAttachmentTag(obj);
            result.add(new SlotAdapterTarget(host, slot, allowAttachmentTag));
        }
        return result;
    }

    private static Identifier parseId(String raw) {
        if (raw != null && !raw.contains(":")) {
            raw = Ares.MOD_ID + ":" + raw;
        }
        Identifier id = Identifier.tryParse(raw);
        if (id == null) {
            throw new JsonParseException("Invalid slot adapter target id: " + raw);
        }
        return id;
    }

    @Nullable
    private static Identifier parseAllowAttachmentTag(JsonObject obj) {
        String raw = GsonHelper.getAsString(obj, "allow_attachment_tag", null);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        if (!raw.contains(":")) {
            throw new JsonParseException("Slot adapter target allow_attachment_tag must include namespace: " + raw);
        }
        Identifier id = Identifier.tryParse(raw);
        if (id == null) {
            throw new JsonParseException("Invalid slot adapter target allow_attachment_tag: " + raw);
        }
        return id;
    }

    public List<SlotAdapterTarget> getTargets() {
        return targets;
    }

    /// > 返回唯一匹配给定「宿主 + 槽位」的 target。不存在或数据中重复声明时返回 null，运行时失败关闭。
    @Nullable
    public SlotAdapterTarget getTarget(Identifier hostId, Identifier slotId) {
        SlotAdapterTarget found = null;
        for (SlotAdapterTarget target : targets) {
            if (!target.host().equals(hostId) || !target.slot().equals(slotId)) {
                continue;
            }
            if (found != null) {
                return null;
            }
            found = target;
        }
        return found;
    }

    /// > 本适配器是否适用于给定「宿主 + 槽位」组合。
    public boolean appliesTo(Identifier hostId, Identifier slotId) {
        return getTarget(hostId, slotId) != null;
    }
}
