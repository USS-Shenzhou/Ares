package cn.ussshenzhou.ares.model.bedrock.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * @author TartaricAcid, USS_Shenzhou
 * <p></p>
 * This file is intergrated from <a href="https://github.com/TartaricAcid/TouhouLittleMaid">TouhouLittleMaid</a> under MIT license.
 */
public class Description {
    @SerializedName("texture_height")
    private int textureHeight;

    @SerializedName("texture_width")
    private int textureWidth;

    @SerializedName("visible_bounds_height")
    private float visibleBoundsHeight;

    @SerializedName("visible_bounds_width")
    private float visibleBoundsWidth;

    @SerializedName("visible_bounds_offset")
    private List<Float> visibleBoundsOffset;

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

    public List<Float> getVisibleBoundsOffset() {
        return visibleBoundsOffset;
    }
}
