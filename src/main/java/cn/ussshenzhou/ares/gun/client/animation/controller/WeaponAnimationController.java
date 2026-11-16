package cn.ussshenzhou.ares.gun.client.animation.controller;

import com.maydaymemory.mae.basic.Pose;

/// @author TACZ, USS_Shenzhou
public interface WeaponAnimationController {
    void draw();

    void putAway();

    void fire();

    void tick(float partialTicks);

    Pose evaluatePose();
}
