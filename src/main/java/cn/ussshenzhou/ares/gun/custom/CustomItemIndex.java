package cn.ussshenzhou.ares.gun.custom;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.custom.attachment.ToggleConfig;
import com.google.gson.annotations.SerializedName;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Comparator;

/// > 数据驱动物品的动态注册表对象
/// >
/// > 一般来说，你不应该直接缓存此对象，而是通过相关方法从ItemStack获取，因为index会在重载时被替换
/// >
/// > @param `D` 此物品index的自定义数据
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW merged with IndexPojo: name, tooltip, display, data, type, item_type, sort and toggle are read from JSON directly.
// The former constructor became bind(...), which fills id, base item, display name, data and stack size after reading.
// ForgeRegistries.ITEMS replaced with BuiltInRegistries.ITEM.
public abstract class CustomItemIndex<D> {
    public static Identifier EMPTY = Ares.modLoc("empty");
    public static Comparator<CustomItemIndex<?>> ID_NAME_SORT = Comparator.comparingInt(CustomItemIndex::getSort);

    @SerializedName("name")
    protected String nameKey;

    @Nullable
    @SerializedName("tooltip")
    private String tooltip;

    @SerializedName("display")
    protected Identifier displayLocation;

    @SerializedName("data")
    protected Identifier dataLocation;

    @SerializedName("type")
    protected String type;

    /// > 物品基类
    @SerializedName("item_type")
    protected String itemType;

    @SerializedName("sort")
    protected int sort;

    /// > 基础形态的开关能力（仅配件 index 消费；枪/下挂声明会触发数据校验错误）。
    @Nullable
    @SerializedName("toggle")
    protected ToggleConfig toggle;

    protected transient Identifier id;
    protected transient Item baseItem;
    protected transient Component name;
    protected transient int stackSize;
    private transient D data;

    protected void bind(Identifier id, Item baseItem, Component name, D data, int stackSize) {
        this.id = id;
        this.baseItem = baseItem;
        this.name = name;
        this.data = data;
        this.stackSize = stackSize;
    }

    /// > 获取此index的注册ID
    /// >
    /// > @return 物品的注册ID
    @Nonnull
    public Identifier getId() {
        return id == null ? EMPTY : id;
    }

    public ItemStack createStack() {
        return createStack(getStackSize());
    }

    public ItemStack createStack(int count) {
        return new ItemStack(baseItem, count);
    }

    public Item getBaseItem() {
        return baseItem;
    }

    public Component getName() {
        return name;
    }

    public Identifier getDisplayLocation() {
        return displayLocation;
    }

    public int getStackSize() {
        return stackSize;
    }

    /// > 获取排序值，越小越靠前
    public int getSort() {
        return sort;
    }

    /// > 获取物品的tooltip
    /// >
    /// > @return tooltip的key，如果为null则不显示
    @Nullable
    public String getTooltip() {
        return tooltip;
    }

    /// > 获取物品的自定义数据值
    /// >
    /// > @return 物品的自定义数据值，如果为null则未加载或不存在
    @Nullable
    public D getData() {
        return data;
    }

    /// > 根据itemType字符串获取基础物品(命名空间按tacz补全)
    /// >
    /// > @param itemType 物品类型字符串
    /// >
    /// > @param fallback 获取失败时的回退物品
    /// >
    /// > @return 基础物品
    @Nullable
    public static Item getBaseItem(String itemType, Item fallback) {
        if (itemType != null) {
            if (!itemType.contains(":")) {
                itemType = Ares.MOD_ID + ":" + itemType;
            }
            Identifier itemTypeId = Identifier.tryParse(itemType);
            if (itemTypeId != null) {
                var type = BuiltInRegistries.ITEM.getValue(itemTypeId);
                if (type != Items.AIR) {
                    return type;
                }
            }
        }
        return fallback;
    }
}
