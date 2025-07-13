package cn.ussshenzhou.ares.logic.gun;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import org.jetbrains.annotations.Nullable;

/**
 * @author USS_Shenzhou
 */
public class Receiver implements Mount {
    private final AttachmentContainer attachments = new AttachmentContainer();

    @Override
    public AttachmentContainer getAttachmentContainer() {
        return this.attachments;
    }
}
