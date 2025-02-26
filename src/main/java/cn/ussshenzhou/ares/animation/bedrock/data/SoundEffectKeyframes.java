package cn.ussshenzhou.ares.animation.bedrock.data;

import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import net.minecraft.resources.ResourceLocation;

/**
 * @author TartaricAcid, USS_Shenzhou
 * <p></p>
 * This file is intergrated from <a href="https://github.com/MCModderAnchor/TACZ">Timeless and Classics Guns Zero</a> under GPLv3 license.
 */
public class SoundEffectKeyframes {
    private final Double2ObjectRBTreeMap<ResourceLocation> keyframes;

    public SoundEffectKeyframes(Double2ObjectRBTreeMap<ResourceLocation> keyframes) {
        this.keyframes = keyframes;
    }

    public Double2ObjectRBTreeMap<ResourceLocation> getKeyframes() {
        return keyframes;
    }
}
