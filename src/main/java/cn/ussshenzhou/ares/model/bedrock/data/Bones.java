package cn.ussshenzhou.ares.model.bedrock.data;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author TartaricAcid, USS_Shenzhou
 * <p></p>
 * This file is intergrated from <a href="https://github.com/TartaricAcid/TouhouLittleMaid">TouhouLittleMaid</a> under MIT license.
 */
public class Bones {
    @SerializedName("cubes")
    private List<Cubes> cubes;

    @SerializedName("name")
    private String name;

    @SerializedName("pivot")
    private List<Float> pivot;

    @SerializedName("rotation")
    private List<Float> rotation;

    @SerializedName("parent")
    private String parent;

    @SerializedName("mirror")
    private boolean mirror = false;

    @Nullable
    public List<Cubes> getCubes() {
        return cubes;
    }

    public String getName() {
        return name;
    }

    public List<Float> getPivot() {
        return pivot;
    }

    public List<Float> getRotation() {
        return rotation;
    }

    public String getParent() {
        return parent;
    }

    public boolean isMirror() {
        return mirror;
    }

    @Override
    public String toString() {
        return
                "BonesItem{" +
                        "cubes = '" + cubes + '\'' +
                        ",name = '" + name + '\'' +
                        ",pivot = '" + pivot + '\'' +
                        ",rotation = '" + rotation + '\'' +
                        ",parent = '" + parent + '\'' +
                        "}";
    }
}