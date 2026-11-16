package cn.ussshenzhou.ares.gun.client.resource.display;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

/// > 枪口火光/枪烟配置。
/// >
/// > 当前粒子由动画 `particle_effects` 关键帧驱动并指定 effect 与 locator，本配置仅在动画
/// > 关键帧未指定 locator 时提供「回退骨骼名」。`texture`/`scale`/`duration` 为
/// > 早期 quad-billboard 实现遗留字段，保留以兼容旧 json，当前不再使用。
/// @author TACZ, USS_Shenzhou
public class MuzzleFlash {
    @Deprecated
    @Nullable
    @SerializedName("texture")
    public Identifier texture;

    /// > 粒子发射点回退骨骼名。动画关键帧未指定 locator 时使用。
    @Nullable
    @SerializedName("bone")
    public String bone = "muzzle_flash";

    @Deprecated
    @SerializedName("scale")
    public float scale = 1.0f;

    @Deprecated
    @SerializedName("duration")
    public int duration = 2;
}
