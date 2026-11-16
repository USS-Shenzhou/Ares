package cn.ussshenzhou.ares.gun.custom;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nonnull;

/// @author TACZ, USS_Shenzhou
public class GunRecoilKeyFrame implements Comparable<GunRecoilKeyFrame> {
    @SerializedName("time")
    private float time;

    @SerializedName("value")
    private float[] value = new float[]{0, 0};

    public GunRecoilKeyFrame(float time, float minValue, float maxValue) {
        this.time = time;
        this.value = new float[]{minValue, maxValue};
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }

    public float[] getValue() {
        return value;
    }

    public void setValue(float[] value) {
        this.value = value;
    }

    @Override
    public int compareTo(@Nonnull GunRecoilKeyFrame o) {
        return Double.compare(this.time, o.time);
    }
}
