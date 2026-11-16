package cn.ussshenzhou.ares.gun.client.render.fx;

/* FIXME rendering
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import cn.ussshenzhou.ares.gun.TimelessHelperC;
import cn.ussshenzhou.ares.gun.gameplay.MuzzleReference;
import cn.ussshenzhou.ares.gun.client.animation.statemachine.GunMolangContext;
import cn.ussshenzhou.ares.gun.client.resource.display.GunDisplayInstance;
import cn.ussshenzhou.ares.gun.util.MountedWeaponHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

/// > 激光束流视觉参数求值器。
/// > <p>
/// > 从射击者当前手持枪的 display（{@link GunDisplayInstance#getLaserBeam()}）取视觉表达式，
/// > 结合束流已存活时长（{@code query.beam_time}）求出本帧的颜色与芯/辉光半宽，供
/// > {@link LaserRenderer} 渲染。表达式共享动画状态机的 {@link GunMolangContext} 词汇。
/// > <p>
/// > 求值回退链：display 表达式 → display 静态值 → 包内回退色 / 渲染端内置默认常量。
/// > 任何缺失或异常都回退，绝不打断渲染。
/// > <p>
/// > <b>首版</b>两视角（本机第一人称 / 第三人称他人）都用独立 {@link GunMolangContext}：
/// > 只有 {@code query.beam_time} 有效，{@code gun.*} 查询因无 live 状态机返回 0（其他玩家本就无
/// > live 状态，合理）。要用 {@code gun.aim_progress} 等真实枪态驱动颜色，需复用本机活跃动画的
/// > live molang context——列为后续增强，避免首版为跨视角一致性引入复杂状态桥接。
/// @author TACZ, USS_Shenzhou
public class LaserVisualResolver {
    /// > 求本帧激光束流视觉参数。
    /// >
    /// > @param shooter     射击者（用于取其手持枪的 display）
    /// > @param hand        发射手
    /// > @param startTimeMs 束流起始时间戳（毫秒，{@link LaserBeamManager} 记录）
    /// > @param fallbackColor 包内回退色 RGB（每分量 0~1），display 未配 laser_beam 时用
    /// > @return 求值结果（颜色 + 芯/辉光半宽），永不为 null
    public static LaserVisual resolve(LivingEntity shooter, InteractionHand hand, MuzzleReference muzzleReference,
                                      long startTimeMs, float[] fallbackColor) {
        float[] color = fallbackColor != null ? fallbackColor : new float[]{1.0f, 0.0f, 0.0f};
        float coreHalfWidth = LaserRenderer.BEAM_CORE_HALF_WIDTH;
        float glowHalfWidth = LaserRenderer.BEAM_GLOW_HALF_WIDTH;
        float roll = 0.0f;

        GunDisplayInstance.LaserBeamVisual beam = getBeamVisual(shooter, hand, muzzleReference);
        if (beam == null) {
            return new LaserVisual(color, coreHalfWidth, glowHalfWidth, roll);
        }

        /// > 独立求值上下文：仅 beam_time 有效，gun.* 返回 0。
        GunMolangContext ctx = new GunMolangContext();
        double beamTime = Math.max(0.0, (System.currentTimeMillis() - startTimeMs) / 1000.0);
        ctx.setBeamTimeSeconds(beamTime);

        /// > 颜色：表达式（0xRRGGBB 解包）→ 静态色 → 回退色。
        Double packed = tryEvaluate(beam.colorExpr(), ctx);
        if (packed != null) {
            color = unpackColor(packed);
        } else if (beam.staticColor() != null) {
            color = beam.staticColor();
        }

        /// > 芯半宽：表达式 → 静态值 → 内置默认。
        Double core = tryEvaluate(beam.coreWidthExpr(), ctx);
        if (core != null) {
            coreHalfWidth = (float) Math.max(0.0, core);
        } else if (beam.coreWidth() != null) {
            coreHalfWidth = beam.coreWidth();
        }

        /// > 辉光半宽：表达式 → 静态值 → 内置默认。
        Double glow = tryEvaluate(beam.glowWidthExpr(), ctx);
        if (glow != null) {
            glowHalfWidth = (float) Math.max(0.0, glow);
        } else if (beam.glowWidth() != null) {
            glowHalfWidth = beam.glowWidth();
        }

        /// > 滚转角（度）：表达式 → 静态值 → 0。
        Double rollValue = tryEvaluate(beam.rollExpr(), ctx);
        if (rollValue != null) {
            roll = (float) (double) rollValue;
        } else if (beam.roll() != null) {
            roll = beam.roll();
        }

        return new LaserVisual(color, coreHalfWidth, glowHalfWidth, roll);
    }

    @Nullable
    private static GunDisplayInstance.LaserBeamVisual getBeamVisual(LivingEntity shooter, InteractionHand hand,
                                                                     MuzzleReference muzzleReference) {
        try {
            ItemStack stack = shooter.getItemInHand(hand);
            if (stack.isEmpty()) {
                return null;
            }
            if (muzzleReference != null && muzzleReference.underBarrel()) {
                stack = MountedWeaponHelper.findInstalledMountedWeapon(stack).orElse(ItemStack.EMPTY);
            }
            return stack.isEmpty() ? null : TimelessHelperC.getGunDisplay(stack)
                    .map(GunDisplayInstance::getLaserBeam).orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Nullable
    private static Double tryEvaluate(@Nullable MolangExpression expr, GunMolangContext ctx) {
        if (expr == null) {
            return null;
        }
        try {
            return expr.evaluate(ctx);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /// > 把 double 结果按 {@code 0xRRGGBB} 整数解包为 RGB（每分量 0~1）。
    private static float[] unpackColor(double packed) {
        int rgb = (int) Math.round(packed);
        return new float[]{
                ((rgb >> 16) & 0xFF) / 255.0f,
                ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f
        };
    }

    /// > 本帧激光束流视觉参数：颜色（RGB，每分量 0~1）+ 芯半宽 + 辉光半宽（米）+ 截面滚转角（度）。
    public record LaserVisual(float[] color, float coreHalfWidth, float glowHalfWidth, float roll) {}
}
*/
