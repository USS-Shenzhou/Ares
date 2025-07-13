package cn.ussshenzhou.ares.logic.gun;

import java.util.Comparator;

/**
 * @author USS_Shenzhou
 */
public interface Attachment {
    boolean canMountOn(Class<? extends Mount> component);

    /**
     *
     * @return The priority of data processing during a firing. Smaller number will be handled earlier.
     * For a basic component, priority should be N times of 1000.
     */
    int getPriority();
}
