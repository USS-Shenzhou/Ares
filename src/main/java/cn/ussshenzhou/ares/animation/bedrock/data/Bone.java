package cn.ussshenzhou.ares.animation.bedrock.data;

import com.google.gson.annotations.SerializedName;

/**
 * @author TartaricAcid, USS_Shenzhou
 * <p></p>
 * This file is intergrated from <a href="https://github.com/MCModderAnchor/TACZ">Timeless and Classics Guns Zero</a> under GPLv3 license.
 */
public class Bone {
    @SerializedName("position")
    private Keyframes position;

    @SerializedName("rotation")
    private Keyframes rotation;

    @SerializedName("scale")
    private Keyframes scale;

    public Keyframes getPosition() {
        return position;
    }

    public Keyframes getRotation() {
        return rotation;
    }

    public Keyframes getScale() {
        return scale;
    }
}
