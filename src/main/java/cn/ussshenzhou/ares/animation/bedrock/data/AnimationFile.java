package cn.ussshenzhou.ares.animation.bedrock.data;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * @author TartaricAcid, USS_Shenzhou
 * <p></p>
 * This file is intergrated from <a href="https://github.com/MCModderAnchor/TACZ">Timeless and Classics Guns Zero</a> under GPLv3 license.
 */
public class AnimationFile {
    @SerializedName("version")
    private String version;

    @SerializedName("animations")
    private Map<String, Animation> animations;

    public String getVersion() {
        return version;
    }

    public Map<String, Animation> getAnimations() {
        return animations;
    }
}
