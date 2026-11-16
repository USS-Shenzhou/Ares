package cn.ussshenzhou.ares.client.resource.display;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.AnimationBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.AnimationKeyframes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockAnimationPOJO;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectKeyframes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.SoundEffectKeyframes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.TimelineKeyframes;
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// @author TACZ, USS_Shenzhou
class AnimationPoseCompositionHelper {
    private static final Field LOOP_FIELD = animationField("loop");
    private static final Field ANIMATION_LENGTH_FIELD = animationField("animationLength");
    private static final Field BONES_FIELD = animationField("bones");
    private static final Field SOUND_EFFECTS_FIELD = animationField("soundEffects");
    private static final Field PARTICLE_EFFECTS_FIELD = animationField("particleEffects");
    private static final Field TIMELINE_FIELD = animationField("timeline");
    private static final Field POSITION_FIELD = boneField("position");
    private static final Field ROTATION_FIELD = boneField("rotation");
    private static final Field SCALE_FIELD = boneField("scale");

    static Map<String, BedrockAnimationPOJO> compose(Map<String, BedrockAnimationPOJO> resolvedAnimations,
                                                      @Nullable List<AnimationPoseComposition> compositions) {
        Map<String, BedrockAnimationPOJO> result = new LinkedHashMap<>(resolvedAnimations);
        if (compositions == null || compositions.isEmpty()) {
            return result;
        }

        Set<String> declaredTargets = new HashSet<>();
        for (int index = 0; index < compositions.size(); index++) {
            AnimationPoseComposition composition = compositions.get(index);
            if (composition == null) {
                throw new IllegalArgumentException("animation_pose_compositions[" + index + "] must not be null");
            }

            String targetName = requireName(composition.target, index, "target");
            String basePoseName = requireName(composition.basePose, index, "base_pose");
            if (!declaredTargets.add(targetName)) {
                throw new IllegalArgumentException("animation_pose_compositions declares duplicate target: " + targetName);
            }
            if (targetName.equals(basePoseName)) {
                throw new IllegalArgumentException("animation_pose_compositions[" + index
                        + "] target and base_pose must differ: " + targetName);
            }

            BedrockAnimationPOJO target = result.get(targetName);
            if (target == null) {
                throw new IllegalArgumentException("animation_pose_compositions[" + index
                        + "] target animation does not exist: " + targetName);
            }
            BedrockAnimationPOJO basePose = result.get(basePoseName);
            if (basePose == null) {
                throw new IllegalArgumentException("animation_pose_compositions[" + index
                        + "] base_pose animation does not exist: " + basePoseName);
            }

            result.put(targetName, compose(target, basePose));
        }
        return result;
    }

    private static String requireName(@Nullable String name, int index, String fieldName) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("animation_pose_compositions[" + index + "]." + fieldName
                    + " must not be blank");
        }
        return name;
    }

    private static BedrockAnimationPOJO compose(BedrockAnimationPOJO target, BedrockAnimationPOJO basePose) {
        Map<String, AnimationBone> bones = copyBones(target.getBones());
        if (basePose.getBones() != null) {
            for (Map.Entry<String, AnimationBone> entry : basePose.getBones().entrySet()) {
                bones.putIfAbsent(entry.getKey(), copyBone(entry.getValue()));
            }
        }
        return copyAnimation(target, bones);
    }

    private static Map<String, AnimationBone> copyBones(@Nullable Map<String, AnimationBone> source) {
        Map<String, AnimationBone> copy = new LinkedHashMap<>();
        if (source != null) {
            for (Map.Entry<String, AnimationBone> entry : source.entrySet()) {
                copy.put(entry.getKey(), copyBone(entry.getValue()));
            }
        }
        return copy;
    }

    private static BedrockAnimationPOJO copyAnimation(BedrockAnimationPOJO source, Map<String, AnimationBone> bones) {
        try {
            BedrockAnimationPOJO copy = new BedrockAnimationPOJO();
            LOOP_FIELD.setBoolean(copy, source.isLoop());
            ANIMATION_LENGTH_FIELD.setDouble(copy, source.getAnimationLength());
            BONES_FIELD.set(copy, bones);
            SOUND_EFFECTS_FIELD.set(copy, copySoundEffects(source.getSoundEffects()));
            PARTICLE_EFFECTS_FIELD.set(copy, copyParticleEffects(source.getParticleEffects()));
            TIMELINE_FIELD.set(copy, copyTimeline(source.getTimeline()));
            return copy;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to copy Bedrock animation POJO for pose composition", exception);
        }
    }

    @Nullable
    private static AnimationBone copyBone(@Nullable AnimationBone source) {
        if (source == null) {
            return null;
        }
        try {
            AnimationBone copy = new AnimationBone();
            POSITION_FIELD.set(copy, copyKeyframes(source.getPosition()));
            ROTATION_FIELD.set(copy, copyKeyframes(source.getRotation()));
            SCALE_FIELD.set(copy, copyKeyframes(source.getScale()));
            return copy;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to copy Bedrock animation bone for pose composition", exception);
        }
    }

    @Nullable
    private static AnimationKeyframes copyKeyframes(@Nullable AnimationKeyframes source) {
        if (source == null) {
            return null;
        }
        Double2ObjectRBTreeMap<AnimationKeyframes.Keyframe> copy = new Double2ObjectRBTreeMap<>();
        for (Double2ObjectMap.Entry<AnimationKeyframes.Keyframe> entry : source.getKeyframes().double2ObjectEntrySet()) {
            copy.put(entry.getDoubleKey(), copyKeyframe(entry.getValue()));
        }
        return new AnimationKeyframes(copy);
    }

    @Nullable
    private static AnimationKeyframes.Keyframe copyKeyframe(@Nullable AnimationKeyframes.Keyframe source) {
        if (source == null) {
            return null;
        }
        return new AnimationKeyframes.Keyframe(copyVector(source.getPre()), copyVector(source.getPost()),
                copyVector(source.getData()), source.getLerpMode(), copyArray(source.getPreExpressions()),
                copyArray(source.getPostExpressions()), copyArray(source.getDataExpressions()));
    }

    @Nullable
    private static Vector3f copyVector(@Nullable Vector3f source) {
        return source != null ? new Vector3f(source) : null;
    }

    @Nullable
    private static String[] copyArray(@Nullable String[] source) {
        return source != null ? source.clone() : null;
    }

    @Nullable
    private static SoundEffectKeyframes copySoundEffects(@Nullable SoundEffectKeyframes source) {
        if (source == null) {
            return null;
        }
        if (source.getKeyframes() == null) {
            return new SoundEffectKeyframes(null);
        }
        Double2ObjectRBTreeMap<Identifier> copy = new Double2ObjectRBTreeMap<>();
        copy.putAll(source.getKeyframes());
        return new SoundEffectKeyframes(copy);
    }

    @Nullable
    private static ParticleEffectKeyframes copyParticleEffects(@Nullable ParticleEffectKeyframes source) {
        if (source == null) {
            return null;
        }
        if (source.getKeyframes() == null) {
            return new ParticleEffectKeyframes(null);
        }
        Double2ObjectRBTreeMap<ParticleEffectData> copy = new Double2ObjectRBTreeMap<>();
        for (Double2ObjectMap.Entry<ParticleEffectData> entry : source.getKeyframes().double2ObjectEntrySet()) {
            ParticleEffectData data = entry.getValue();
            copy.put(entry.getDoubleKey(), data == null ? null
                    : new ParticleEffectData(data.effect(), data.locator(), data.preEffectScript()));
        }
        return new ParticleEffectKeyframes(copy);
    }

    @Nullable
    private static TimelineKeyframes copyTimeline(@Nullable TimelineKeyframes source) {
        if (source == null) {
            return null;
        }
        if (source.getKeyframes() == null) {
            return new TimelineKeyframes(null);
        }
        Double2ObjectRBTreeMap<List<String>> copy = new Double2ObjectRBTreeMap<>();
        for (Double2ObjectMap.Entry<List<String>> entry : source.getKeyframes().double2ObjectEntrySet()) {
            List<String> commands = entry.getValue();
            copy.put(entry.getDoubleKey(), commands != null ? new ArrayList<>(commands) : null);
        }
        return new TimelineKeyframes(copy);
    }

    private static Field animationField(String name) {
        return field(BedrockAnimationPOJO.class, name);
    }

    private static Field boneField(String name) {
        return field(AnimationBone.class, name);
    }

    private static Field field(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
