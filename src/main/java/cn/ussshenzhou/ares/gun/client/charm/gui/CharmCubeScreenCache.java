package cn.ussshenzhou.ares.gun.client.charm.gui;

import java.util.ArrayList;
import java.util.List;

/// > 缓存改装界面挂饰拾取态下枪体各 cube 投影到屏幕的记录（[ProjectedGunCube]）。
/// >
/// > 由 [cn.ussshenzhou.ares.gun.client.refit.RefitScreenCacheCollector] 在第一人称改装 + 挂饰拾取态时每帧重建：遍历枪模型所有骨骼
/// > 的 cube，构造投影记录写入本缓存。[cn.ussshenzhou.ares.gun.client.gui.GunRefitScreen] 据此做
/// > hover 命中判定、画线框 / 命中点 / 法线预览。每帧重建，仅拾取态有效。
/// @author TACZ, USS_Shenzhou
public class CharmCubeScreenCache {
    private static final List<ProjectedGunCube> CUBES = new ArrayList<>();

    public static void clear() {
        CUBES.clear();
    }

    public static void add(ProjectedGunCube cube) {
        CUBES.add(cube);
    }

    public static List<ProjectedGunCube> all() {
        return CUBES;
    }
}
