package cn.ussshenzhou.ares.gun.client.animation.mounted;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.control.montage.AnimationMontage;
import cn.ussshenzhou.ares.gun.client.animation.montage.MontageContext;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

/// > 解析后的下挂 overlay 运行时数据：在资源重载阶段按解析链（combo profile + mounted template）
/// > 预先构建，所有动画已绑定到具体 host 模型骨架。供 {@link HostOverlayRuntime} 直接使用。
/// > <p>
/// > 解析链：combo profile 显式声明 {@link MountedCompatibilityProfile#grip} 持枪姿态（per-combo），
/// > mounted template 提供操作差量蒙太奇与持枪混合规则（per-mounted 复用）。
/// @author TACZ, USS_Shenzhou
public class MountedOverlayResolved {
    /// > 持枪姿态（绝对），已绑定 host 模型骨架。layered 混合在 movement 之前注入。
    public final BedrockAnimation gripAnimation;

    /// > 持枪姿态层混合模式（layered / additive）。
    public final String gripBlendMode;

    /// > 持枪姿态层 per-hand 权重分组。
    public final List<MountedOverlayTemplate.GripGroup> gripGroups;

    /// > 操作差量动画集（已绑定 host 模型骨架），由下挂状态机按动画名直接触发。
    public final Map<String, BedrockAnimation> operationAnimations;

    /// > 旧操作差量蒙太奇（已绑定 host 模型骨架）。保留兼容，优先使用 operationAnimations。
    @Nullable
    public final AnimationMontage<MontageContext> operationMontage;

    /// > 切到下挂模式的过渡段差量动画（已绑定 host 模型骨架），additive 叠加。可为 null（无专门过渡动画，走纯权重淡入淡出）。
    @Nullable
    public final BedrockAnimation transitionToUnderBarrel;

    /// > 切回主枪模式的过渡段差量动画（已绑定 host 模型骨架），additive 叠加。可为 null。
    @Nullable
    public final BedrockAnimation transitionToPrimary;

    public MountedOverlayResolved(BedrockAnimation gripAnimation,
                                  String gripBlendMode,
                                  List<MountedOverlayTemplate.GripGroup> gripGroups,
                                  Map<String, BedrockAnimation> operationAnimations,
                                  @Nullable AnimationMontage<MontageContext> operationMontage,
                                  @Nullable BedrockAnimation transitionToUnderBarrel,
                                  @Nullable BedrockAnimation transitionToPrimary) {
        this.gripAnimation = gripAnimation;
        this.gripBlendMode = gripBlendMode != null ? gripBlendMode : "layered";
        this.gripGroups = gripGroups != null ? gripGroups : List.of();
        this.operationAnimations = operationAnimations != null ? java.util.Map.copyOf(operationAnimations) : java.util.Map.of();
        this.operationMontage = operationMontage;
        this.transitionToUnderBarrel = transitionToUnderBarrel;
        this.transitionToPrimary = transitionToPrimary;
    }
}
