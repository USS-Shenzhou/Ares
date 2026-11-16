package cn.ussshenzhou.ares.gun.client.animation.statemachine;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/// > 状态输出姿态的来源声明。
/// > <p>
/// > pose 是数据查询（返回 Pose 对象）而非动作，因此用结构化字段声明而非 Molang。
/// > <p>
/// > 支持两种写法（{@code tracks} 优先于 {@code track}）：
/// > <ul>
/// >   <li>单轨道：{@code "pose": { "track": "base" }}</li>
/// >   <li>多轨道合成：{@code "pose": { "tracks": ["base", "selector"] }}，以首个轨道为基底，
/// >       后续轨道按 additive 依次叠加（各轨道 pose 均为相对 bind 的增量）。
/// >       骨骼不相交时等价于覆盖（如 selector 拨杆叠在 base 手部姿态上）；
/// >       相交时为增量相加，符合动画师制作叠加层的习惯。</li>
/// > </ul>
/// @author TACZ, USS_Shenzhou
public class StateMachinePose {
    /// > 输出姿态读取的单个轨道名（与 {@link #tracks} 二选一，后者优先）。
    @SerializedName("track")
    public String track;

    /// > 输出姿态合成读取的多个轨道名。以首个为基底，后续按 additive 依次叠加。
    @SerializedName("tracks")
    public List<String> tracks;

    /// > 解析有效轨道列表：{@code tracks} 优先；否则回退到单个 {@code track}；都为空返回空列表。
    public List<String> resolveTracks() {
        List<String> result = new ArrayList<>();
        if (tracks != null && !tracks.isEmpty()) {
            for (String t : tracks) {
                if (t != null && !t.isBlank()) {
                    result.add(t);
                }
            }
        } else if (track != null && !track.isBlank()) {
            result.add(track);
        }
        return result;
    }
}
