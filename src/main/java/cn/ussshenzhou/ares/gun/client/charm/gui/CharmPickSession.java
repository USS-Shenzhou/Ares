package cn.ussshenzhou.ares.gun.client.charm.gui;

import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

import java.util.Comparator;

/// > 改装界面挂饰拾取态的客户端状态机（纯客户端意图，绑定走服务端权威）。
/// > <p>
/// > 持有「是否处于拾取态」「当前选定的挂饰 id 及其背包槽位」「当前 hover 命中的 cube」。
/// > 渲染器据 {@link #isActive()} 决定是否填充 {@link CharmCubeScreenCache}；界面据
/// > {@link #hovered()} 画反馈、左键确认时发绑定消息。
/// @author TACZ, USS_Shenzhou
public class CharmPickSession {
    private static boolean active;
    @Nullable
    private static Identifier selectedCharmId;
    private static int selectedCharmInvSlot = -1;
    @Nullable
    private static ProjectedGunCube.Hit hovered;

    public static boolean isActive() {
        return active;
    }

    /// > 进入拾取态。
    public static void begin() {
        active = true;
        selectedCharmId = null;
        selectedCharmInvSlot = -1;
        hovered = null;
    }

    /// > 退出拾取态（清空缓存与选择）。
    public static void cancel() {
        active = false;
        selectedCharmId = null;
        selectedCharmInvSlot = -1;
        hovered = null;
        CharmCubeScreenCache.clear();
    }

    public static void select(Identifier charmId, int invSlot) {
        selectedCharmId = charmId;
        selectedCharmInvSlot = invSlot;
    }

    @Nullable
    public static Identifier selectedCharmId() {
        return selectedCharmId;
    }

    public static int selectedCharmInvSlot() {
        return selectedCharmInvSlot;
    }

    public static boolean hasSelection() {
        return selectedCharmId != null && selectedCharmInvSlot >= 0;
    }

    @Nullable
    public static ProjectedGunCube.Hit hovered() {
        return hovered;
    }

    /// > 用当前鼠标位置遍历 cube 缓存取最近命中，更新 hover。未选挂饰时不命中。
    public static void updateHover(double mouseX, double mouseY, float guiW, float guiH) {
        if (!active || !hasSelection()) {
            hovered = null;
            return;
        }
        hovered = CharmCubeScreenCache.all().stream()
                .map(cube -> cube.hit(mouseX, mouseY, guiW, guiH))
                .filter(java.util.Objects::nonNull)
                .min(Comparator.comparingDouble(ProjectedGunCube.Hit::distance))
                .orElse(null);
    }
}
