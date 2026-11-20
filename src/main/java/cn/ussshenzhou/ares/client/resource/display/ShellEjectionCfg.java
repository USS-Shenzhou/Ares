package cn.ussshenzhou.ares.client.resource.display;

import com.google.gson.annotations.SerializedName;
import org.joml.Vector3f;

/// > 抛壳运动配置（枪 display）。
/// >
/// > 弹壳生成后在「冻结的第一人称视图坐标系」内做匀变速直线运动 + 三轴匀角速度旋转，
/// > 各参数的缺省值沿用 legacy 实现。坐标轴含义同模型空间：x=右, y=上, z=前。
/// @author TACZ, USS_Shenzhou
public class ShellEjectionCfg {
    /// > 抛壳初速度（每秒，模型空间单位）。
    @SerializedName("initial_velocity")
    public Vector3f initialVelocity = new Vector3f(8.0f, 5.0f, -0.5f);

    /// > 抛壳随机速度幅度，每枚弹壳在 [0, random] 区间取随机偏移叠加到初速度。
    @SerializedName("random_velocity")
    public Vector3f randomVelocity = new Vector3f(2.5f, 1.5f, 0.25f);

    /// > 抛壳加速度（每秒²），通常 y 为负表示重力。
    @SerializedName("acceleration")
    public Vector3f acceleration = new Vector3f(0, -20f, 0);

    /// > 抛壳三轴角速度（度/秒）。
    @SerializedName("angular_velocity")
    public Vector3f angularVelocity = new Vector3f(-720, -720, 90);

    /// > 抛壳存活时间（秒），超时后从队列移除。
    @SerializedName("living_time")
    public float livingTime = 1.0f;
}
