package cn.ussshenzhou.ares.gun.gameplay.timeline.segment;

import cn.ussshenzhou.ares.attachment.entity.GunHolderState;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionGate;
import cn.ussshenzhou.ares.gun.gameplay.timeline.ActionSegment;
import cn.ussshenzhou.ares.gun.gameplay.timeline.CooldownProvider;
import cn.ussshenzhou.ares.gun.gameplay.timeline.SegmentSet;
import cn.ussshenzhou.ares.gun.gameplay.timeline.SegmentToken;
import javax.annotation.Nullable;

/// > RPM 射击冷却片段（仅客户端表现）。
/// > <p>
/// > 以<b>墙上时钟</b>判定（{@code now >= readyAtMs} 才放行），给出亚刻（sub-tick）精度的开火节流。
/// > 连射由渲染帧驱动 {@code fire} 轮询，本片段据墙上时钟即时放行，避免把判定量化到游戏刻（50ms）
/// > 边界导致的射速抖动。冷却期投影 {@link GunHolderState#FIRE_COOLDOWN} 并收紧开火门禁。
/// > <p>
/// > 节奏锚点 {@code readyAtMs} 在出膛时步进（{@code += cooldownMs}）以吃掉 tick 量化余数，
/// > 保证连射节奏均匀；间隔过大（首发 / 长时间未射）时以当前时刻重新起锚。
/// > <p>
/// > <b>注意</b>：服务端不使用本片段。服务端的射速门控是一种容差性质的额外门控（credit-pool 缓冲，
/// > 宽容地吃掉网络抖动与 tick 量化），与客户端「严格冷却 + 投影 + 门禁」语义不同，
/// > 直接由服务端 operator 维护，不强行塞进本片段体系（见设计文档 4.1）。
/// @author TACZ, USS_Shenzhou
public class CooldownSegment extends ActionSegment {

    public static final String ID = "cooldown";
    public static final SegmentToken<CooldownSegment> TOKEN = SegmentToken.of(ID, CooldownSegment.class);

    /// > 下一发冷却间隔供给器：每发出膛现取（RPM 静态或脚本 {@code cooldownMs} 动态）。
    /// > 使得动态射速（转速渐进 / 过热降速）可逐发生效，而非构造时固定。
    private final CooldownProvider provider;

    /// > 最近一次 {@link #onShotFired} 取到的冷却间隔（毫秒），供门禁判定与调试展示。
    private long cooldownMs;

    /// > 墙上时钟：冷却结束、允许下一发的时刻。-1 表示尚未开火（不在冷却中）。
    private long readyAtMs = -1;

    /// > 固定冷却间隔构造（零脚本路径）：等价供给器恒返回同一值。
    public CooldownSegment(long cooldownMs) {
        this(fixed(cooldownMs));
    }

    /// > 动态冷却构造：每发出膛从 {@code provider} 现取下一发间隔。
    public CooldownSegment(CooldownProvider provider) {
        this.provider = provider;
        this.cooldownMs = Math.max(provider.nextCooldownMs(), 1);
    }

    private static CooldownProvider fixed(long cooldownMs) {
        long ms = Math.max(cooldownMs, 1);
        return () -> ms;
    }

    /// > 一发出膛后驱动集合内的冷却片段重启 / 步进节奏。
    /// > 收编原先散落在单发 / burst 片段里「捞 cooldown 调 onShotFired」的重复块。
    public static void driveShotFired(@Nullable SegmentSet segments) {
        if (segments == null) {
            return;
        }
        CooldownSegment cooldown = segments.get(TOKEN);
        if (cooldown != null) {
            cooldown.onShotFired();
        }
    }

    /// > 一发出膛（主发或 burst 后续均适用）：以墙上时钟重启 / 步进冷却。
    /// > 由 {@link SingleShotSegment} 在出膛 notify 时通过 back-reference 驱动。
    public void onShotFired() {
        /// > 每发现取下一发冷却间隔：动态射速（转速渐进 / 过热降速）逐发生效。
        this.cooldownMs = Math.max(provider.nextCooldownMs(), 1);
        long now = System.currentTimeMillis();
        /// > 节奏步进：仍在上一发冷却节奏内（连射），以 readyAtMs 为锚步进，吃掉 tick 量化余数；
        /// > 否则（首发或间隔过大）以当前时刻重新起锚。
        if (readyAtMs > 0 && now - readyAtMs < cooldownMs) {
            readyAtMs += cooldownMs;
        } else {
            readyAtMs = now + cooldownMs;
        }
    }

    /// > 冷却是否已过（墙上时钟严格判定）。
    public boolean isCooldownOver() {
        return readyAtMs < 0 || System.currentTimeMillis() >= readyAtMs;
    }

    public long cooldownMs() {
        return cooldownMs;
    }

    /// > ---- ActionSegment ----

    @Override
    public SegmentToken<CooldownSegment> token() {
        return TOKEN;
    }

    @Override
    public boolean tick(long deltaMs) {
        /// > 持久片段：持枪期间常驻；冷却是否结束由墙上时钟即时判定，无需累加。
        return true;
    }

    @Override
    public void constrain(ActionGate.Builder gate) {
        /// > 冷却期收紧开火门禁（operator 仍可在 FIRE_COOLDOWN 下用 isCooldownOver 精判放行）。
        if (!isCooldownOver()) {
            gate.denyShoot();
        }
    }

    @Nullable
    @Override
    public GunHolderState projectedState() {
        if (!isCooldownOver()) {
            return GunHolderState.FIRE_COOLDOWN;
        }
        return null;
    }

    @Override
    public String debugInfo() {
        if (isCooldownOver()) {
            return "ready";
        }
        long remain = readyAtMs - System.currentTimeMillis();
        return String.format("%dms left / %dms", Math.max(remain, 0), cooldownMs);
    }
}
