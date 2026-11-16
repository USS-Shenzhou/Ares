package cn.ussshenzhou.ares.gun.client.renderer.entity;

import cn.ussshenzhou.ares.gun.gameplay.MuzzleReference;
import net.minecraft.world.InteractionHand;
import javax.annotation.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/// > 第一人称视图空间姿态缓存（渲染期 → 事件期 的单帧传递通道）。
/// >
/// > 由 `BedrockGunItemRenderer` 在 afterRender（第一人称主视图，含 item FOV 修正）期间写入：
/// > - **枪口姿态**：按手、主枪/下挂来源和 display 枪口序号隔离（多来源并存）；
/// >   渲染某手第一人称枪体前先清除该手，未在本帧渲染的来源不会沿用旧姿态。
/// > - **枪灯姿态**：单值（屏幕后处理单灯合成，多灯取第一个）；每帧收集前清除。
/// >
/// > 消费者（`AFTER_LEVEL` 手电后处理 / 曳光弹实体渲染 / 枪口火光后处理）直接读取当前帧姿态。
/// @author TACZ, USS_Shenzhou
public class FirstPersonPoseCache {
    private static final Map<MuzzleCacheKey, MuzzlePose> MUZZLES = new HashMap<>();

    private static boolean flashlightActive = false;
    @Nullable
    private static Vector3f flashlightFrom;
    @Nullable
    private static Vector3f flashlightForward;
    @Nullable
    private static float[] flashlightColor;
    private static float flashlightRange;

    /// > ============================ 枪口姿态 ============================

    public static void setMuzzle(InteractionHand hand, MuzzleReference reference,
                                 @Nullable Vector3f offset, @Nullable Vector3f forward) {
        MuzzleCacheKey key = new MuzzleCacheKey(hand, reference);
        if (offset == null || forward == null) {
            MUZZLES.remove(key);
            return;
        }
        MUZZLES.put(key, new MuzzlePose(offset, forward));
    }

    public static void clear(InteractionHand hand) {
        MUZZLES.keySet().removeIf(key -> key.hand == hand);
    }

    public static void clearAll() {
        MUZZLES.clear();
    }

    @Nullable
    public static MuzzlePose getMuzzle(InteractionHand hand, MuzzleReference reference) {
        return MUZZLES.get(new MuzzleCacheKey(hand, reference));
    }

    @Nullable
    public static Vector3f getMuzzleOffset(InteractionHand hand, MuzzleReference reference) {
        MuzzlePose pose = getMuzzle(hand, reference);
        return pose != null ? pose.offset() : null;
    }

    @Nullable
    public static Vector3f getMuzzleForward(InteractionHand hand, MuzzleReference reference) {
        MuzzlePose pose = getMuzzle(hand, reference);
        return pose != null ? pose.forward() : null;
    }

    /// > ============================ 枪灯姿态 ============================

    public static void setFlashlight(@Nullable Vector3f viewFrom, @Nullable Vector3f viewForward,
                                     @Nullable float[] rgb, float maxRange) {
        flashlightActive = viewFrom != null && viewForward != null && rgb != null;
        flashlightFrom = viewFrom;
        flashlightForward = viewForward;
        flashlightColor = rgb;
        flashlightRange = maxRange;
    }

    public static void clearFlashlight() {
        flashlightActive = false;
        flashlightFrom = null;
        flashlightForward = null;
        flashlightColor = null;
        flashlightRange = 0.0f;
    }

    public static boolean isFlashlightActive() {
        return flashlightActive;
    }

    @Nullable
    public static Vector3f getFlashlightFrom() {
        return flashlightFrom == null ? null : new Vector3f(flashlightFrom);
    }

    @Nullable
    public static Vector3f getFlashlightForward() {
        return flashlightForward == null ? null : new Vector3f(flashlightForward);
    }

    @Nullable
    public static float[] getFlashlightColor() {
        return flashlightColor == null ? null : flashlightColor.clone();
    }

    public static float getFlashlightRange() {
        return flashlightRange;
    }

    public record MuzzleCacheKey(InteractionHand hand, MuzzleReference reference) {
        public MuzzleCacheKey {
            hand = hand != null ? hand : InteractionHand.MAIN_HAND;
            reference = reference != null ? reference : MuzzleReference.primary();
        }
    }

    public record MuzzlePose(Vector3f offset, Vector3f forward) {
        public MuzzlePose {
            offset = new Vector3f(offset);
            forward = new Vector3f(forward);
        }

        @Override
        public Vector3f offset() {
            return new Vector3f(offset);
        }

        @Override
        public Vector3f forward() {
            return new Vector3f(forward);
        }
    }
}