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

/// > 把 datapack 的 [Montage] 编译为 MAE 的 [AnimationMontage]。
/// >
/// > 蒙太奇的播放、轨道混合、section 切换、notify/clip 触发全部交由 MAE 的
/// > [com.maydaymemory.mae.control.montage.AnimationMontageRunner] 执行，本类只负责数据转换。
/// @author TACZ, USS_Shenzhou
public class MontageCompiler {
    /// > 编译蒙太奇定义。animationResolver 用于把动画名解析为 [BedrockAnimation]。
    /// > 编译失败（无有效轨道/段）时返回 null。
    public static AnimationMontage<MontageContext> compile(Identifier montageId,
                                                           Montage definition,
                                                           Function<String, BedrockAnimation> animationResolver) {
        AnimationMontage<MontageContext> montage = new AnimationMontage<>();

        List<AnimationMontageTrack> tracks = new ArrayList<>();
        float inferredEnd = 0.0f;
        boolean hasSegment = false;
        for (MontageTrack trackDef : tracks(definition)) {
            if (trackDef == null || trackDef.segments == null) {
                continue;
            }
            ArrayList<Keyframe<AnimationSegment>> segmentKeyframes = new ArrayList<>();
            for (MontageSegment segmentDef : trackDef.segments) {
                AnimationSegment segment = buildSegment(montageId, segmentDef, animationResolver);
                if (segment == null) {
                    continue;
                }
                segmentKeyframes.add(new AnimationSegmentKeyframe(segmentDef.startTime, segment));
                hasSegment = true;
                inferredEnd = Math.max(inferredEnd, segmentDef.startTime + segment.getLength());
            }
            if (segmentKeyframes.isEmpty()) {
                continue;
            }
            AnimationMontageTrack track = new AnimationMontageTrack(segmentKeyframes);
            track.setAdditive(trackDef.additive);
            track.setEnabled(trackDef.enabled);
            tracks.add(track);
        }
        if (!hasSegment) {
            LogUtils.getLogger().warn("[TACZ] Animation montage has no valid segments: montage={}", montageId);
            return null;
        }
        montage.setTracks(tracks);

        montage.setSections(buildSections(montageId, definition, inferredEnd));
        montage.setNotifyChannels(buildNotifyChannels(montageId, definition));
        return montage;
    }

    private static AnimationSegment buildSegment(Identifier montageId,
                                                 MontageSegment segmentDef,
                                                 Function<String, BedrockAnimation> animationResolver) {
        if (segmentDef == null || segmentDef.animation == null) {
            LogUtils.getLogger().warn("[TACZ] Animation montage segment missing animation: montage={}", montageId);
            return null;
        }
        BedrockAnimation animation = animationResolver.apply(segmentDef.animation);
        if (animation == null) {
            LogUtils.getLogger().warn("[TACZ] Animation bedrock animation missing: montage={}, animation={}", montageId, segmentDef.animation);
            return null;
        }
        float sourceEnd = segmentDef.sourceEndTime != null ? segmentDef.sourceEndTime : animation.getSpecifiedEndTimeS();
        return new AnimationSegment(animation, segmentDef.sourceStartTime, sourceEnd);
    }

    private static Map<String, AnimationMontageSection> buildSections(Identifier montageId,
                                                                      Montage definition,
                                                                      float inferredEnd) {
        Map<String, AnimationMontageSection> sections = new HashMap<>();
        if (definition.sections != null && !definition.sections.isEmpty()) {
            for (MontageSection sectionDef : definition.sections) {
                if (sectionDef == null || sectionDef.name == null) {
                    continue;
                }
                sections.put(sectionDef.name, new AnimationMontageSection(
                        sectionDef.name, sectionDef.startTime, sectionDef.endTime, sectionDef.nextSectionName));
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
            Identifier montageId, Montage definition) {
        if (definition.notifyTracks == null || definition.notifyTracks.isEmpty()) {
            return null;
        }
        List<com.maydaymemory.mae.basic.ClipChannel<IAnimationNotify<MontageContext>>> channels = new ArrayList<>();
        for (MontageNotifyTrack trackDef : definition.notifyTracks) {
            if (trackDef == null || trackDef.keys == null || trackDef.keys.isEmpty()) {
                continue;
            }
            ArrayList<Keyframe<IAnimationNotify<MontageContext>>> keyframes = new ArrayList<>();
            for (MontageNotifyKey keyDef : trackDef.keys) {
                if (keyDef == null) {
                    continue;
                }
                String event = keyDef.event != null && !keyDef.event.isBlank() ? keyDef.event : trackDef.name;
                keyframes.add(new AnimationNotifyKeyframe<>(keyDef.time,
                        new EventNotify(montageId, trackDef.name, event, keyDef.time)));
            }
            if (!keyframes.isEmpty()) {
                channels.add(new ArrayClipChannel<>(keyframes));
            }
        }
        return channels.isEmpty() ? null : channels;
    }

    private static List<MontageTrack> tracks(Montage definition) {
        return definition.tracks != null ? definition.tracks : List.of();
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
