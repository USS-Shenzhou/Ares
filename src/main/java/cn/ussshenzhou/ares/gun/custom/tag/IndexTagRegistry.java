package cn.ussshenzhou.ares.gun.custom.tag;

import cn.ussshenzhou.ares.gun.resource.IndexTagLookup;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// > 单个索引标签语义域的不可变运行时快照。
/// @author TACZ, USS_Shenzhou
public class IndexTagRegistry implements IndexTagLookup {
    private static final IndexTagRegistry EMPTY = new IndexTagRegistry(Map.of(), Map.of(), Set.of());

    private final Map<Identifier, List<IndexTagEntry>> directEntries;
    private final Map<Identifier, Set<Identifier>> resolvedMembers;
    private final Map<Identifier, Set<Identifier>> memberTags;
    private final Set<Identifier> invalidTags;

    public IndexTagRegistry(
            Map<Identifier, ? extends List<IndexTagEntry>> directEntries,
            Map<Identifier, ? extends Set<Identifier>> resolvedMembers,
            Set<Identifier> invalidTags
    ) {
        this.directEntries = copyEntries(directEntries);
        this.resolvedMembers = copyResolvedMembers(resolvedMembers);
        this.memberTags = buildMemberTags(this.resolvedMembers);
        this.invalidTags = Collections.unmodifiableSet(new LinkedHashSet<>(invalidTags));
    }

    public static IndexTagRegistry empty() {
        return EMPTY;
    }

    @Override
    public boolean contains(Identifier tagId, Identifier memberId) {
        if (!isValid(tagId) || memberId == null) {
            return false;
        }
        return resolvedMembers.getOrDefault(tagId, Set.of()).contains(memberId);
    }

    @Override
    public boolean isKnown(Identifier tagId) {
        return tagId != null && (directEntries.containsKey(tagId) || invalidTags.contains(tagId));
    }

    @Override
    public boolean isValid(Identifier tagId) {
        return tagId != null && directEntries.containsKey(tagId) && !invalidTags.contains(tagId);
    }

    @Override
    public Set<Identifier> getMembers(Identifier tagId) {
        if (!isValid(tagId)) {
            return Set.of();
        }
        return resolvedMembers.getOrDefault(tagId, Set.of());
    }

    @Override
    public Set<Identifier> getTags(Identifier memberId) {
        if (memberId == null) {
            return Set.of();
        }
        return memberTags.getOrDefault(memberId, Set.of());
    }

    /// > 返回合并后的直接图，供网络同步与调试使用。
    public Map<Identifier, List<IndexTagEntry>> getDirectEntries() {
        return directEntries;
    }

    public Set<Identifier> getInvalidTags() {
        return invalidTags;
    }

    private static Map<Identifier, List<IndexTagEntry>> copyEntries(
            Map<Identifier, ? extends List<IndexTagEntry>> source) {
        Map<Identifier, List<IndexTagEntry>> result = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            result.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<Identifier, Set<Identifier>> copyResolvedMembers(
            Map<Identifier, ? extends Set<Identifier>> source) {
        Map<Identifier, Set<Identifier>> result = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableSet(new LinkedHashSet<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<Identifier, Set<Identifier>> buildMemberTags(
            Map<Identifier, Set<Identifier>> resolvedMembers) {
        Map<Identifier, LinkedHashSet<Identifier>> mutableResult = new LinkedHashMap<>();
        for (var entry : resolvedMembers.entrySet()) {
            Identifier tagId = entry.getKey();
            for (Identifier memberId : entry.getValue()) {
                mutableResult.computeIfAbsent(memberId, ignored -> new LinkedHashSet<>()).add(tagId);
            }
        }

        Map<Identifier, Set<Identifier>> result = new LinkedHashMap<>();
        for (var entry : mutableResult.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableSet(new LinkedHashSet<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(result);
    }
}
