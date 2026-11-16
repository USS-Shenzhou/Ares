package cn.ussshenzhou.ares.gun.client.refit;

/* FIXME rendering
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
*/
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import cn.ussshenzhou.ares.gun.TimelessHelperC;
import cn.ussshenzhou.ares.gun.item.AttachmentPath;
import cn.ussshenzhou.ares.gun.item.AttachmentSlot;
import cn.ussshenzhou.ares.gun.item.IAttachment;
import cn.ussshenzhou.ares.gun.item.IAttachmentHolder;
import cn.ussshenzhou.ares.gun.item.IMountedWeapon;
import cn.ussshenzhou.ares.gun.client.model.BedrockAttachmentModel;
import cn.ussshenzhou.ares.gun.client.model.BedrockGunModel;
import cn.ussshenzhou.ares.gun.client.renderer.AttachmentMountHelper;
import cn.ussshenzhou.ares.gun.client.resource.display.AttachmentDisplayInstance;
import cn.ussshenzhou.ares.gun.client.resource.display.GunDisplayInstance;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/// > 收集改装界面所需的槽位和挂饰拾取屏幕投影缓存。
/// @author TACZ, USS_Shenzhou
public class RefitScreenCacheCollector {
    /* FIXME rendering
    /// > 将当前枪整棵配件树的槽位挂点投影到 GUI 屏幕坐标。
    public static void cacheSlotPositions(PoseStack poseStack, BedrockGunModel model,
                                          ItemStack stack, GunDisplayInstance displayInstance) {
        RefitSlotScreenCache.clear();
        IGun iGun = IGun.asGun(stack);
        if (iGun == null) {
            return;
        }
        Matrix4f viewMatrix = poseStack.last().pose();
        Matrix4f projMatrix = RenderSystem.getProjectionMatrix();
        Matrix4f viewProj = new Matrix4f(projMatrix).mul(viewMatrix);

        Minecraft mc = Minecraft.getInstance();
        double guiScale = mc.getWindow().getGuiScale();
        float guiW = (float) (mc.getWindow().getWidth() / guiScale);
        float guiH = (float) (mc.getWindow().getHeight() / guiScale);

        for (var entry : iGun.getAttachmentSlots(stack).entrySet()) {
            var slotDisplay = displayInstance.getSlot(entry.getKey());
            if (slotDisplay == null || slotDisplay.posGroup() == null) {
                continue;
            }
            Matrix4f mount = model.getGlobalTransform(slotDisplay.posGroup());
            if (mount == null) {
                continue;
            }
            mount = AttachmentMountHelper.applyRailOffset(mount, entry.getValue(),
                    iGun.getAttachment(stack, entry.getValue()));
            AttachmentPath path = AttachmentPath.of(entry.getKey());
            projectAndPut(path, mount, viewProj, guiW, guiH);

            cacheChildSlotPositions(iGun.getAttachment(stack, entry.getValue()), mount, path,
                    model, displayInstance, viewProj, guiW, guiH, IAttachmentHolder.MAX_ATTACHMENT_DEPTH - 1);
        }
    }
    */

    private static void cacheChildSlotPositions(ItemStack attachmentStack, Matrix4f parentMount, AttachmentPath parentPath,
                                                BedrockGunModel gunModel, GunDisplayInstance gunDisplay,
                                                Matrix4f viewProj, float guiW, float guiH, int remaining) {
        if (attachmentStack.isEmpty() || remaining <= 0) {
            return;
        }
        if (attachmentStack.getItem() instanceof IMountedWeapon) {
            return;
        }
        if (!(attachmentStack.getItem() instanceof IAttachmentHolder holder)) {
            return;
        }

        if (IAttachment.isBuiltinAttachment(attachmentStack)) {
            for (var entry : holder.getAttachmentSlots(attachmentStack).entrySet()) {
                AttachmentSlot slot = entry.getValue();
                var slotDisplay = gunDisplay.getSlot(slot.id());
                if (slotDisplay == null || slotDisplay.posGroup() == null) {
                    continue;
                }
                Matrix4f mount = gunModel.getGlobalTransform(slotDisplay.posGroup());
                if (mount == null) {
                    continue;
                }
                ItemStack child = holder.getStoredAttachment(attachmentStack, slot);
                mount = AttachmentMountHelper.applyRailOffset(mount, slot, child);
                AttachmentPath path = parentPath.append(slot.id());
                projectAndPut(path, mount, viewProj, guiW, guiH);
                cacheChildSlotPositions(child, mount, path, gunModel, gunDisplay,
                        viewProj, guiW, guiH, remaining - 1);
            }
            return;
        }

        AttachmentDisplayInstance attachmentDisplay = TimelessHelperC.getAttachmentDisplay(attachmentStack).orElse(null);
        if (attachmentDisplay == null || attachmentDisplay.getModel() == null) {
            return;
        }
        BedrockAttachmentModel attachmentModel = attachmentDisplay.getModel();
        for (var entry : holder.getAttachmentSlots(attachmentStack).entrySet()) {
            AttachmentSlot slot = entry.getValue();
            var slotDisplay = attachmentDisplay.getSlot(slot.id());
            if (slotDisplay == null || slotDisplay.posGroup() == null) {
                continue;
            }
            Matrix4f local = attachmentModel.getGlobalTransform(slotDisplay.posGroup());
            if (local == null) {
                continue;
            }
            ItemStack child = holder.getStoredAttachment(attachmentStack, slot);
            Matrix4f mount = new Matrix4f(parentMount).mul(local);
            mount = AttachmentMountHelper.applyRailOffset(mount, slot, child);
            AttachmentPath path = parentPath.append(slot.id());
            projectAndPut(path, mount, viewProj, guiW, guiH);
            cacheChildSlotPositions(child, mount, path, gunModel, gunDisplay,
                    viewProj, guiW, guiH, remaining - 1);
        }
    }

    /* FIXME rendering
    /// > 将枪体各 cube 投影到 GUI 屏幕，供改装态的挂饰拾取使用。
    public static void cacheCharmCubePositions(PoseStack poseStack, BedrockGunModel model) {
        CharmCubeScreenCache.clear();
        Matrix4f viewModel = poseStack.last().pose();
        Matrix4f projMatrix = RenderSystem.getProjectionMatrix();

        Minecraft mc = Minecraft.getInstance();
        double guiScale = mc.getWindow().getGuiScale();
        float guiW = (float) (mc.getWindow().getWidth() / guiScale);
        float guiH = (float) (mc.getWindow().getHeight() / guiScale);

        for (TreeBoneDefinition bone : model.getBaseModel().bones()) {
            ICube[] cubes = bone.cubes();
            if (cubes.length == 0 || !isBoneChainVisible(model, bone.index())) {
                continue;
            }
            Matrix4f boneGlobal = model.getGlobalTransform(bone.index());
            if (boneGlobal == null) {
                continue;
            }
            for (ICube cube : cubes) {
                Matrix4f cubeToBone = new Matrix4f();
                if (cube.hasRotation()) {
                    float[] pivot = cube.pivot();
                    cubeToBone.translate(pivot[0], pivot[1], pivot[2]);
                    cubeToBone.rotate(cube.rotation());
                    cubeToBone.translate(-pivot[0], -pivot[1], -pivot[2]);
                }
                Vector3f localMin = new Vector3f(cube.x(), cube.y(), cube.z());
                Vector3f localMax = new Vector3f(cube.x() + cube.width(), cube.y() + cube.height(), cube.z() + cube.depth());
                CharmCubeScreenCache.add(new ProjectedGunCube(bone.name(), viewModel, boneGlobal, cubeToBone,
                        projMatrix, localMin, localMax, guiW, guiH));
            }
        }
    }
    */

    private static void projectAndPut(AttachmentPath path, Matrix4f mount, Matrix4f viewProj, float guiW, float guiH) {
        Vector3f modelPos = mount.getTranslation(new Vector3f());
        Vector4f clip = viewProj.transform(new Vector4f(modelPos.x, modelPos.y, modelPos.z, 1.0f));
        if (clip.w() <= 1.0e-4f) {
            RefitSlotScreenCache.put(path, new RefitSlotScreenCache.SlotMarker(0, 0, false));
            return;
        }
        float ndcX = clip.x() / clip.w();
        float ndcY = clip.y() / clip.w();
        float ndcZ = clip.z() / clip.w();
        float screenX = (ndcX * 0.5f + 0.5f) * guiW;
        float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * guiH;
        boolean visible = ndcZ >= -1.0f && ndcZ <= 1.0f;
        RefitSlotScreenCache.put(path, new RefitSlotScreenCache.SlotMarker(screenX, screenY, visible));
    }

    private static boolean isBoneChainVisible(BedrockGunModel model, int boneIndex) {
        int current = boneIndex;
        while (current >= 0) {
            BoneState state = model.getBone(current);
            if (state != null && !state.visible) {
                return false;
            }
            current = model.getBaseModel().bone(current).parentIndex();
        }
        return true;
    }
}
