package cn.ussshenzhou.ares.model.bedrock.data;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author TartaricAcid, USS_Shenzhou
 * <p></p>
 * This file is intergrated from <a href="https://github.com/TartaricAcid/TouhouLittleMaid">TouhouLittleMaid</a> under MIT license.
 */
public class Geometry {
    @SerializedName("description")
    private Description description;

    @SerializedName("bones")
    @Nullable
    private List<Bones> bones;

    public Description getDescription() {
        return description;
    }

    @Nullable
    public List<Bones> getBones() {
        return bones;
    }

    public Geometry deco() {
        if (bones != null) {
            this.bones.forEach(bonesItem -> {
                if (bonesItem.getCubes() != null) {
                    bonesItem.getCubes().forEach(cubesItem -> {
                        if (!cubesItem.isHasMirror()) {
                            cubesItem.setMirror(bonesItem.isMirror());
                        }
                    });
                }
            });
        }
        return this;
    }
}
