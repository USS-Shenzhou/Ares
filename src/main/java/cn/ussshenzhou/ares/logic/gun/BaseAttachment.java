package cn.ussshenzhou.ares.logic.gun;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author USS_Shenzhou
 */
public abstract class BaseAttachment implements Attachment {

    private final Set<Class<? extends Mount>> canMountOn = new HashSet<>();

    public BaseAttachment(Class<? extends Mount>... canMountOn) {
        this.canMountOn.addAll(List.of(canMountOn));
    }

    @Override
    public boolean canMountOn(Class<? extends Mount> component) {
        return canMountOn.contains(component);
    }
}
