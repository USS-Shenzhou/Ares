package cn.ussshenzhou.ares.model.bedrock.data;

import com.google.gson.annotations.SerializedName;

/**
 * @author TartaricAcid, USS_Shenzhou
 * <p></p>
 * This file is intergrated from <a href="https://github.com/TartaricAcid/TouhouLittleMaid">TouhouLittleMaid</a> under MIT license.
 */
public class Face {
    public static final Face EMPTY = empty();

    @SerializedName("uv")
    private float[] uv;

    @SerializedName("uv_size")
    private float[] uvSize;

    public static Face single16X() {
        Face face = new Face();
        face.uv = new float[]{0, 0};
        face.uvSize = new float[]{16, 16};
        return face;
    }

    private static Face empty() {
        Face face = new Face();
        face.uv = new float[]{0, 0};
        face.uvSize = new float[]{0, 0};
        return face;
    }

    public float[] getUv() {
        return uv;
    }

    public float[] getUvSize() {
        return uvSize;
    }
}
