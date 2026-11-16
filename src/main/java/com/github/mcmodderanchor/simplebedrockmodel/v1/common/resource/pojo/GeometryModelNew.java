package com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.exclusion.ClientOnly;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.exclusion.NeedForRootMotion;
import com.google.gson.annotations.SerializedName;

public class GeometryModelNew {
    @SerializedName("description")
    @ClientOnly
    private Description description;

    @SerializedName("bones")
    @NeedForRootMotion
    private BonesItem[] bones;

    public Description getDescription() {
        return description;
    }

    public BonesItem[] getBones() {
        return bones;
    }

    public void deco() {
        if (bones == null) {
            return;
        }
        for (BonesItem bonesItem : this.bones) {
            if (bonesItem.getCubes() == null) {
                continue;
            }
            for (CubesItem cubesItem : bonesItem.getCubes()) {
                if (!cubesItem.isHasMirror()) {
                    cubesItem.setMirror(bonesItem.isMirror());
                }
            }
        }
    }
}
