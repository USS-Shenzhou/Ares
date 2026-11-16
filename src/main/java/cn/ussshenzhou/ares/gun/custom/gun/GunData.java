package cn.ussshenzhou.ares.gun.custom.gun;

import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.JsonAdapter;
import cn.ussshenzhou.ares.gun.custom.AttachmentSlotInfo;
import cn.ussshenzhou.ares.gun.resource.serialize.AttributeIdMapDeserializer;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW removed fields: projectile_type, explosion, laser, reload_plan_script, fire_plan_script, hooks, one_handed.
public class GunData {
    @SerializedName("attributes")
    @JsonAdapter(AttributeIdMapDeserializer.class)
    private Map<Identifier, Double> baseAttributes = new HashMap<>();

    @SerializedName("attachment_slots")
    private AttachmentSlotInfo attachmentSlots = new AttachmentSlotInfo();

    @SerializedName("fire_modes")
    private List<FireModeEntry> fireModes = List.of();

    @SerializedName("magazine_capacity")
    private int magazineCapacity = 30;

    @SerializedName("reload_duration")
    private int reloadDuration = 40;

    /// > 按弹匣扩容等级（{@code magLevel}，0 起）区分的换弹总时长（tick）。
    /// > 声明后弹匣换弹按当前扩容等级取对应时长，缺省 / 越界回退 {@link #reloadDuration}。
    @Nullable
    @SerializedName("reload_duration_by_level")
    private int[] reloadDurationByLevel;

    @SerializedName("bullet_gravity")
    private float bulletGravity = 0.05f;

    @SerializedName("bullet_amount")
    private int bulletAmount = 1;

    @SerializedName("bullet_lifespan")
    private int bulletLifespan = 200;

    @Nullable
    @SerializedName("accepted_ammo")
    private Identifier acceptedAmmo;

    @SerializedName("reload_type")
    private String reloadType = "magazine";

    @Nullable
    @SerializedName("shell_reload")
    private ShellReloadTiming shellReload;

    @Nullable
    @SerializedName("bolt_type")
    private String boltType = null;

    @SerializedName("bolt_action_time")
    private int boltActionTime = 0;

    @SerializedName("put_away_time")
    private float putAwayTime = 0.5f;

    @SerializedName("draw_time")
    private float drawTime = 0.35f;

    @SerializedName("tracer_frequency")
    private int tracerFrequency = 0;

    @SerializedName("ammos")
    private List<AmmoEntry> ammos = List.of();

    @Nullable
    @SerializedName("damage_falloff")
    private List<DamageFalloffPoint> damageFalloff;

    @SerializedName("allow_aim_while_reloading")
    private boolean allowAimWhileReloading = true;

    public Map<Identifier, Double> getBaseAttributes() {
        return baseAttributes;
    }

    /// > 该属性是否在本枪数据里显式声明（不含修饰链贡献，也不含回落默认值）。
    public boolean hasAttribute(Identifier attribute) {
        return baseAttributes.containsKey(attribute);
    }

    public double getBaseAttribute(Identifier attribute) {
        return baseAttributes.get(attribute);
    }

    @Nullable
    public AttachmentSlotInfo getAttachmentSlots() {
        return attachmentSlots;
    }

    public List<FireModeEntry> getFireModes() {
        return fireModes;
    }

    public int getMagazineCapacity() {
        return magazineCapacity;
    }

    public int getReloadDuration() {
        return reloadDuration;
    }

    /// > 按弹匣扩容等级取换弹总时长（tick）。
    /// > 未声明 {@code reload_duration_by_level} 或数组为空时回退 {@link #getReloadDuration()}；
    /// > 等级越界（超出数组长度）时回退数组末元素。
    /// >
    /// > @param level 弹匣扩容等级（0 起），来自 {@code IGunProperties.getMagLevel()}
    public int getReloadDurationForLevel(int level) {
        if (reloadDurationByLevel == null || reloadDurationByLevel.length == 0) {
            return reloadDuration;
        }
        int index = Math.min(Math.max(level, 0), reloadDurationByLevel.length - 1);
        return reloadDurationByLevel[index];
    }

    public float getBulletGravity() {
        return bulletGravity;
    }

    public int getBulletAmount() {
        return bulletAmount;
    }

    public int getBulletLifespan() {
        return bulletLifespan;
    }

    @Nullable
    public Identifier getAcceptedAmmo() {
        return acceptedAmmo;
    }

    public String getReloadType() {
        return reloadType;
    }

    @Nullable
    public ShellReloadTiming getShellReload() {
        return shellReload;
    }

    /// > bolt 类型字符串：open / closed / manual。null 表示缺省（解析为 OPEN）。
    /// > 整枪唯一，不随开火模式切换而变。
    @Nullable
    public String getBoltType() {
        return boltType;
    }

    /// > 拉栓时长（tick，0=无拉栓窗口）。整枪级参数：栓动时间是武器物理结构特性，与开火模式无关。
    public int getBoltActionTime() {
        return boltActionTime;
    }

    public List<AmmoEntry> getAmmos() {
        return ammos;
    }

    public int getTracerFrequency() {
        return tracerFrequency;
    }

    @Nullable
    public List<DamageFalloffPoint> getDamageFalloff() {
        return damageFalloff;
    }

    public boolean isAllowAimWhileReloading() {
        return allowAimWhileReloading;
    }

    public float getPutAwayTime() {
        return putAwayTime;
    }

    public int getPutAwayTicks() {
        return Math.max(1, Math.round(putAwayTime * 20));
    }

    public float getDrawTime() {
        return drawTime;
    }

    public int getDrawTicks() {
        return Math.max(1, Math.round(drawTime * 20));
    }

    /// > 返回所有兼容弹种 ID 列表。
    /// > 合并 ammos 列表 + accepted_ammo 向后兼容字段。
    public List<Identifier> getAcceptedAmmoIds() {
        List<Identifier> result = new ArrayList<>();
        for (AmmoEntry entry : ammos) {
            if (entry.getType() != null) {
                result.add(entry.getType());
            }
        }
        if (result.isEmpty() && acceptedAmmo != null) {
            result.add(acceptedAmmo);
        }
        return result;
    }

    /// > 按弹种 ID 查找对应的 AmmoEntry。
    @Nullable
    public AmmoEntry getAmmoEntry(Identifier ammoId) {
        if (ammoId == null) {
            return null;
        }
        for (AmmoEntry entry : ammos) {
            if (ammoId.equals(entry.getType())) {
                return entry;
            }
        }
        return null;
    }

    public static class ShellReloadTiming {
        @SerializedName("intro")
        private int intro = 10;

        @SerializedName("intro_empty")
        private int introEmpty = -1;

        @SerializedName("loop")
        private int loop = 8;

        @SerializedName("end")
        private int end = 6;

        @SerializedName("fire_interrupts")
        private boolean fireInterrupts = true;

        @SerializedName("sprint_interrupts")
        private boolean sprintInterrupts = true;

        public int getIntro() {
            return intro;
        }

        public int getIntroEmpty() {
            return introEmpty > 0 ? introEmpty : intro;
        }

        public int getLoop() {
            return loop;
        }

        public int getEnd() {
            return end;
        }

        public boolean canFireInterrupt() {
            return fireInterrupts;
        }

        public boolean canSprintInterrupt() {
            return sprintInterrupts;
        }
    }

    /// > 距离衰减曲线上的一个点。
    /// > 弹丸飞行距离在相邻点之间线性插值。
    public static class DamageFalloffPoint {
        @SerializedName("distance")
        private float distance;

        @SerializedName("multiplier")
        private float multiplier = 1.0f;

        public float getDistance() {
            return distance;
        }

        public float getMultiplier() {
            return multiplier;
        }
    }
}
