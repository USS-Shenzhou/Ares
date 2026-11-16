package cn.ussshenzhou.ares.gun.custom.charm;

import cn.ussshenzhou.ares.gun.item.ICustomItem;
import cn.ussshenzhou.ares.gun.util.CustomItemDataHelper;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.joml.Vector3f;

/// > 单条「挂饰结合到枪体」的数据：挂在某骨骼表面的某点、朝某法线方向。
/// > <p>
/// > 仅承载单条记录的序列化（{@link #save()} / {@link #load(CompoundTag)}）。枪械 NBT 根键
/// > {@code TaczCharms.charms}（list）的读写集中在 {@link cn.ussshenzhou.ares.gun.util.GunCharmDataHelper}，
/// > 跨版本风险收敛于该 helper。
/// >
/// > @param charmStack  挂饰 ItemStack（含标准 {@link ICustomItem#CUSTOM_ID_TAG}）
/// > @param displayId   枪械 display id（绑定时记录，用于校验模型一致）
/// > @param boneName    结合到枪体的哪个骨骼
/// > @param localHit    骨骼局部坐标系下的命中点（吊挂锚点，像素）
/// > @param localNormal 局部命中面法线（决定挂饰朝外方向）
/// > @param localOffset 局部附加偏移
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW the charm stack is stored as ItemStackTemplate with charmStack() returning an ItemStack; save/load replaced with
// CODEC and STREAM_CODEC, defaults for missing fields unchanged.
public record GunCharmData(
        ItemStackTemplate charm,
        Identifier displayId,
        String boneName,
        Vector3f localHit,
        Vector3f localNormal,
        Vector3f localOffset
) {
    private static final Codec<Vector3f> VECTOR_CODEC = ExtraCodecs.VECTOR3F.xmap(Vector3f::new, vector -> vector);
    private static final StreamCodec<ByteBuf, Vector3f> VECTOR_STREAM_CODEC = ByteBufCodecs.VECTOR3F.map(Vector3f::new, vector -> vector);

    public static final Codec<GunCharmData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStackTemplate.CODEC.fieldOf("item").forGetter(GunCharmData::charm),
            Identifier.CODEC.optionalFieldOf("display_id", ICustomItem.EMPTY_ID).forGetter(GunCharmData::displayId),
            Codec.STRING.optionalFieldOf("bone", "").forGetter(GunCharmData::boneName),
            VECTOR_CODEC.optionalFieldOf("hit", new Vector3f()).forGetter(GunCharmData::localHit),
            VECTOR_CODEC.optionalFieldOf("normal", new Vector3f()).forGetter(GunCharmData::localNormal),
            VECTOR_CODEC.optionalFieldOf("offset", new Vector3f()).forGetter(GunCharmData::localOffset)
    ).apply(instance, GunCharmData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunCharmData> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC, GunCharmData::charm,
            Identifier.STREAM_CODEC, GunCharmData::displayId,
            ByteBufCodecs.STRING_UTF8, GunCharmData::boneName,
            VECTOR_STREAM_CODEC, GunCharmData::localHit,
            VECTOR_STREAM_CODEC, GunCharmData::localNormal,
            VECTOR_STREAM_CODEC, GunCharmData::localOffset,
            GunCharmData::new
    );

    public ItemStack charmStack() {
        return charm.create();
    }

    /// > 取挂饰的配置 id（标准 CustomItemId）。
    public Identifier charmId() {
        return CustomItemDataHelper.getCustomId(charmStack());
    }
}
