package cn.ussshenzhou.ares.gun.client.animation.montage;

import cn.ussshenzhou.ares.gun.client.animation.context.AnimationEventSink;
import net.minecraft.resources.Identifier;

/// > MAE 蒙太奇运行时的上下文，供 notify 回调把事件转发到 [AnimationEventSink]。
/// @author TACZ, USS_Shenzhou
public class MontageContext {
    private final Identifier montageId;
    private final AnimationEventSink eventSink;

    public MontageContext(Identifier montageId, AnimationEventSink eventSink) {
        this.montageId = montageId;
        this.eventSink = eventSink != null ? eventSink : AnimationEventSink.NONE;
    }

    public Identifier montageId() {
        return montageId;
    }

    public AnimationEventSink eventSink() {
        return eventSink;
    }
}
