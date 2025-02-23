package cn.ussshenzhou.ares.model.bedrock;

import org.joml.Vector3f;

/**
 * @author TartaricAcid, USS_Shenzhou
 * <p></p>
 * This file is intergrated from <a href="https://github.com/TartaricAcid/TouhouLittleMaid">TouhouLittleMaid</a> under MIT license.
 */
public class BedrockVertex {
    public final Vector3f pos;
    public final float u;
    public final float v;

    public BedrockVertex(float x, float y, float z, float u, float v) {
        this(new Vector3f(x, y, z), u, v);
    }

    public BedrockVertex(Vector3f pos, float u, float v) {
        this.pos = pos;
        this.u = u;
        this.v = v;
    }

    public BedrockVertex remap(float u, float v) {
        return new BedrockVertex(this.pos, u, v);
    }
}
