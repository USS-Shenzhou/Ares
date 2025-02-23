package cn.ussshenzhou.ares.model.bedrock.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.Direction;

/**
 * @author TartaricAcid, USS_Shenzhou
 * <p></p>
 * This file is intergrated from <a href="https://github.com/TartaricAcid/TouhouLittleMaid">TouhouLittleMaid</a> under MIT license.
 */
public class FaceContainer {
    @SerializedName("down")
    private Face down;
    @SerializedName("east")
    private Face east;
    @SerializedName("north")
    private Face north;
    @SerializedName("south")
    private Face south;
    @SerializedName("up")
    private Face up;
    @SerializedName("west")
    private Face west;

    public static FaceContainer singleSouthFace() {
        FaceContainer faces = new FaceContainer();
        faces.north = Face.EMPTY;
        faces.east = Face.EMPTY;
        faces.west = Face.EMPTY;
        faces.south = Face.single16X();
        faces.up = Face.EMPTY;
        faces.down = Face.EMPTY;
        return faces;
    }

    public Face getFace(Direction direction) {
        return switch (direction) {
            case EAST -> west == null ? Face.EMPTY : west;
            case WEST -> east == null ? Face.EMPTY : east;
            case NORTH -> north == null ? Face.EMPTY : north;
            case SOUTH -> south == null ? Face.EMPTY : south;
            case UP -> down == null ? Face.EMPTY : down;
            default -> up == null ? Face.EMPTY : up;
        };
    }
}
