package cn.ussshenzhou.ares.logic.gun;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;

/**
 * @author USS_Shenzhou
 */
public abstract class BaseAttachmentMount extends BaseAttachment implements Mount{
    private final AttachmentContainer attachments = new AttachmentContainer();

    @Override
    public AttachmentContainer getAttachmentContainer() {
        return this.attachments;
    }
}
