package cn.ussshenzhou.ares.gun.custom.attachment;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

/// > 形态的开关能力配置（可开关配件 = 形态声明了 toggle）。
/// >
/// > 配件 index 与变体 index 均可声明，分别代表「基础形态」与「该变体形态」的开关能力。
/// > `toggle` 只引用 data/display 资源文件（单向引用），不引用其它形态 id，与形态选择正交。
/// > - `effect`：特殊效果种类（laser / flashlight），仅开关开启时生效；空表示纯数据/几何开关。
/// > - `defaultEnabled`：NBT 缺省时的初始态，缺省 true。
/// > - `offData`：关闭态使用的完整附件数据文件引用；缺省回退形态本身的 data。
/// > - `offDisplay`：关闭态使用的几何 display 引用（与变体 display 同构）；缺省回退形态本身的几何。
///
/// @author TACZ, USS_Shenzhou
public class ToggleConfig {
    @Nullable
    @SerializedName("effect")
    private String effect;

    @SerializedName("default_enabled")
    private boolean defaultEnabled = true;

    @Nullable
    @SerializedName("off_data")
    private Identifier offData;

    @Nullable
    @SerializedName("off_display")
    private Identifier offDisplay;

    @Nullable
    public String getEffect() {
        return effect;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    @Nullable
    public Identifier getOffData() {
        return offData;
    }

    @Nullable
    public Identifier getOffDisplay() {
        return offDisplay;
    }
}