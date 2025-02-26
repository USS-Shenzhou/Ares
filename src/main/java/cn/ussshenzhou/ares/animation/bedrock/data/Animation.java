package cn.ussshenzhou.ares.animation.bedrock.data;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author TartaricAcid, USS_Shenzhou
 * <p></p>
 * This file is intergrated from <a href="https://github.com/MCModderAnchor/TACZ">Timeless and Classics Guns Zero</a> under GPLv3 license.
 */
public class Animation {
    @SerializedName("loop")
    private boolean loop;

    @SerializedName("animation_length")
    private double animationLength;

    @SerializedName("bones")
    @Nullable
    private Map<String, Bone> bones;

    @SerializedName("sound_effects")
    private SoundEffectKeyframes soundEffects;

    public boolean isLoop() {
        return loop;
    }

    public double getAnimationLength() {
        return animationLength;
    }

    @Nullable
    public Map<String, Bone> getBones() {
        return bones;
    }

    public SoundEffectKeyframes getSoundEffects() {
        return soundEffects;
    }
}
