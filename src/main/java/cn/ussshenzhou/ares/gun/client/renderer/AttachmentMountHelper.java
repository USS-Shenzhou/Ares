package cn.ussshenzhou.ares.gun.client.renderer;

import cn.ussshenzhou.ares.gun.item.AttachmentSlot;
import cn.ussshenzhou.ares.gun.item.RailConfig;
import cn.ussshenzhou.ares.gun.util.AttachmentOffsetHelper;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/// > 配件挂点变换工具。
/// @author TACZ, USS_Shenzhou
public class AttachmentMountHelper {
    /// > 将导轨微调偏移叠加到挂点变换上。
    public static Matrix4f applyRailOffset(Matrix4f base, AttachmentSlot slot, ItemStack attachment) {
        RailConfig rail = slot.rail();
        if (rail == null) {
            return base;
        }
        float offset = AttachmentOffsetHelper.getOffset(attachment);
        if (offset == 0f) {
            return base;
        }
        Vector3f delta = rail.offsetVector(rail.clamp(offset), new Vector3f());
        return new Matrix4f(base).translate(delta);
    }
}
