package cn.ussshenzhou.ares.gun.util;

import cn.ussshenzhou.ares.gun.custom.attribute.GunPropertiesCaches;
import cn.ussshenzhou.ares.gun.item.AttachmentCandidate;
import cn.ussshenzhou.ares.gun.item.AttachmentCompatibilityHelper;
import cn.ussshenzhou.ares.gun.item.AttachmentPath;
import cn.ussshenzhou.ares.gun.item.AttachmentSlot;
import cn.ussshenzhou.ares.gun.item.IAttachment;
import cn.ussshenzhou.ares.gun.item.IAttachmentHolder;
import cn.ussshenzhou.ares.gun.item.RailConfig;
import cn.ussshenzhou.ares.gun.TimelessHelperS;
import cn.ussshenzhou.ares.gun.custom.attachment.AttachmentData;
import cn.ussshenzhou.ares.gun.custom.attachment.AttachmentIndex;
import cn.ussshenzhou.ares.gun.custom.slotadapter.SlotAdapterTarget;
import cn.ussshenzhou.ares.gun.custom.underbarrel.UnderBarrelGunIndex;
import cn.ussshenzhou.ares.gun.custom.variant.VariantIndex;
import cn.ussshenzhou.ares.gun.init.ModCustomItemTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/// > 多层配件树的安装/卸载工具（双端共用纯逻辑，服务端权威）。
/// > <p>
/// > 关键点：子配件嵌在父配件的 NBT 中，深层写入必须「自底向上回写」——
/// > 修改末端 holder 的 NBT 后，逐层 setAttachment 回写到上一层，直到顶层枪械。
/// > 读取一律走 {@link IAttachmentHolder#getStoredAttachment}（NBT 直读），
/// > 避免触发 {@link GunPropertiesCaches} 的只读快照缓存。
/// @author TACZ, USS_Shenzhou
public class AttachmentTreeHelper {
    /// > 链条上的一层：holder 物品堆 + 在其父层用于回写的槽位。
    /// > rootSlot 为该 holder 自身被安装到上一层时所用的槽位；顶层枪械的 rootSlot 为 null。
    private record Layer(ItemStack holderStack, @Nullable AttachmentSlot mountSlot) {
    }

    /// > 在多层树的指定路径末端槽位安装一个配件。
    /// >
    /// > @param gun          顶层枪械物品堆（将被原地修改 NBT）
    /// > @param path         目标槽位路径（末端为要安装的槽位）
    /// > @param attachmentId 配件的注册 id
    /// > @return 是否安装成功
    public static boolean install(ItemStack gun, AttachmentPath path, Identifier attachmentId) {
        if (gun.isEmpty() || path.isEmpty()) {
            return false;
        }
        if (path.depth() > IAttachmentHolder.MAX_ATTACHMENT_DEPTH) {
            return false;
        }

        ItemStack attachment = buildAttachment(attachmentId);
        if (attachment == null) {
            return false;
        }

        /// > 解析从枪到目标父 holder 的链条
        List<Layer> chain = resolveChain(gun, path);
        if (chain == null) {
            return false;
        }

        Layer targetLayer = chain.get(chain.size() - 1);
        ItemStack holderStack = targetLayer.holderStack();
        IAttachmentHolder holder = asHolder(holderStack);
        if (holder == null) {
            return false;
        }

        Identifier targetSlotId = path.last();
        AttachmentSlot baseSlot = holder.getBaseAttachmentSlots(holderStack).get(targetSlotId);
        AttachmentSlot targetSlot = holder.getAttachmentSlots(holderStack).get(targetSlotId);
        if (baseSlot == null || targetSlot == null) {
            return false;
        }

        AttachmentCandidate candidate = AttachmentCandidate.fromStack(attachment);
        if (candidate == null) {
            return false;
        }
        Identifier adapterId = SlotAdapterHelper.getAdapter(holderStack, targetSlotId);
        Identifier hostId = CustomItemDataHelper.getCustomId(holderStack);
        SlotAdapterTarget adapterTarget = AdapterLookupHelper.getAdapterTarget(adapterId, hostId, targetSlotId);
        if (!AttachmentCompatibilityHelper.checkRuntime(
                holder, holderStack, baseSlot, targetSlot, adapterId, adapterTarget, candidate, path.depth()
        ).compatible()) {
            return false;
        }

        AttachmentDataHelper.setAttachment(holderStack, targetSlot, attachment);

        writeBack(gun, chain);
        return true;
    }

    /// > 微调多层树指定路径末端槽位上配件的导轨偏移（服务端权威）。
    /// > <p>
    /// > 偏移写入配件自身 NBT；若该槽位当前是虚拟默认（内置）件，会先实体化进 NBT 再写偏移。
    /// >
    /// > @param gun    顶层枪械物品堆（将被原地修改 NBT）
    /// > @param path   目标槽位路径（末端为要微调的槽位）
    /// > @param offset 沿导轨方向的偏移标量
    /// > @return 是否微调成功
    public static boolean adjust(ItemStack gun, AttachmentPath path, float offset) {
        if (gun.isEmpty() || path.isEmpty()) {
            return false;
        }

        List<Layer> chain = resolveChain(gun, path);
        if (chain == null) {
            return false;
        }

        Layer targetLayer = chain.get(chain.size() - 1);
        ItemStack holderStack = targetLayer.holderStack();
        IAttachmentHolder holder = asHolder(holderStack);
        if (holder == null) {
            return false;
        }

        Identifier targetSlotId = path.last();
        AttachmentSlot targetSlot = holder.getAttachmentSlots(holderStack).get(targetSlotId);
        if (targetSlot == null) {
            return false;
        }

        /// > 该槽位必须声明导轨，且偏移落在 datapack 声明的范围内（服务端权威）
        RailConfig rail = targetSlot.rail();
        if (rail == null || !rail.inRange(offset)) {
            return false;
        }

        /// > 取出当前槽位上的配件（含内置件实体化），写偏移后回写 holder
        ItemStack attachment = holder.getStoredAttachment(holderStack, targetSlot).copy();
        if (attachment.isEmpty()) {
            return false;
        }

        AttachmentOffsetHelper.setOffset(attachment, rail.clamp(offset));
        AttachmentDataHelper.setAttachment(holderStack, targetSlot, attachment);

        writeBack(gun, chain);
        return true;
    }

    /// > 把一个修改后的配件物品堆写回多层树指定路径末端槽位，并自底向上回写到顶层枪械（服务端权威）。
    /// > <p>
    /// > 与 {@link #install} 不同：此处不做安装校验（类型/高度/allow list），仅做「原位替换 + 回写」，
    /// > 用于玩法层把对子配件（如下挂武器）的弹药/上膛等运行时修改落地到宿主 NBT。
    /// > 调用方需保证 {@code mutated} 与该槽位原有配件同源（仅 NBT 状态变化）。
    /// >
    /// > @param gun     顶层枪械物品堆（将被原地修改 NBT）
    /// > @param path    目标槽位路径（末端为承载该配件的槽位）
    /// > @param mutated 修改后的配件物品堆
    /// > @return 是否成功
    public static boolean writeAttachmentBack(ItemStack gun, AttachmentPath path, ItemStack mutated) {
        if (gun.isEmpty() || path.isEmpty() || mutated == null || mutated.isEmpty()) {
            return false;
        }

        List<Layer> chain = resolveChain(gun, path);
        if (chain == null) {
            return false;
        }

        Layer targetLayer = chain.get(chain.size() - 1);
        ItemStack holderStack = targetLayer.holderStack();
        IAttachmentHolder holder = asHolder(holderStack);
        if (holder == null) {
            return false;
        }

        AttachmentSlot targetSlot = holder.getAttachmentSlots(holderStack).get(path.last());
        if (targetSlot == null) {
            return false;
        }

        AttachmentDataHelper.setAttachment(holderStack, targetSlot, mutated);
        writeBack(gun, chain);
        return true;
    }

    /// > 卸载多层树指定路径末端槽位上的配件。
    public static boolean uninstall(ItemStack gun, AttachmentPath path) {
        if (gun.isEmpty() || path.isEmpty()) {
            return false;
        }

        List<Layer> chain = resolveChain(gun, path);
        if (chain == null) {
            return false;
        }

        Layer targetLayer = chain.get(chain.size() - 1);
        ItemStack holderStack = targetLayer.holderStack();
        IAttachmentHolder holder = asHolder(holderStack);
        if (holder == null) {
            return false;
        }

        Identifier targetSlotId = path.last();
        AttachmentSlot targetSlot = holder.getAttachmentSlots(holderStack).get(targetSlotId);
        if (targetSlot == null) {
            return false;
        }

        /// > 必填槽：不允许卸成空。卸载语义=移除 NBT 覆盖，回退到虚拟默认件。
        if (targetSlot.required()) {
            /// > 必填但未声明默认件属于数据错误，禁止卸载以免变空。
            if (!targetSlot.hasDefault()) {
                return false;
            }
            /// > 当前若无 NBT 覆盖（已是虚拟默认），无可卸载。
            ItemStack override = AttachmentDataHelper.getAttachment(holderStack, targetSlot);
            if (override == null || override.isEmpty()) {
                return false;
            }
        }

        AttachmentDataHelper.removeAttachment(holderStack, targetSlot);

        writeBack(gun, chain);
        return true;
    }

    /// > 设置（或清除）多层树指定路径末端槽位上配件的变体（服务端权威）。
    /// > <p>
    /// > 变体写入该配件自身 NBT；variantId 为空表示恢复基础几何。
    /// >
    /// > @param gun       顶层枪械物品堆（将被原地修改 NBT）
    /// > @param path      目标槽位路径（末端槽位上的配件被设置变体）
    /// > @param variantId 变体 index id；为空表示清除变体
    /// > @return 是否成功
    public static boolean setVariant(ItemStack gun, AttachmentPath path, @Nullable Identifier variantId) {
        if (gun.isEmpty() || path.isEmpty()) {
            return false;
        }

        List<Layer> chain = resolveChain(gun, path);
        if (chain == null) {
            return false;
        }

        Layer targetLayer = chain.get(chain.size() - 1);
        ItemStack holderStack = targetLayer.holderStack();
        IAttachmentHolder holder = asHolder(holderStack);
        if (holder == null) {
            return false;
        }

        AttachmentSlot targetSlot = holder.getAttachmentSlots(holderStack).get(path.last());
        if (targetSlot == null) {
            return false;
        }

        /// > 取出当前槽位上的配件（含内置件实体化），它才是承载变体的对象
        ItemStack attachment = holder.getStoredAttachment(holderStack, targetSlot).copy();
        if (attachment.isEmpty()) {
            return false;
        }

        if (variantId == null) {
            VariantHelper.removeVariant(attachment);
        } else {
            /// > 服务端权威校验：变体必须声明适用于该配件 id
            Identifier attachmentId = CustomItemDataHelper.getCustomId(attachment);
            if (!AdapterLookupHelper.isVariantValidFor(variantId, attachmentId)) {
                return false;
            }
            VariantHelper.setVariant(attachment, variantId);
        }

        AttachmentDataHelper.setAttachment(holderStack, targetSlot, attachment);
        writeBack(gun, chain);
        return true;
    }

    /// > 设置多层树指定路径末端槽位上配件的开关状态（服务端权威）。
    /// > <p>
    /// > 开关仅对「当前生效形态声明了 toggle」的配件有效；切换为关闭态时校验关态数据
    /// > （当前形态的 off_data，缺失沿用形态自身数据）的子槽位仍能容纳当前已安装的子配件，
    /// > 否则拒绝切换并保持原状态，避免关态结构变化导致子配件 NBT 成孤儿。
    /// >
    /// > @param gun     顶层枪械物品堆（将被原地修改 NBT）
    /// > @param path    目标槽位路径（末端槽位上的配件被切换）
    /// > @param enabled 目标开关状态
    /// > @return 是否成功
    public static boolean setToggle(ItemStack gun, AttachmentPath path, boolean enabled) {
        if (gun.isEmpty() || path.isEmpty()) {
            return false;
        }

        List<Layer> chain = resolveChain(gun, path);
        if (chain == null) {
            return false;
        }

        Layer targetLayer = chain.get(chain.size() - 1);
        ItemStack holderStack = targetLayer.holderStack();
        IAttachmentHolder holder = asHolder(holderStack);
        if (holder == null) {
            return false;
        }

        AttachmentSlot targetSlot = holder.getAttachmentSlots(holderStack).get(path.last());
        if (targetSlot == null) {
            return false;
        }

        ItemStack attachment = holder.getStoredAttachment(holderStack, targetSlot).copy();
        if (attachment.isEmpty() || ToggleHelper.resolveActiveToggle(attachment) == null) {
            return false;
        }

        if (!enabled && !offStateKeepsChildren(attachment, holderStack, holder, targetSlot)) {
            return false;
        }

        ToggleHelper.setEnabled(attachment, enabled);
        AttachmentDataHelper.setAttachment(holderStack, targetSlot, attachment);
        writeBack(gun, chain);
        return true;
    }

    /// > 关态结构校验：预计关态生效的附件数据（当前形态的 off_data，缺失沿用形态自身数据）
    /// > 的子槽位集合必须覆盖当前已安装的子配件所在槽位。
    private static boolean offStateKeepsChildren(ItemStack attachment, ItemStack holderStack,
                                                 IAttachmentHolder holder, AttachmentSlot targetSlot) {
        AttachmentData offData = resolveOffStateData(attachment);
        if (offData == null) {
            /// > 关态沿用形态自身数据，槽位结构不变，无需校验
            return true;
        }
        Set<Identifier> offSlots = offData.getAttachmentSlots().getSlots().keySet();
        // FIXME REVIEW this iterates the slots of the parent holder, not the children of the toggled attachment itself; kept as in respawn.
        for (var entry : holder.getAttachmentSlots(holderStack).entrySet()) {
            ItemStack child = holder.getStoredAttachment(holderStack, entry.getValue());
            if (!child.isEmpty() && !offSlots.contains(entry.getKey())) {
                return false;
            }
        }
        return true;
    }

    /// > 预测「关闭态」生效的完整附件数据：当前生效形态（变体优先）的 off_data；
    /// > 未声明或不可开关返回 null（表示关态沿用形态自身数据）。
    @Nullable
    private static AttachmentData resolveOffStateData(ItemStack attachment) {
        Identifier variantId = VariantHelper.getVariant(attachment);
        if (variantId != null) {
            VariantIndex variant = TimelessHelperS.getIndex(variantId, ModCustomItemTypes.ATTACHMENT_VARIANT.get()).orElse(null);
            if (variant != null && variant.isToggleable()) {
                return variant.getOffData();
            }
            return null;
        }
        IAttachment iAttachment = IAttachment.asAttachment(attachment);
        if (iAttachment == null) {
            return null;
        }
        AttachmentIndex index = iAttachment.getIndex(attachment).orElse(null);
        if (index == null) {
            return null;
        }
        return index.getOffData();
    }

    /// > 遍历整棵配件树（深度受 {@link IAttachmentHolder#MAX_ATTACHMENT_DEPTH} 约束），
    /// > 收集「当前生效形态声明了指定效果开关」的配件（双端共用）。
    /// > <p>
    /// > 按键翻转为同一组目标各取反一次；改装界面/渲染判据也可基于本遍历。
    /// >
    /// > @param gun     顶层枪械物品堆
    /// > @param effect  目标效果（laser / flashlight）
    /// > @param consumer 回调（末端槽位路径, 配件物品堆）
    public static void forEachEffectToggleable(ItemStack gun, String effect,
                                               BiConsumer<AttachmentPath, ItemStack> consumer) {
        if (gun.isEmpty() || effect == null || effect.isEmpty()) {
            return;
        }
        forEachEffectToggleableRecursive(gun, AttachmentPath.EMPTY, effect, consumer, 0);
    }

    private static void forEachEffectToggleableRecursive(ItemStack holderStack, AttachmentPath path, String effect,
                                                         BiConsumer<AttachmentPath, ItemStack> consumer, int depth) {
        IAttachmentHolder holder = asHolder(holderStack);
        if (holder == null || depth >= IAttachmentHolder.MAX_ATTACHMENT_DEPTH) {
            return;
        }
        for (var entry : holder.getAttachmentSlots(holderStack).entrySet()) {
            ItemStack attachment = holder.getStoredAttachment(holderStack, entry.getValue());
            if (attachment.isEmpty()) {
                continue;
            }
            AttachmentPath childPath = path.append(entry.getKey());
            if (ToggleHelper.matchesEffect(attachment, effect)) {
                consumer.accept(childPath, attachment);
            }
            forEachEffectToggleableRecursive(attachment, childPath, effect, consumer, depth + 1);
        }
    }

    /// > 写入（或清除）多层树指定路径末端槽位上瞄具的倍率状态（服务端权威）。
    /// > <p>
    /// > 倍率写入该配件自身 NBT（{@link ZoomHelper}）：档位索引 + 无级进度（0~1，范围外 = 清除）。
    /// > 档位数服务端不可知，越界由客户端消费端 {@code % length} 兜底。
    /// >
    /// > @param gun         顶层枪械物品堆（将被原地修改 NBT）
    /// > @param path        目标槽位路径（末端槽位上的瞄具被写入）
    /// > @param zoomIndex   有级档位索引（&gt;= 0）
    /// > @param zoomProgress 无级进度（0~1 生效；其余清除无级进度）
    /// > @return 是否成功
    public static boolean setZoom(ItemStack gun, AttachmentPath path, int zoomIndex, float zoomProgress) {
        if (gun.isEmpty() || path.isEmpty() || zoomIndex < 0) {
            return false;
        }

        List<Layer> chain = resolveChain(gun, path);
        if (chain == null) {
            return false;
        }

        Layer targetLayer = chain.get(chain.size() - 1);
        ItemStack holderStack = targetLayer.holderStack();
        IAttachmentHolder holder = asHolder(holderStack);
        if (holder == null) {
            return false;
        }

        AttachmentSlot targetSlot = holder.getAttachmentSlots(holderStack).get(path.last());
        if (targetSlot == null) {
            return false;
        }

        ItemStack attachment = holder.getStoredAttachment(holderStack, targetSlot).copy();
        if (attachment.isEmpty()) {
            return false;
        }

        ZoomHelper.setZoomIndex(attachment, zoomIndex);
        if (zoomProgress >= 0.0F && zoomProgress <= 1.0F) {
            ZoomHelper.setZoomProgress(attachment, zoomProgress);
        } else {
            ZoomHelper.removeZoomProgress(attachment);
        }

        AttachmentDataHelper.setAttachment(holderStack, targetSlot, attachment);
        writeBack(gun, chain);
        return true;
    }

    /// > 设置（或清除）多层树指定路径末端槽位上的适配器（服务端权威）。
    /// > <p>
    /// > 适配器选择写入该槽位所属 holder 的 NBT，独立于该槽是否装有子配件（空槽也可设）。
    /// > <p>
    /// > 冲突检测：若该槽已装有配件，且应用新适配器后的有效 allow list 不再兼容已装配件类型，则拒绝
    /// > （要求玩家先卸下配件）。
    /// >
    /// > @param gun       顶层枪械物品堆（将被原地修改 NBT）
    /// > @param path      目标槽位路径（末端为要设置适配器的槽位）
    /// > @param adapterId 适配器 index id；为空表示清除适配器
    /// > @return 是否成功
    public static boolean setSlotAdapter(ItemStack gun, AttachmentPath path, @Nullable Identifier adapterId) {
        if (gun.isEmpty() || path.isEmpty()) {
            return false;
        }

        List<Layer> chain = resolveChain(gun, path);
        if (chain == null) {
            return false;
        }

        Layer targetLayer = chain.get(chain.size() - 1);
        ItemStack holderStack = targetLayer.holderStack();
        IAttachmentHolder holder = asHolder(holderStack);
        if (holder == null) {
            return false;
        }

        Identifier targetSlotId = path.last();
        AttachmentSlot baseSlot = holder.getBaseAttachmentSlots(holderStack).get(targetSlotId);
        if (baseSlot == null) {
            return false;
        }

        Identifier hostId = CustomItemDataHelper.getCustomId(holderStack);
        SlotAdapterTarget adapterTarget = adapterId == null
                ? null
                : AdapterLookupHelper.getAdapterTarget(adapterId, hostId, targetSlotId);
        if (adapterId != null && adapterTarget == null) {
            return false;
        }
        AttachmentSlot effectiveSlot = AttachmentSlotHelper.apply(baseSlot, adapterId);

        /// > 包含虚拟默认件；A -> B 与 A -> null 均只从 base slot 推导拟议状态。
        ItemStack installed = holder.getStoredAttachment(holderStack, baseSlot);
        if (!installed.isEmpty()) {
            AttachmentCandidate candidate = AttachmentCandidate.fromStack(installed);
            if (candidate == null || !AttachmentCompatibilityHelper.checkRuntime(
                    holder, holderStack, baseSlot, effectiveSlot, adapterId, adapterTarget, candidate, path.depth()
            ).compatible()) {
                return false;
            }
        }

        if (adapterId == null) {
            SlotAdapterHelper.removeAdapter(holderStack, targetSlotId);
        } else {
            SlotAdapterHelper.setAdapter(holderStack, targetSlotId, adapterId);
        }

        writeBack(gun, chain);
        return true;
    }

    /// > 解析从枪到目标父 holder 的链条。
    /// > <p>
    /// > 路径 {@code [a, b, c]} 表示：枪的 a 槽 → 配件的 b 槽 → 配件的 c 槽。
    /// > 末端槽位 c 是「目标槽位」，其父 holder 是「枪.a → 配件.b」所定位的那个配件。
    /// > 因此链条包含：枪（顶层）、枪.a 上的配件、(枪.a).b 上的配件……直到 path 倒数第二段所定位的 holder。
    /// >
    /// > @return 链条（首项为枪，末项为目标槽位所属的 holder）；路径非法返回 null
    @Nullable
    private static List<Layer> resolveChain(ItemStack gun, AttachmentPath path) {
        List<Layer> chain = new ArrayList<>();
        chain.add(new Layer(gun, null));

        ItemStack currentHolderStack = gun;
        /// > 遍历到倒数第二段：每一段都要「进入」对应槽位上的子配件
        for (int i = 0; i < path.depth() - 1; i++) {
            IAttachmentHolder holder = asHolder(currentHolderStack);
            if (holder == null) {
                return null;
            }
            Identifier slotId = path.slots().get(i);
            AttachmentSlot slot = holder.getAttachmentSlots(currentHolderStack).get(slotId);
            if (slot == null) {
                return null;
            }
            ItemStack child = holder.getStoredAttachment(currentHolderStack, slot);
            if (child.isEmpty()) {
                return null;
            }
            chain.add(new Layer(child, slot));
            currentHolderStack = child;
        }
        return chain;
    }

    /// > 自底向上回写：从最深层开始，把修改后的子 holder 重新 set 到上一层对应槽位，直到顶层枪械。
    private static void writeBack(ItemStack gun, List<Layer> chain) {
        /// > 配件树写入的唯一汇聚点：改完树后统一让属性缓存失效（属性来源含配件树及其开关 / 变体形态）。
        GunPropertiesCaches.invalidateCache(gun);

        /// > chain[0] 是枪本身，直接持有原 NBT，无需回写。
        /// > 从最深层往上：把 chain[i] 写回 chain[i-1] 的 mountSlot。
        for (int i = chain.size() - 1; i >= 1; i--) {
            Layer current = chain.get(i);
            Layer parent = chain.get(i - 1);
            IAttachmentHolder parentHolder = asHolder(parent.holderStack());
            AttachmentSlot mountSlot = current.mountSlot();
            if (parentHolder == null || mountSlot == null) {
                return;
            }
            /// > 直接写 NBT，不走 installAttachment 的校验（链条已校验过，且会刷新缓存）
            AttachmentDataHelper.setAttachment(parent.holderStack(), mountSlot, current.holderStack());
        }
    }

    @Nullable
    private static IAttachmentHolder asHolder(ItemStack stack) {
        if (stack.getItem() instanceof IAttachmentHolder holder) {
            return holder;
        }
        return null;
    }

    /// > 沿路径解析出目标 holder 物品堆（只读，供界面/查询使用）。
    /// > <p>
    /// > 与 {@link #resolveChain} 不同，这里把整条路径都视为「进入」，返回路径末端槽位上的那个 holder。
    /// > 空路径返回枪本身。路径非法或中途为空返回 null。
    @Nullable
    public static ItemStack resolveHolderStack(ItemStack gun, AttachmentPath path) {
        ItemStack current = gun;
        for (Identifier slotId : path.slots()) {
            IAttachmentHolder holder = asHolder(current);
            if (holder == null) {
                return null;
            }
            AttachmentSlot slot = holder.getAttachmentSlots(current).get(slotId);
            if (slot == null) {
                return null;
            }
            ItemStack child = holder.getStoredAttachment(current, slot);
            if (child.isEmpty()) {
                return null;
            }
            current = child;
        }
        return current;
    }

    /// > 根据注册 id 构建一个配件物品堆（带 CustomItemId）。
    /// > <p>
    /// > 优先按普通配件解析；若不存在则尝试按下挂武器（under_barrel）解析。
    /// > <p>
    /// > 注意：仅做 createStack + setCustomId，不读取属性缓存，可安全用于虚拟默认件的惰性构建。
    @Nullable
    public static ItemStack buildAttachment(Identifier attachmentId) {
        AttachmentIndex index = TimelessHelperS.getAttachmentIndex(attachmentId).orElse(null);
        if (index != null) {
            ItemStack stack = index.createStack(1);
            CustomItemDataHelper.setCustomId(stack, attachmentId);
            return stack;
        }
        /// > 下挂武器属于独立的 under_barrels 类型
        UnderBarrelGunIndex ubIndex = TimelessHelperS.getIndex(attachmentId, UnderBarrelGunIndex.TYPE).orElse(null);
        if (ubIndex != null) {
            ItemStack stack = ubIndex.createStack(1);
            CustomItemDataHelper.setCustomId(stack, attachmentId);
            return stack;
        }
        return null;
    }

    /// > 构建槽位声明的虚拟默认（内置）配件；无默认件返回 EMPTY。
    public static ItemStack resolveDefault(AttachmentSlot slot) {
        if (slot == null || !slot.hasDefault()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = buildAttachment(slot.defaultAttachment());
        return stack == null ? ItemStack.EMPTY : stack;
    }
}
