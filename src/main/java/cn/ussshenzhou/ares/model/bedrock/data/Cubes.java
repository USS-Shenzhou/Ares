package cn.ussshenzhou.ares.model.bedrock.data;

import com.google.common.collect.Lists;
import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author TartaricAcid, USS_Shenzhou
 * <p></p>
 * This file is intergrated from <a href="https://github.com/TartaricAcid/TouhouLittleMaid">TouhouLittleMaid</a> under MIT license.
 */
public class Cubes {
    private List<Float> uv;
    private FaceContainer faceUv;
    private boolean mirror = false;
    private boolean hasMirror = false;

    @Expose
    @SerializedName("inflate")
    private float inflate;

    @Expose
    @SerializedName("size")
    private List<Float> size;

    @Expose
    @SerializedName("origin")
    private List<Float> origin;

    @Expose
    @SerializedName("rotation")
    private List<Float> rotation;

    @Expose
    @SerializedName("pivot")
    private List<Float> pivot;

    public List<Float> getUv() {
        return uv;
    }

    @Nullable
    public FaceContainer getFaceUv() {
        return faceUv;
    }

    public boolean isMirror() {
        return mirror;
    }

    public void setMirror(boolean mirror) {
        this.mirror = mirror;
    }

    public boolean isHasMirror() {
        return hasMirror;
    }

    public float getInflate() {
        return inflate;
    }

    /**
     * 基岩版这货居然可以为浮点数，服了
     */
    public List<Float> getSize() {
        return size;
    }

    public List<Float> getOrigin() {
        return origin;
    }

    @Nullable
    public List<Float> getRotation() {
        return rotation;
    }

    @Nullable
    public List<Float> getPivot() {
        return pivot;
    }

    public static class Deserializer implements JsonDeserializer<Cubes> {
        @Override
        public Cubes deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            Cubes cube = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, Cubes.class);
            if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();

                JsonElement uvElement = obj.get("uv");
                if (uvElement.isJsonArray()) {
                    cube.uv = Lists.newArrayList();
                    JsonArray array = uvElement.getAsJsonArray();
                    for (int i = 0; i < array.size(); i++) {
                        cube.uv.add(array.get(i).getAsFloat());
                    }
                }
                if (uvElement.isJsonObject()) {
                    cube.faceUv = new Gson().fromJson(uvElement, FaceContainer.class);

                }

                JsonElement mirrorElement = obj.get("mirror");
                if (mirrorElement != null && mirrorElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = mirrorElement.getAsJsonPrimitive();
                    if (primitive.isBoolean()) {
                        cube.mirror = primitive.getAsBoolean();
                        cube.hasMirror = true;
                    }
                }
            }
            return cube;
        }
    }
}