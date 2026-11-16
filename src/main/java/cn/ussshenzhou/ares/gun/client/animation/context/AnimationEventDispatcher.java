package cn.ussshenzhou.ares.gun.client.animation.context;

import com.mojang.logging.LogUtils;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// @author TACZ, USS_Shenzhou
public class AnimationEventDispatcher implements AnimationEventSink {
    private static final Set<String> KNOWN_NOTIFY_EVENTS = Set.of(
            "mag_out",
            "mag_in",
            "bolt_release",
            "shell_eject"
    );

    private final WeaponAnimationContext context;
    /// > 本帧动画触发、尚待渲染层消费生成发射器的粒子请求。
    /// > 渲染层（BedrockGunItemRenderer）在拿到枪口真实变换时取走并清空。
    private final List<ParticleEffectData> pendingParticles = new ArrayList<>();
    /// > 本帧动画触发、尚待渲染层消费的抛壳请求，元素为抛壳窗序号（shell_eject -> 0, shell_eject_N -> N）。
    private final List<Integer> pendingShellEjects = new ArrayList<>();

    public AnimationEventDispatcher(WeaponAnimationContext context) {
        this.context = context;
    }

    @Override
    public void onNotify(AnimationNotifyEvent event) {
        if (event == null) {
            return;
        }
        String eventName = event.event();
        if (KNOWN_NOTIFY_EVENTS.contains(eventName)) {
            LogUtils.getLogger().debug("[TACZ] Animation notify dispatch: montage={}, section={}, track={}, event={}, time={}",
                    event.montageId(), event.sectionName(), event.trackName(), eventName, event.keyTime());
        } else {
            LogUtils.getLogger().debug("[TACZ] Animation notify ignored: montage={}, section={}, track={}, event={}, time={}",
                    event.montageId(), event.sectionName(), event.trackName(), eventName, event.keyTime());
        }
        /// > 抛壳事件：shell_eject -> 抛壳窗 0；shell_eject_N -> 抛壳窗 N。
        int shellWindow = parseShellEjectWindow(eventName);
        if (shellWindow >= 0) {
            pendingShellEjects.add(shellWindow);
        }
    }

    /// > 解析抛壳事件名对应的抛壳窗序号；非抛壳事件返回 -1。
    /// > `shell_eject` -> 0，`shell_eject_N` -> N。
    private static int parseShellEjectWindow(String eventName) {
        if (eventName == null) {
            return -1;
        }
        if (eventName.equals("shell_eject")) {
            return 0;
        }
        if (eventName.startsWith("shell_eject_")) {
            try {
                return Math.max(0, Integer.parseInt(eventName.substring("shell_eject_".length())));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return -1;
    }

    @Override
    public void onSound(Identifier soundId) {
        if (context == null || context.player() == null || soundId == null) {
            return;
        }
        Level level = context.player().level();
        if (!level.isClientSide()) {
            return;
        }
        BlockPos pos = context.player().getOnPos();
        SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundId);
        level.playSound(context.player(), pos, soundEvent, SoundSource.PLAYERS);
    }

    @Override
    public void onParticle(ParticleEffectData data) {
        if (data == null || data.effect() == null) {
            return;
        }
        pendingParticles.add(data);
    }

    /// > 取出本帧累积的粒子请求并清空。由渲染层每帧调用。
    public List<ParticleEffectData> consumePendingParticles() {
        if (pendingParticles.isEmpty()) {
            return List.of();
        }
        List<ParticleEffectData> result = new ArrayList<>(pendingParticles);
        pendingParticles.clear();
        return result;
    }

    /// > 取出本帧累积的抛壳请求（抛壳窗序号列表）并清空。由渲染层每帧调用。
    public List<Integer> consumePendingShellEjects() {
        if (pendingShellEjects.isEmpty()) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>(pendingShellEjects);
        pendingShellEjects.clear();
        return result;
    }
}
