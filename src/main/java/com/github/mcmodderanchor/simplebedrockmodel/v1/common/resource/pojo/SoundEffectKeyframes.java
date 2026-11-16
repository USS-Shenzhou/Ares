package com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo;

import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import net.minecraft.resources.Identifier;

public class SoundEffectKeyframes {
    private final Double2ObjectRBTreeMap<Identifier> keyframes;

    public SoundEffectKeyframes(Double2ObjectRBTreeMap<Identifier> keyframes) {
        this.keyframes = keyframes;
    }

    public Double2ObjectRBTreeMap<Identifier> getKeyframes() {
        return keyframes;
    }
}
