package com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.exclusion.ClientOnly;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.exclusion.NeedForRootMotion;
import com.google.gson.annotations.SerializedName;

public class GeometryModelLegacy {
    @SerializedName("bones")
    @NeedForRootMotion
    private BonesItem[] bones;

    @SerializedName("textureheight")
    @ClientOnly
    private int textureHeight;

    @SerializedName("texturewidth")
    @ClientOnly
    private int textureWidth;

    @SerializedName("visible_bounds_height")
    @ClientOnly
    private float visibleBoundsHeight;

    @SerializedName("visible_bounds_width")
    @ClientOnly
    private float visibleBoundsWidth;

    @SerializedName("visible_bounds_offset")
    @ClientOnly
    private float[] visibleBoundsOffset;

    public BonesItem[] getBones() {
        return bones;
    }

    public int getTextureHeight() {
        return textureHeight;
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public float getVisibleBoundsHeight() {
        return visibleBoundsHeight;
    }

    public float getVisibleBoundsWidth() {
        return visibleBoundsWidth;
    }

    public float[] getVisibleBoundsOffset() {
        return visibleBoundsOffset;
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