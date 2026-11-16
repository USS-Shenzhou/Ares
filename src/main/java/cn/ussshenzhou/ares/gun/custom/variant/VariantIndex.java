package cn.ussshenzhou.ares.gun.custom.variant;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.resource.GunGsonHelper;
import cn.ussshenzhou.ares.gun.custom.CustomItemIndex;
import cn.ussshenzhou.ares.gun.custom.CustomItemType;
import cn.ussshenzhou.ares.gun.custom.attachment.AttachmentData;
import cn.ussshenzhou.ares.gun.custom.attachment.ToggleConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// > 配件变体的数据驱动 index。
/// > <p>
/// > 变体不是独立物品（不会被 createStack），其作用是为已存在的配件提供可切换的几何/挂点中间层。
/// > 因此 baseItem 仅占位，{@code targets} 决定本变体挂载到哪些配件上。
/// > <p>
/// > 变体形态可选声明 {@code toggle}（该形态的开关能力）；关态使用的完整附件数据
/// > {@code offData} 从本变体数据域（data/attachment_variants）解析，格式与附件 data 一致。
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW also merged the targets field of VariantIndexPojo.
public class VariantIndex extends CustomItemIndex<VariantData> {
    public static final CustomItemType<VariantIndex, VariantData> TYPE = new CustomItemType<>(
            Ares.modLoc("attachment_variants"),
            VariantIndex::of,
            GunGsonHelper.GSON
    );

    /// > {@code targets} 声明本变体适用于哪些配件（按配件注册 id 定向），支持跨包扩展：
    /// > 第三方包无需改动原配件 data，只需新增一个变体 index 文件即可为其挂载变体。
    /// >
    /// > 本变体适用的配件 id 列表（定向目标）。
    @SerializedName("targets")
    private List<Identifier> targets = new ArrayList<>();

    /// > 本变体形态关闭态使用的完整附件数据（{@link ToggleConfig#getOffData()} 解析结果）；
    /// > 声明了 off_data 但解析失败时回退配件基础 data（null），由数据校验输出错误提示。
    @Nullable
    private transient AttachmentData offData;

    public static VariantIndex of(
            Gson gson,
            Identifier id,
            JsonElement indexJson,
            Map<Identifier, JsonElement> dataJson,
            HolderLookup.Provider registryAccess
    ) throws JsonParseException, IllegalArgumentException {
        var index = gson.fromJson(indexJson, VariantIndex.class);
        if (index == null) {
            throw new JsonParseException("Failed to parse variant index: " + id);
        }
        if (index.dataLocation == null) {
            throw new JsonParseException("Missing data field: " + id);
        }
        var dataJsonElement = dataJson.get(index.dataLocation);
        if (dataJsonElement == null) {
            throw new JsonParseException("Variant data not found: " + index.dataLocation);
        }
        VariantData data = gson.fromJson(dataJsonElement, VariantData.class);
        if (data == null) {
            throw new JsonParseException("Failed to parse variant data: " + index.dataLocation);
        }

        ToggleConfig toggle = index.toggle;
        AttachmentData offData = null;
        if (toggle != null && toggle.getOffData() != null) {
            JsonElement offJson = dataJson.get(toggle.getOffData());
            if (offJson != null) {
                offData = gson.fromJson(offJson, AttachmentData.class);
            }
        }

        index.bind(
                id,
                Items.AIR,
                index.nameKey == null ? Component.empty() : Component.translatable(index.nameKey),
                data,
                1
        );
        index.targets = index.targets == null ? List.of() : List.copyOf(index.targets);
        index.offData = offData;
        return index;
    }

    /// > 本变体适用的配件 id 列表。
    public List<Identifier> getTargets() {
        return targets;
    }

    /// > 本变体是否适用于给定配件 id。
    public boolean appliesTo(Identifier attachmentId) {
        return targets.contains(attachmentId);
    }

    /// > 本变体形态是否可开关（声明了 toggle）。
    public boolean isToggleable() {
        return toggle != null;
    }

    /// > 本变体形态的开关能力配置；不可开关返回 null。
    @Nullable
    public ToggleConfig getToggle() {
        return toggle;
    }

    /// > 本变体形态关闭态使用的完整附件数据；未声明或解析失败返回 null（调用方回退配件基础 data）。
    @Nullable
    public AttachmentData getOffData() {
        return offData;
    }
}
