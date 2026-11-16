package cn.ussshenzhou.ares.gun.attribute;

/// @author TACZ, USS_Shenzhou
public enum GunAttributeFormatter {
    NORMAL((modified, delta, positive) -> {
        String title;
        String c = positive ^ (delta > 0) ? "§c" : "§a";
        if (delta == 0) {
            title = String.format("%.2f", modified);
        } else if (delta > 0) {
            title = String.format("%.2f " + c + "(+%.2f)", modified, delta);
        } else {
            title = String.format("%.2f " + c + "(%.2f)", modified, delta);
        }
        return title;
    }),
    PERCENTAGE((modified, delta, positive) -> {
        String title;
        String c = positive ^ (delta > 0) ? "§c" : "§a";
        if (delta == 0) {
            title = String.format("%.1f%%", modified * 100);
        } else if (delta > 0) {
            title = String.format("%.2f%% " + c + "(+%.2f%%)", modified * 100, delta * 100);
        } else {
            title = String.format("%.2f%% " + c + "(%.2f%%)", modified * 100, delta * 100);
        }
        return title;
    }),
    ;

    private final AttributeFormatter formatter;

    GunAttributeFormatter(AttributeFormatter formatter) {
        this.formatter = formatter;
    }

    public String format(double modified, double delta, boolean positive) {
        return formatter.format(modified, delta, positive);
    }

    @FunctionalInterface
    public interface AttributeFormatter {
        String format(double modified, double delta, boolean positive);
    }
}
