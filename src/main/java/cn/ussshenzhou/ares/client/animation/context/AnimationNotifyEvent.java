package cn.ussshenzhou.ares.client.animation.context;

import net.minecraft.resources.Identifier;

/// @author TACZ, USS_Shenzhou
public record AnimationNotifyEvent(Identifier montageId, String sectionName, String trackName, String event, float keyTime) {
}
