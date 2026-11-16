package cn.ussshenzhou.ares.gun.custom;

import cn.ussshenzhou.ares.Ares;
import cn.ussshenzhou.ares.gun.item.AttachmentCandidate;
import cn.ussshenzhou.ares.gun.item.AttachmentCompatibilityHelper;
import cn.ussshenzhou.ares.gun.item.AttachmentCompatibilityResult;
import cn.ussshenzhou.ares.gun.item.AttachmentKind;
import cn.ussshenzhou.ares.gun.item.AttachmentSlot;
import cn.ussshenzhou.ares.gun.item.AttachmentTagRootHelper;
import cn.ussshenzhou.ares.gun.item.AttachmentType;
import cn.ussshenzhou.ares.gun.item.IAttachmentHolder;
import cn.ussshenzhou.ares.gun.resource.IndexTagLookup;
import cn.ussshenzhou.ares.gun.custom.attachment.AttachmentIndex;
import cn.ussshenzhou.ares.gun.custom.attachment.AttachmentData;
import cn.ussshenzhou.ares.gun.custom.attachment.ToggleConfig;
import cn.ussshenzhou.ares.gun.custom.gun.GunIndex;
import cn.ussshenzhou.ares.gun.custom.slotadapter.SlotAdapterIndex;
import cn.ussshenzhou.ares.gun.custom.slotadapter.SlotAdapterTarget;
import cn.ussshenzhou.ares.gun.custom.underbarrel.UnderBarrelGunIndex;
import cn.ussshenzhou.ares.gun.custom.variant.VariantIndex;
import net.minecraft.resources.Identifier;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/// > 使用本轮 Index map 与刚解析出的 Tag Registry 执行配件数据静态校验。
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW renamed from AttachmentDataValidator.
public class AttachmentDataValidationHelper {
    private static final Identifier UNDER_BARREL_TYPE = Ares.modLoc("under_barrel");
    private static final Comparator<Identifier> ID_ORDER = Comparator.comparing(Identifier::toString);

    public static List<String> validate(
            Map<Identifier, GunIndex> guns,
            Map<Identifier, AttachmentIndex> attachments,
            Map<Identifier, UnderBarrelGunIndex> underBarrels,
            Map<Identifier, SlotAdapterIndex> adapters,
            Map<Identifier, VariantIndex> variants,
            IndexTagLookup attachmentTags,
            Function<Identifier, AttachmentType> attachmentTypeLookup
    ) {
        Context context = new Context(
                guns, attachments, underBarrels, adapters, variants, attachmentTags, attachmentTypeLookup
        );
        context.validate();
        return List.copyOf(context.errors);
    }

    private static class Context {
        private final Map<Identifier, GunIndex> guns;
        private final Map<Identifier, AttachmentIndex> attachments;
        private final Map<Identifier, UnderBarrelGunIndex> underBarrels;
        private final Map<Identifier, SlotAdapterIndex> adapters;
        private final Map<Identifier, VariantIndex> variants;
        private final IndexTagLookup attachmentTags;
        private final Function<Identifier, AttachmentType> attachmentTypeLookup;
        private final LinkedHashSet<String> errors = new LinkedHashSet<>();
        private final Map<Identifier, Integer> subtreeDepths = new HashMap<>();
        private final Set<Identifier> reportedCycles = new HashSet<>();

        private Context(
                Map<Identifier, GunIndex> guns,
                Map<Identifier, AttachmentIndex> attachments,
                Map<Identifier, UnderBarrelGunIndex> underBarrels,
                Map<Identifier, SlotAdapterIndex> adapters,
                Map<Identifier, VariantIndex> variants,
                IndexTagLookup attachmentTags,
                Function<Identifier, AttachmentType> attachmentTypeLookup
        ) {
            this.guns = guns;
            this.attachments = attachments;
            this.underBarrels = underBarrels;
            this.adapters = adapters;
            this.variants = variants;
            this.attachmentTags = attachmentTags;
            this.attachmentTypeLookup = attachmentTypeLookup;
        }

        private void validate() {
            validateIdConflicts();
            validateDerivedRootTags();
            validateSlotTags();
            validateAdapters();
            validateToggles();
            validateDefaults();
            validateDefaultTreeDepths();
        }

        private void validateIdConflicts() {
            sortedIds(attachments.keySet()).forEach(id -> {
                if (underBarrels.containsKey(id)) {
                    error("Attachment ID exists in both normal attachment and under-barrel domains: " + id);
                }
                if (guns.containsKey(id) && !attachmentSlots(attachments.get(id)).isEmpty()) {
                    error("Holder ID exists in both gun and container attachment domains: " + id);
                }
            });
        }

        private void validateDerivedRootTags() {
            sortedIds(guns.keySet()).forEach(id ->
                    validateDerivedRoot(AttachmentTagRootHelper.holder(id), "gun=" + id));
            sortedEntries(attachments).forEach(entry -> {
                if (!attachmentSlots(entry.getValue()).isEmpty()) {
                    validateDerivedRoot(AttachmentTagRootHelper.holder(entry.getKey()),
                            "attachment=" + entry.getKey());
                }
            });
            sortedIds(adapters.keySet()).forEach(id ->
                    validateDerivedRoot(AttachmentTagRootHelper.adapter(id), "adapter=" + id));
        }

        private void validateDerivedRoot(Identifier root, String owner) {
            if (attachmentTags.isKnown(root) && !attachmentTags.isValid(root)) {
                error("Derived attachment root tag is invalid: " + owner + ", tag=" + root);
            }
        }

        private void validateSlotTags() {
            sortedEntries(guns).forEach(entry ->
                    validateSlotTags(entry.getKey(), gunSlots(entry.getValue())));
            sortedEntries(attachments).forEach(entry ->
                    validateSlotTags(entry.getKey(), attachmentSlots(entry.getValue())));
        }

        private void validateSlotTags(Identifier holderId, Map<Identifier, AttachmentSlot> slots) {
            sortedEntries(slots).forEach(entry -> {
                AttachmentSlot slot = entry.getValue();
                Identifier override = slot.allowAttachmentTag();
                if (override != null && !attachmentTags.isValid(override)) {
                    error("Slot allow_attachment_tag is missing or invalid: holder=" + holderId
                            + ", slot=" + entry.getKey() + ", tag=" + override);
                }
            });
        }

        private void validateAdapters() {
            sortedEntries(adapters).forEach(entry -> {
                Identifier adapterId = entry.getKey();
                SlotAdapterIndex adapter = entry.getValue();
                Set<TargetKey> targets = new HashSet<>();

                List<Identifier> allowTypes = adapter.getData().getAllowTypesOverride();
                if (allowTypes != null) {
                    allowTypes.stream().sorted(ID_ORDER).forEach(typeId -> {
                        if (attachmentTypeLookup.apply(typeId) == null) {
                            error("Slot adapter allow_types contains an unknown type: adapter=" + adapterId + ", type=" + typeId);
                        }
                    });
                }

                for (SlotAdapterTarget target : adapter.getTargets()) {
                    TargetKey key = new TargetKey(target.host(), target.slot());
                    if (!targets.add(key)) {
                        error("Slot adapter declares a duplicate target: adapter=" + adapterId + ", host="
                                + target.host() + ", slot=" + target.slot());
                    }
                    Map<Identifier, AttachmentSlot> hostSlots = resolveRawHostSlots(target.host());
                    if (hostSlots == null) {
                        error("Slot adapter target host is missing, not a holder, or ambiguous: adapter=" + adapterId
                                + ", host=" + target.host());
                    } else if (!hostSlots.containsKey(target.slot())) {
                        error("Slot adapter target slot does not exist: adapter=" + adapterId + ", host="
                                + target.host() + ", slot=" + target.slot());
                    }
                    if (target.allowAttachmentTag() != null
                            && !attachmentTags.isValid(target.allowAttachmentTag())) {
                        error("Adapter target allow_attachment_tag is missing or invalid: adapter=" + adapterId
                                + ", host=" + target.host() + ", slot=" + target.slot()
                                + ", tag=" + target.allowAttachmentTag());
                    }
                }
            });
        }

        private Map<Identifier, AttachmentSlot> resolveRawHostSlots(Identifier hostId) {
            GunIndex gun = guns.get(hostId);
            AttachmentIndex attachment = attachments.get(hostId);
            Map<Identifier, AttachmentSlot> attachmentSlots = attachment == null
                    ? Map.of()
                    : attachmentSlots(attachment);
            boolean gunHolder = gun != null;
            boolean attachmentHolder = attachment != null && !attachmentSlots.isEmpty();
            if (gunHolder == attachmentHolder) {
                return null;
            }
            return gunHolder ? gunSlots(gun) : attachmentSlots;
        }

        private void validateToggles() {
            sortedEntries(attachments).forEach(entry ->
                    validateToggle(entry.getKey(), entry.getValue().getToggle(), entry.getValue().getOffData()));
            sortedEntries(variants).forEach(entry ->
                    validateToggle(entry.getKey(), entry.getValue().getToggle(), entry.getValue().getOffData()));
        }

        private void validateToggle(Identifier ownerId, @Nullable ToggleConfig toggle,
                                    @Nullable AttachmentData offData) {
            if (toggle == null) {
                return;
            }
            String effect = toggle.getEffect();
            if (effect != null && !effect.isEmpty() && !"laser".equals(effect) && !"flashlight".equals(effect)) {
                error("Toggle effect is invalid: owner=" + ownerId + ", effect=" + effect
                        + " (expected laser, flashlight or empty)");
            }
            if (toggle.getOffData() != null && offData == null) {
                error("Toggle off_data is missing or failed to parse: owner=" + ownerId
                        + ", off_data=" + toggle.getOffData());
            }
        }

        private void validateDefaults() {
            sortedEntries(guns).forEach(entry -> validateHolderDefaults(
                    entry.getKey(), gunSlots(entry.getValue()), true
            ));
            sortedEntries(attachments).forEach(entry -> validateHolderDefaults(
                    entry.getKey(), attachmentSlots(entry.getValue()), false
            ));
        }

        private void validateHolderDefaults(Identifier holderId, Map<Identifier, AttachmentSlot> slots,
                                            boolean acceptsMountedWeapon) {
            sortedEntries(slots).forEach(entry -> {
                AttachmentSlot slot = entry.getValue();
                if (slot.required() && !slot.hasDefault()) {
                    error("Required slot has no default attachment: holder=" + holderId + ", slot=" + slot.id());
                }
                if (!slot.hasDefault()) {
                    return;
                }

                AttachmentCandidate candidate = resolveCandidate(slot.defaultAttachment(), new LinkedHashSet<>());
                if (candidate == null) {
                    return;
                }
                boolean structureAllowed = acceptsMountedWeapon || candidate.kind() != AttachmentKind.MOUNTED_WEAPON;
                AttachmentCompatibilityResult result = AttachmentCompatibilityHelper.check(
                        attachmentTags,
                        holderId,
                        slot,
                        slot,
                        null,
                        null,
                        candidate,
                        1,
                        structureAllowed
                );
                if (!result.compatible()) {
                    error("Default attachment is incompatible: holder=" + holderId + ", slot=" + slot.id() + ", attachment="
                            + candidate.id() + ", reason=" + result.failure() + ", root=" + result.rootTag());
                }
            });
        }

        private AttachmentCandidate resolveCandidate(Identifier id, LinkedHashSet<Identifier> visiting) {
            boolean normal = attachments.containsKey(id);
            boolean mounted = underBarrels.containsKey(id);
            if (normal && mounted) {
                error("Default attachment ID is ambiguous across domains: " + id);
                return null;
            }
            if (!normal && !mounted) {
                error("Default attachment does not exist: " + id);
                return null;
            }
            if (mounted) {
                AttachmentType type = attachmentTypeLookup.apply(UNDER_BARREL_TYPE);
                if (type == null) {
                    error("Mounted weapon attachment type is not registered: " + UNDER_BARREL_TYPE);
                    return null;
                }
                return new AttachmentCandidate(id, type, AttachmentKind.MOUNTED_WEAPON, 0);
            }
            AttachmentIndex attachment = attachments.get(id);
            int subtreeDepth = computeSubtreeDepth(id, visiting);
            return new AttachmentCandidate(id, attachment.getAttachmentType(), AttachmentKind.NORMAL, subtreeDepth);
        }

        private int computeSubtreeDepth(Identifier attachmentId, LinkedHashSet<Identifier> visiting) {
            Integer cached = subtreeDepths.get(attachmentId);
            if (cached != null) {
                return cached;
            }
            if (!visiting.add(attachmentId)) {
                if (reportedCycles.add(attachmentId)) {
                    List<Identifier> cycle = new ArrayList<>(visiting);
                    cycle.add(attachmentId);
                    error("Default attachment recursion contains a cycle: " + cycle);
                }
                return IAttachmentHolder.MAX_ATTACHMENT_DEPTH + 1;
            }

            AttachmentIndex attachment = attachments.get(attachmentId);
            if (attachment == null) {
                visiting.remove(attachmentId);
                return 0;
            }
            int max = 0;
            for (AttachmentSlot slot : attachmentSlots(attachment).values()) {
                if (!slot.hasDefault()) {
                    continue;
                }
                Identifier childId = slot.defaultAttachment();
                int childDepth;
                if (underBarrels.containsKey(childId)) {
                    childDepth = 0;
                } else if (attachments.containsKey(childId)) {
                    childDepth = computeSubtreeDepth(childId, visiting);
                } else {
                    continue;
                }
                max = Math.max(max, 1 + childDepth);
            }
            visiting.remove(attachmentId);
            subtreeDepths.put(attachmentId, max);
            return max;
        }

        private void validateDefaultTreeDepths() {
            sortedIds(attachments.keySet()).forEach(id -> {
                int depth = computeSubtreeDepth(id, new LinkedHashSet<>());
                if (depth + 1 > IAttachmentHolder.MAX_ATTACHMENT_DEPTH) {
                    error("Attachment virtual default subtree exceeds the maximum install depth: attachment=" + id
                            + ", subtreeDepth=" + depth);
                }
            });

            sortedEntries(guns).forEach(entry -> {
                int depth = 0;
                for (AttachmentSlot slot : gunSlots(entry.getValue()).values()) {
                    if (!slot.hasDefault()) {
                        continue;
                    }
                    Identifier childId = slot.defaultAttachment();
                    int childDepth = attachments.containsKey(childId)
                            ? computeSubtreeDepth(childId, new LinkedHashSet<>())
                            : 0;
                    depth = Math.max(depth, 1 + childDepth);
                }
                if (depth > IAttachmentHolder.MAX_ATTACHMENT_DEPTH) {
                    error("Gun virtual default tree exceeds the maximum depth: gun=" + entry.getKey()
                            + ", depth=" + depth);
                }
            });
        }

        private void error(String message) {
            errors.add(message);
        }
    }

    private record TargetKey(Identifier host, Identifier slot) {
    }

    private static Map<Identifier, AttachmentSlot> gunSlots(GunIndex index) {
        if (index == null || index.getData() == null || index.getData().getAttachmentSlots() == null) {
            return Map.of();
        }
        return index.getData().getAttachmentSlots().getSlots();
    }

    private static Map<Identifier, AttachmentSlot> attachmentSlots(AttachmentIndex index) {
        if (index == null || index.getData() == null || index.getData().getAttachmentSlots() == null) {
            return Map.of();
        }
        return index.getData().getAttachmentSlots().getSlots();
    }

    private static <T> List<Map.Entry<Identifier, T>> sortedEntries(Map<Identifier, T> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(ID_ORDER))
                .toList();
    }

    private static List<Identifier> sortedIds(Set<Identifier> ids) {
        return ids.stream().sorted(ID_ORDER).toList();
    }
}
