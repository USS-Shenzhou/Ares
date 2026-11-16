package cn.ussshenzhou.ares.gun.client.animation.mounted;

/// @author TACZ, USS_Shenzhou
public interface HostOverlayBridge {
    HostOverlayBridge NONE = new HostOverlayBridge() {
    };

    default void playHostOverlayOnce(String animation) {
    }

    default void playHostOverlayScaled(String animation, float expectedDurationSeconds) {
    }

    default void stopHostOverlay(String actionName) {
    }
}
