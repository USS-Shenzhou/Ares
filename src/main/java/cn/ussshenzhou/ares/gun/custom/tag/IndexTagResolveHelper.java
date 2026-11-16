package cn.ussshenzhou.ares.gun.custom.tag;

import net.minecraft.resources.Identifier;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/// > 将合并后的标签直接图解析为可 O(1) 查询的成员缓存。
/// @author TACZ, USS_Shenzhou
// FIXME REVIEW renamed from IndexTagResolver.
public class IndexTagResolveHelper {
    public static IndexTagRegistry resolve(
            Map<Identifier, ? extends List<IndexTagEntry>> directEntries,
            Set<Identifier> initiallyInvalidTags,
            Predicate<Identifier> memberValidator
    ) {
        return resolve(directEntries, initiallyInvalidTags, memberValidator, ignored -> {
        });
    }

    public static IndexTagRegistry resolve(
            Map<Identifier, ? extends List<IndexTagEntry>> directEntries,
            Set<Identifier> initiallyInvalidTags,
            Predicate<Identifier> memberValidator,
            Consumer<String> diagnostics
    ) {
        Objects.requireNonNull(directEntries, "directEntries");
        Objects.requireNonNull(initiallyInvalidTags, "initiallyInvalidTags");
        Objects.requireNonNull(memberValidator, "memberValidator");
        Objects.requireNonNull(diagnostics, "diagnostics");
        return new Resolver(directEntries, initiallyInvalidTags, memberValidator, diagnostics).resolveAll();
    }

    private enum ResolveState {
        UNVISITED,
        VISITING,
        DONE,
        INVALID
    }

    private static class Resolver {
        private final Map<Identifier, List<IndexTagEntry>> directEntries = new LinkedHashMap<>();
        private final Map<Identifier, Set<Identifier>> resolvedMembers = new LinkedHashMap<>();
        private final Map<Identifier, ResolveState> states = new LinkedHashMap<>();
        private final Set<Identifier> invalidTags = new LinkedHashSet<>();
        private final Predicate<Identifier> memberValidator;
        private final Consumer<String> diagnostics;
        private final Deque<Identifier> stack = new ArrayDeque<>();

        private Resolver(
                Map<Identifier, ? extends List<IndexTagEntry>> directEntries,
                Set<Identifier> initiallyInvalidTags,
                Predicate<Identifier> memberValidator,
                Consumer<String> diagnostics
        ) {
            for (var entry : directEntries.entrySet()) {
                this.directEntries.put(entry.getKey(), List.copyOf(entry.getValue()));
                states.put(entry.getKey(), ResolveState.UNVISITED);
            }
            invalidTags.addAll(initiallyInvalidTags);
            this.memberValidator = memberValidator;
            this.diagnostics = diagnostics;
        }

        private IndexTagRegistry resolveAll() {
            for (Identifier tagId : directEntries.keySet()) {
                resolve(tagId);
            }
            return new IndexTagRegistry(directEntries, resolvedMembers, invalidTags);
        }

        private Set<Identifier> resolve(Identifier tagId) {
            if (!directEntries.containsKey(tagId)) {
                return null;
            }
            if (invalidTags.contains(tagId)) {
                states.put(tagId, ResolveState.INVALID);
                return null;
            }

            ResolveState state = states.getOrDefault(tagId, ResolveState.UNVISITED);
            if (state == ResolveState.DONE) {
                return resolvedMembers.get(tagId);
            }
            if (state == ResolveState.INVALID) {
                return null;
            }
            if (state == ResolveState.VISITING) {
                markCycle(tagId);
                return null;
            }

            states.put(tagId, ResolveState.VISITING);
            stack.addLast(tagId);
            boolean invalid = false;
            LinkedHashSet<Identifier> members = new LinkedHashSet<>();
            for (IndexTagEntry entry : directEntries.get(tagId)) {
                if (!entry.tagReference()) {
                    if (!memberValidator.test(entry.id())) {
                        diagnostics.accept("标签 " + tagId + " 引用了当前语义域中不存在的索引 ID: " + entry.id());
                        invalid = true;
                        continue;
                    }
                    members.add(entry.id());
                    continue;
                }

                if (!directEntries.containsKey(entry.id())) {
                    diagnostics.accept("标签 " + tagId + " 引用了不存在的标签: #" + entry.id());
                    invalid = true;
                    continue;
                }
                Set<Identifier> nestedMembers = resolve(entry.id());
                if (nestedMembers == null) {
                    invalid = true;
                    continue;
                }
                members.addAll(nestedMembers);
            }
            stack.removeLast();

            if (invalid || invalidTags.contains(tagId)) {
                invalidTags.add(tagId);
                states.put(tagId, ResolveState.INVALID);
                return null;
            }

            Set<Identifier> immutableMembers = Collections.unmodifiableSet(new LinkedHashSet<>(members));
            resolvedMembers.put(tagId, immutableMembers);
            states.put(tagId, ResolveState.DONE);
            return immutableMembers;
        }

        private void markCycle(Identifier repeatedTag) {
            List<Identifier> cycle = stack.stream()
                    .dropWhile(tagId -> !tagId.equals(repeatedTag))
                    .collect(Collectors.toList());
            if (cycle.isEmpty()) {
                cycle = List.of(repeatedTag);
            }
            cycle.forEach(tagId -> {
                invalidTags.add(tagId);
                states.put(tagId, ResolveState.INVALID);
            });
            diagnostics.accept("检测到标签循环引用: " + cycle.stream()
                    .map(Identifier::toString)
                    .collect(Collectors.joining(" -> ")) + " -> " + repeatedTag);
        }
    }
}
