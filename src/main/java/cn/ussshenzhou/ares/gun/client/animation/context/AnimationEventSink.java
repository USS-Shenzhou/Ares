package cn.ussshenzhou.ares.gun.client.animation.context;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData;
import net.minecraft.resources.Identifier;

/// @author TACZ, USS_Shenzhou
public interface AnimationEventSink {
    void onNotify(AnimationNotifyEvent event);

    void onSound(Identifier soundId);

    /// > 动画 `particle_effects` 通道关键帧触发：请求生成一个粒子效果。
    /// >
    /// > 仅入队待发射的粒子数据，真正的发射器创建与枪口骨骼绑定由渲染层在拿到
    /// > 枪口真实变换时完成（见 `BedrockGunItemRenderer`）。
    void onParticle(ParticleEffectData data);

    AnimationEventSink NONE = new AnimationEventSink() {
        @Override
        public void onNotify(AnimationNotifyEvent event) {
        }

        @Override
        public void onSound(Identifier soundId) {
        }

        @Override
        public void onParticle(ParticleEffectData data) {
        }
    };
}
