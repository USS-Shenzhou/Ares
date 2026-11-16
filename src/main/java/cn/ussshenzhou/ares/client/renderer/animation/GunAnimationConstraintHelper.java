package cn.ussshenzhou.ares.client.renderer.animation;

/* FIXME rendering
import com.mojang.blaze3d.vertex.PoseStack;
*/
import com.maydaymemory.mae.basic.BoneTransform;
import com.maydaymemory.mae.basic.Pose;
import cn.ussshenzhou.ares.client.model.BedrockGunModel;
import cn.ussshenzhou.ares.gun.util.math.MathUtil;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/// > 将动画 constraint 骨骼的位移和旋转补偿应用到第一人称持枪姿态。
/// @author TACZ, USS_Shenzhou
public class GunAnimationConstraintHelper {
    /* FIXME rendering
    /// > 应用动画约束变换。
    /// >
    /// > @param weight 控制约束变换的权重，用于插值。
    public static void apply(PoseStack poseStack, BedrockGunModel gunModel, float weight, Pose currentPose,
                             InteractionHand hand) {
        if (!Float.isFinite(weight) || weight <= 0.0f) {
            return;
        }
        int constraintBoneIndex = gunModel.getIndex("constraint");
        if (constraintBoneIndex < 0) {
            return;
        }
        IFPAnimationInstance animationInstance = FirstPersonRenderHandler.getActiveAnimationInstance(hand);
        if (animationInstance == null) {
            return;
        }

        int[] nodePath = gunModel.getConstraintPath(constraintBoneIndex);
        if (nodePath.length == 0) {
            return;
        }

        Vector3f originTranslation = new Vector3f();
        Vector3f animatedTranslation = new Vector3f();
        Vector3f rotation = new Vector3f();
        Vector3f translationIca = getConstraintTranslationIca(currentPose, constraintBoneIndex);
        Vector3f rotationIca = getConstraintRotationIca(currentPose, constraintBoneIndex);

        getAnimationConstraintTransform(nodePath, gunModel, originTranslation, animatedTranslation, rotation);

        Vector3f inverseTranslation = new Vector3f(originTranslation).sub(animatedTranslation);
        inverseTranslation.mulDirection(poseStack.last().pose());
        inverseTranslation.mul(
                translationIca.x() - 1.0F,
                translationIca.y() - 1.0F,
                1.0F - translationIca.z()
        );

        Vector3f inverseRotation = new Vector3f(rotation).mul(
                rotationIca.x() - 1.0F,
                rotationIca.y() - 1.0F,
                rotationIca.z() - 1.0F
        );

        if (!animatedTranslation.isFinite() || !inverseTranslation.isFinite() || !inverseRotation.isFinite()) {
            return;
        }

        poseStack.translate(animatedTranslation.x(), animatedTranslation.y(), animatedTranslation.z());
        poseStack.mulPose(Axis.XP.rotation(inverseRotation.x() * weight));
        poseStack.mulPose(Axis.YP.rotation(inverseRotation.y() * weight));
        poseStack.mulPose(Axis.ZP.rotation(inverseRotation.z() * weight));
        poseStack.translate(-animatedTranslation.x(), -animatedTranslation.y(), -animatedTranslation.z());

        Matrix4f poseMatrix = poseStack.last().pose();
        poseMatrix.m30(poseMatrix.m30() - inverseTranslation.x() * weight);
        poseMatrix.m31(poseMatrix.m31() - inverseTranslation.y() * weight);
        poseMatrix.m32(poseMatrix.m32() + inverseTranslation.z() * weight);
    }
    */

    private static void getAnimationConstraintTransform(int[] nodePath, BedrockGunModel gunModel,
                                                        Vector3f originTranslation, Vector3f animatedTranslation,
                                                        Vector3f rotation) {
        Matrix4f animatedMatrix = new Matrix4f().identity();
        Matrix4f originMatrix = new Matrix4f().identity();
        int constraintBoneIndex = nodePath[nodePath.length - 1];

        for (int boneIndex : nodePath) {
            applyBoneTransform(originMatrix, boneIndex, gunModel, true);
            applyBoneTransform(animatedMatrix, boneIndex, gunModel, boneIndex == constraintBoneIndex);
        }

        animatedMatrix.getTranslation(animatedTranslation);
        originMatrix.getTranslation(originTranslation);

        Vector3f animatedRotation = MathUtil.getEulerAngles(animatedMatrix);
        Vector3f originRotation = MathUtil.getEulerAngles(originMatrix);
        rotation.set(animatedRotation.sub(originRotation));
    }

    private static void applyBoneTransform(Matrix4f matrix, int boneIndex, BedrockGunModel gunModel,
                                           boolean useBindPose) {
        Vector3f translation;
        Quaternionf boneRotation;
        var bone = gunModel.getBone(boneIndex);
        if (useBindPose) {
            BoneTransform bindTransform = gunModel.getBindTransform(boneIndex);
            if (bindTransform == null) {
                translation = bone == null ? new Vector3f() : new Vector3f(bone.x, bone.y, bone.z);
                boneRotation = bone == null ? new Quaternionf() : new Quaternionf(bone.rotation);
            } else {
                translation = new Vector3f(bindTransform.translation());
                boneRotation = new Quaternionf(bindTransform.rotation().asQuaternion());
            }
        } else {
            translation = bone == null ? new Vector3f() : new Vector3f(bone.x, bone.y, bone.z);
            boneRotation = bone == null ? new Quaternionf() : new Quaternionf(bone.rotation);
        }
        matrix.translate(translation.x() / 16.0F, translation.y() / 16.0F, translation.z() / 16.0F);
        matrix.rotate(boneRotation);
    }

    @Nullable
    private static BoneTransform getBindTransform(Pose bindPose, int boneIndex) {
        for (BoneTransform transform : bindPose.getBoneTransforms()) {
            if (transform.boneIndex() == boneIndex) {
                return transform;
            }
        }
        return null;
    }

    private static Vector3f getConstraintTranslationIca(Pose currentPose, int boneIndex) {
        BoneTransform currentTransform = getBindTransform(currentPose, boneIndex);
        if (currentTransform == null) {
            return new Vector3f(0.2F, 0.2F, 0.2F);
        }
        return new Vector3f(
                Mth.clamp(Math.abs(currentTransform.translation().x()), 0.0F, 1.0F),
                Mth.clamp(Math.abs(currentTransform.translation().y()), 0.0F, 1.0F),
                Mth.clamp(Math.abs(currentTransform.translation().z()), 0.0F, 1.0F)
        );
    }

    private static Vector3f getConstraintRotationIca(Pose currentPose, int boneIndex) {
        BoneTransform currentTransform = getBindTransform(currentPose, boneIndex);
        if (currentTransform == null) {
            return new Vector3f(0.2F, 0.2F, 0.2F);
        }
        Vector3f currentEuler = new Vector3f(currentTransform.rotation().asEulerAngle());
        return new Vector3f(
                Mth.clamp((float) Math.abs(Math.toDegrees(currentEuler.x())), 0.0F, 1.0F),
                Mth.clamp((float) Math.abs(Math.toDegrees(currentEuler.y())), 0.0F, 1.0F),
                Mth.clamp((float) Math.abs(Math.toDegrees(currentEuler.z())), 0.0F, 1.0F)
        );
    }
}
