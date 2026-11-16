package cn.ussshenzhou.ares.gun.custom.charm;

import java.util.List;

/// > 挂饰模型 layout：把挂饰 bedrock 模型拆成「固定环 + 动态环 + 摆体」用于铰链摆动。
/// > <p>
/// > 端无关的纯数据，随 {@link CharmData} 一起加载/同步。各字段为挂饰模型内的骨骼名约定，
/// > 渲染/物理阶段据此解析模型各部件。
/// @author TACZ, USS_Shenzhou
public record CharmModelLayout(
        String anchorPart,
        String fixedPivotPart,
        String dynamicPivotPart,
        List<String> fixedParts,
        List<String> dynamicParts,
        List<String> collisionParts
) {
    public static final CharmModelLayout LEGACY = new CharmModelLayout("", "", "", List.of(), List.of(), List.of());
    public static final CharmModelLayout ROD_DOUBLE_RING = new CharmModelLayout(
            "gun_anchor",
            "gun_ring",
            "charm_ring",
            List.of("attachment", "gun_ring"),
            List.of("charm_ring", "body"),
            List.of()
    );

    public CharmModelLayout {
        anchorPart = anchorPart == null ? "" : anchorPart;
        fixedPivotPart = fixedPivotPart == null ? "" : fixedPivotPart;
        dynamicPivotPart = dynamicPivotPart == null ? "" : dynamicPivotPart;
        fixedParts = fixedParts == null ? List.of() : List.copyOf(fixedParts);
        dynamicParts = dynamicParts == null ? List.of() : List.copyOf(dynamicParts);
        collisionParts = collisionParts == null ? List.of() : List.copyOf(collisionParts);
    }

    public boolean isLayered() {
        return !fixedParts.isEmpty() || !dynamicParts.isEmpty();
    }
}
