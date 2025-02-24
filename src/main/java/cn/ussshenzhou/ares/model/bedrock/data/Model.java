package cn.ussshenzhou.ares.model.bedrock.data;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author TartaricAcid, USS_Shenzhou
 * <p></p>
 * This file is intergrated from <a href="https://github.com/TartaricAcid/TouhouLittleMaid">TouhouLittleMaid</a> under MIT license.
 */
public class Model {
    @SerializedName("format_version")
    private String formatVersion;

    @SerializedName("minecraft:geometry")
    @Nullable
    private List<Geometry> geometryNew;

    public String getFormatVersion() {
        return formatVersion;
    }

    @Nullable
    public Geometry getGeometryModel() {
        if (geometryNew == null) {
            return null;
        }
        return geometryNew.getFirst();
    }
}