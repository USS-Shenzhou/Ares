package cn.ussshenzhou.ares.client.animation.montage;

import com.mojang.logging.LogUtils;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.Keyframe;
import com.maydaymemory.mae.basic.ArrayClipChannel;
import com.maydaymemory.mae.control.montage.AnimationMontage;
import com.maydaymemory.mae.control.montage.AnimationMontageSection;
import com.maydaymemory.mae.control.montage.AnimationMontageTrack;
import com.maydaymemory.mae.control.montage.AnimationNotifyKeyframe;
import com.maydaymemory.mae.control.montage.AnimationSegment;
import com.maydaymemory.mae.control.montage.AnimationSegmentKeyframe;
import com.maydaymemory.mae.control.montage.IAnimationNotify;
import cn.ussshenzhou.ares.client.animation.context.AnimationNotifyEvent;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/// > 把 datapack 的 [MontageCfg] 编译为 MAE 的 [AnimationMontage]。
/// >
/// > 蒙太奇的播放、轨道混合、section 切换、notify/clip 触发全部交由 MAE 的
/// > [com.maydaymemory.mae.control.montage.AnimationMontageRunner] 执行，本类只负责数据转换。
/// @author TACZ, USS_Shenzhou
public class MontageCompiler {
    /// > 编译蒙太奇定义。animationResolver 用于把动画名解析为 [BedrockAnimation]。
    /// > 编译失败（无有效轨道/段）时返回 null。
    public static AnimationMontage<MontageContext> compile(Identifier montageId,
                                                           MontageCfg montageCfg,
                                                           Function<String, BedrockAnimation> animationResolver) {
        AnimationMontage<MontageContext> montage = new AnimationMontage<>();

        List<AnimationMontageTrack> tracks = new ArrayList<>();
        float inferredEnd = 0.0f;
        boolean hasSegment = false;
        for (MontageTrackCfg trackCfg : tracks(montageCfg)) {
            if (trackCfg == null || trackCfg.segments == null) {
                continue;
            }
            ArrayList<Keyframe<AnimationSegment>> segmentKeyframes = new ArrayList<>();
            for (MontageSegmentCfg segmentCfg : trackCfg.segments) {
                AnimationSegment segment = buildSegment(montageId, segmentCfg, animationResolver);
                if (segment == null) {
                    continue;
                }
                segmentKeyframes.add(new AnimationSegmentKeyframe(segmentCfg.startTime, segment));
                hasSegment = true;
                inferredEnd = Math.max(inferredEnd, segmentCfg.startTime + segment.getLength());
            }
            if (segmentKeyframes.isEmpty()) {
                continue;
            }
            AnimationMontageTrack track = new AnimationMontageTrack(segmentKeyframes);
            track.setAdditive(trackCfg.additive);
            track.setEnabled(trackCfg.enabled);
            tracks.add(track);
        }
        if (!hasSegment) {
            LogUtils.getLogger().warn("[TACZ] Animation montage has no valid segments: montage={}", montageId);
            return null;
        }
        montage.setTracks(tracks);

        montage.setSections(buildSections(montageId, montageCfg, inferredEnd));
        montage.setNotifyChannels(buildNotifyChannels(montageId, montageCfg));
        return montage;
    }

    private static AnimationSegment buildSegment(Identifier montageId,
                                                 MontageSegmentCfg segmentCfg,
                                                 Function<String, BedrockAnimation> animationResolver) {
        if (segmentCfg == null || segmentCfg.animation == null) {
            LogUtils.getLogger().warn("[TACZ] Animation montage segment missing animation: montage={}", montageId);
            return null;
        }
        BedrockAnimation animation = animationResolver.apply(segmentCfg.animation);
        if (animation == null) {
            LogUtils.getLogger().warn("[TACZ] Animation bedrock animation missing: montage={}, animation={}", montageId, segmentCfg.animation);
            return null;
        }
        float sourceEnd = segmentCfg.sourceEndTime != null ? segmentCfg.sourceEndTime : animation.getSpecifiedEndTimeS();
        return new AnimationSegment(animation, segmentCfg.sourceStartTime, sourceEnd);
    }

    private static Map<String, AnimationMontageSection> buildSections(Identifier montageId,
                                                                      MontageCfg montageCfg,
                                                                      float inferredEnd) {
        Map<String, AnimationMontageSection> sections = new HashMap<>();
        if (montageCfg.sections != null && !montageCfg.sections.isEmpty()) {
            for (MontageSectionCfg sectionCfg : montageCfg.sections) {
                if (sectionCfg == null || sectionCfg.name == null) {
                    continue;
                }
                sections.put(sectionCfg.name, new AnimationMontageSection(
                        sectionCfg.name, sectionCfg.startTime, sectionCfg.endTime, sectionCfg.nextSectionName));
            }
            return sections;
        }
        /// > 未声明 sections 时，用所有段推断出的总时长构建一个默认 main section。
        if (inferredEnd <= 0.0f) {
            LogUtils.getLogger().warn("[TACZ] Animation montage cannot infer default main section: montage={}", montageId);
            return sections;
        }
        sections.put("main", new AnimationMontageSection("main", 0.0f, inferredEnd, null));
        return sections;
    }

    private static List<com.maydaymemory.mae.basic.ClipChannel<IAnimationNotify<MontageContext>>> buildNotifyChannels(
            Identifier montageId, MontageCfg montageCfg) {
        if (montageCfg.notifyTracks == null || montageCfg.notifyTracks.isEmpty()) {
            return null;
        }
        List<com.maydaymemory.mae.basic.ClipChannel<IAnimationNotify<MontageContext>>> channels = new ArrayList<>();
        for (MontageNotifyTrackCfg trackCfg : montageCfg.notifyTracks) {
            if (trackCfg == null || trackCfg.keys == null || trackCfg.keys.isEmpty()) {
                continue;
            }
            ArrayList<Keyframe<IAnimationNotify<MontageContext>>> keyframes = new ArrayList<>();
            for (MontageNotifyKeyCfg keyCfg : trackCfg.keys) {
                if (keyCfg == null) {
                    continue;
                }
                String event = keyCfg.event != null && !keyCfg.event.isBlank() ? keyCfg.event : trackCfg.name;
                keyframes.add(new AnimationNotifyKeyframe<>(keyCfg.time,
                        new EventNotify(montageId, trackCfg.name, event, keyCfg.time)));
            }
            if (!keyframes.isEmpty()) {
                channels.add(new ArrayClipChannel<>(keyframes));
            }
        }
        return channels.isEmpty() ? null : channels;
    }

    private static List<MontageTrackCfg> tracks(MontageCfg cfg) {
        return cfg.tracks != null ? cfg.tracks : List.of();
    }

    /// > 把 datapack notify 关键帧转发为 [AnimationNotifyEvent] 到事件 sink。
    private record EventNotify(Identifier montageId, String trackName, String event, float keyTime)
            implements IAnimationNotify<MontageContext> {
        @Override
        public void onNotify(MontageContext context) {
            context.eventSink().onNotify(new AnimationNotifyEvent(montageId, null, trackName, event, keyTime));
        }
    }
}
