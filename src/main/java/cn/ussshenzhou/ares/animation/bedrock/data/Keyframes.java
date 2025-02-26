package cn.ussshenzhou.ares.animation.bedrock.data;


import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * @author TartaricAcid, USS_Shenzhou
 * <p></p>
 * This file is intergrated from <a href="https://github.com/MCModderAnchor/TACZ">Timeless and Classics Guns Zero</a> under GPLv3 license.
 */
@SuppressWarnings("MapOrSetKeyShouldOverrideHashCodeEquals")
public class Keyframes {
    private final Double2ObjectRBTreeMap<Keyframe> keyframes;

    public Keyframes(Double2ObjectRBTreeMap<Keyframe> keyframes) {
        this.keyframes = keyframes;
    }

    public Double2ObjectRBTreeMap<Keyframe> getKeyframes() {
        return keyframes;
    }

    public record Keyframe(@Nullable Vector3f pre, @Nullable Vector3f post, @Nullable Vector3f data,
                           @Nullable String lerpMode) {
    }
}
