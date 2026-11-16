package cn.ussshenzhou.ares.gun.client.gui;

/// Only the externally exposed toggle state of the TACZ debug HUD is kept; the overlay itself is not ported.
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW the GUI port keeps only this state and the renderer-facing state classes under client/refit and client/charm/gui.
public class DebugGunHudOverlay {

    private static boolean enabled = false;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void toggle() {
        enabled = !enabled;
    }
}
