package cn.ussshenzhou.ares.gun.resource;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.AnimationKeyframes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.CubesItem;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectKeyframes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.SoundEffectKeyframes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.serialize.AnimationKeyframesSerializer;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.serialize.IdentifierSerializer;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.serialize.ParticleEffectKeyframesSerializer;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.serialize.SoundEffectKeyframesSerializer;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.serialize.Vector3fSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cn.ussshenzhou.ares.gun.item.AttachmentSlot;
import cn.ussshenzhou.ares.gun.item.RailConfig;
import cn.ussshenzhou.ares.client.animation.mounted.OverlayWeightCfg;
import cn.ussshenzhou.ares.client.animation.mounted.serialize.OverlayWeightCfgSerializer;
import cn.ussshenzhou.ares.gun.custom.gun.FireModeEntry;
import cn.ussshenzhou.ares.gun.custom.AttachmentSlotInfo;
import cn.ussshenzhou.ares.gun.custom.AttributeModifierMap;
import cn.ussshenzhou.ares.gun.resource.serialize.AttachmentSlotInfoSerializer;
import cn.ussshenzhou.ares.gun.resource.serialize.AttachmentSlotSerializer;
import cn.ussshenzhou.ares.gun.resource.serialize.FireModeEntrySerializer;
import cn.ussshenzhou.ares.gun.resource.serialize.RailConfigSerializer;
import cn.ussshenzhou.ares.gun.resource.serialize.Vec3Serializer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW renamed from GsonUtil.
public class GunGsonHelper {
    public static final Gson GSON = new GsonBuilder()
            // FIXME REVIEW 26.3 has no ResourceLocation.Serializer, so the IdentifierSerializer of SBM is used. The hooks
            // deserializer is removed.
            .registerTypeAdapter(Identifier.class, new IdentifierSerializer())
            .registerTypeAdapter(Vec3.class, new Vec3Serializer())
            .registerTypeAdapter(CubesItem.class, new CubesItem.Deserializer())
            .registerTypeAdapter(Vector3f.class, new Vector3fSerializer())
            .registerTypeAdapter(AnimationKeyframes.class, new AnimationKeyframesSerializer())
            .registerTypeAdapter(SoundEffectKeyframes.class, new SoundEffectKeyframesSerializer())
            .registerTypeAdapter(ParticleEffectKeyframes.class, new ParticleEffectKeyframesSerializer())
            .registerTypeAdapter(AttachmentSlot.class, new AttachmentSlotSerializer())
            .registerTypeAdapter(RailConfig.class, new RailConfigSerializer())
            .registerTypeAdapter(AttachmentSlotInfo.class, new AttachmentSlotInfoSerializer())
            .registerTypeAdapter(AttributeModifierMap.class, new AttributeModifierMap.Deserializer())
            .registerTypeAdapter(FireModeEntry.class, new FireModeEntrySerializer())
            .registerTypeAdapter(OverlayWeightCfg.class, new OverlayWeightCfgSerializer())
            .create();

}
