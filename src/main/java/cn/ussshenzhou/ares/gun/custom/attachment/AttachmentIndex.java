package cn.ussshenzhou.ares.gun.custom.attachment;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.TimelessHelperS;
import cn.ussshenzhou.ares.gun.item.AttachmentType;
import cn.ussshenzhou.ares.gun.resource.GunGsonHelper;
import cn.ussshenzhou.ares.gun.custom.CustomItemIndex;
import cn.ussshenzhou.ares.gun.custom.CustomItemType;
import cn.ussshenzhou.ares.gun.init.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import javax.annotation.Nullable;

import java.util.Map;

/// @author TACZ, USS_Shenzhou
public class AttachmentIndex extends CustomItemIndex<AttachmentData> {
    public static final CustomItemType<AttachmentIndex, AttachmentData> TYPE = new CustomItemType<>(
            Ares.modLoc("attachments"),
            AttachmentIndex::of,
            GunGsonHelper.GSON
    );

    private transient AttachmentType attachmentType;

    /// > 基础形态关闭态使用的完整附件数据（[ToggleConfig#getOffData()] 解析结果）；
    /// > 声明了 off_data 但解析失败时回退基础 data（null），由数据校验输出错误提示。
    @Nullable
    private transient AttachmentData offData;

    public static AttachmentIndex of(
            Gson gson,
            Identifier id,
            JsonElement indexJson,
            Map<Identifier, JsonElement> dataJson,
            HolderLookup.Provider registryAccess
    ) throws JsonParseException, IllegalArgumentException {
        var index = gson.fromJson(indexJson, AttachmentIndex.class);
        if (index == null) {
            throw new JsonParseException("Failed to parse gun index: " + id);
        }
        if (index.dataLocation == null) {
            throw new JsonParseException("Missing data field: " + id);
        }
        var dataJsonElement = dataJson.get(index.dataLocation);
        if (dataJsonElement == null) {
            throw new JsonParseException("Attachment data not found: " + index.dataLocation);
        }
        AttachmentData data = gson.fromJson(dataJsonElement, AttachmentData.class);
        if (data == null) {
            throw new JsonParseException("Failed to parse gun data: " + index.dataLocation);
        }

        Item baseItem = getBaseItem(index.itemType, ModItems.ATTACHMENT.get());

        String rawAttachmentType = index.type;
        if (rawAttachmentType == null || rawAttachmentType.isEmpty()) {
            throw new JsonParseException("Attachment type is missing: " + id);
        }
        if (!rawAttachmentType.contains(":")) {
            rawAttachmentType = Ares.MOD_ID + ":" + rawAttachmentType;
        }
        Identifier typeId = Identifier.parse(rawAttachmentType);
        var attachmentType = TimelessHelperS.getAttachmentType(typeId);
        if (attachmentType == null) {
            throw new JsonParseException("Unknown attachment type: " + typeId);
        }

        ToggleConfig toggle = index.toggle;
        AttachmentData offData = null;
        if (toggle != null && toggle.getOffData() != null) {
            JsonElement offJson = dataJson.get(toggle.getOffData());
            if (offJson != null) {
                offData = gson.fromJson(offJson, AttachmentData.class);
            }
        }

        index.bind(id, baseItem, Component.translatable(index.nameKey), data, 1);
        index.attachmentType = attachmentType;
        index.offData = offData;
        return index;
    }

    public AttachmentType getAttachmentType() {
        return attachmentType;
    }

    /// > 基础形态是否可开关（声明了 toggle）。
    public boolean isToggleable() {
        return toggle != null;
    }

    /// > 基础形态的开关能力配置；不可开关返回 null。
    @Nullable
    public ToggleConfig getToggle() {
        return toggle;
    }

    /// > 基础形态关闭态使用的完整附件数据；未声明或解析失败返回 null（调用方回退基础 data）。
    @Nullable
    public AttachmentData getOffData() {
        return offData;
    }

    /// > 按开关状态取基础形态的生效数据：关闭且声明了 off_data 时返回关态数据，否则返回基础 data。
    public AttachmentData getDataFor(boolean enabled) {
        if (!enabled && offData != null) {
            return offData;
        }
        return getData();
    }
}
