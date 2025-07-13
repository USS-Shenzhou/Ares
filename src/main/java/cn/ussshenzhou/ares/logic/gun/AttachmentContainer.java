package cn.ussshenzhou.ares.logic.gun;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * @author USS_Shenzhou
 */
@ParametersAreNonnullByDefault
public class AttachmentContainer {
    private final ArrayList<Attachment> attachments = new ArrayList<>();

    public ArrayList<Attachment> getAttachments() {
        return attachments;
    }

    @Nullable
    public Attachment findFirst(Class<? extends Attachment> attachment) {
        for (Attachment att : attachments) {
            if (att.getClass().equals(attachment)) {
                return att;
            }
        }
        return null;
    }

    public ArrayList<Attachment> findAll(Class<? extends Attachment> attachment) {
        ArrayList<Attachment> result = new ArrayList<>();
        for (Attachment att : attachments) {
            if (att.getClass().equals(attachment)) {
                result.add(att);
            }
        }
        return result;
    }

    @Nullable
    public Attachment findFirstRecursive(Class<? extends Attachment> attachment) {
        for (Attachment att : attachments) {
            if (att.getClass().equals(attachment)) {
                return att;
            }
        }
        for (Attachment att : attachments) {
            if (att instanceof Mount mount) {
                var toReturn = mount.getAttachmentContainer().findFirst(attachment);
                if (toReturn != null) {
                    return toReturn;
                }
            }
        }
        return null;
    }

    public ArrayList<Attachment> findAllRecursive(Class<? extends Attachment> attachment) {
        ArrayList<Attachment> result = new ArrayList<>();
        for (Attachment att : attachments) {
            if (att.getClass().equals(attachment)) {
                result.add(att);
            }
        }
        for (Attachment att : attachments) {
            if (att instanceof Mount mount) {
                result.addAll(mount.getAttachmentContainer().findAllRecursive(attachment));
            }
        }
        return result;
    }

    public void attach(Attachment attachment) {
        var iterator = attachments.listIterator();
        while (iterator.hasNext()) {
            var next = iterator.next();
            if (attachment.getPriority() < next.getPriority()) {
                iterator.previous();
                iterator.add(attachment);
                return;
            }
        }
        attachments.add(attachment);
    }

    public boolean detach(Attachment attachment) {
        return attachments.remove(attachment);
    }

    @Nullable
    public Attachment detach(Class<? extends Attachment> attachment) {
        var toRemove = this.findFirst(attachment);
        if (toRemove != null) {
            this.detach(toRemove);
            return toRemove;
        }
        return null;
    }

    public void foreach(Consumer<Attachment> action) {
        this.attachments.forEach(action);
    }
}
