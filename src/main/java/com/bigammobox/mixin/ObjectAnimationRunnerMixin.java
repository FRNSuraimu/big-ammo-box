package com.bigammobox.mixin;

import com.bigammobox.client.reload.ClientQuickChargeState;
import com.tacz.guns.api.client.animation.ObjectAnimation;
import com.tacz.guns.api.client.animation.ObjectAnimationRunner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = ObjectAnimationRunner.class, remap = false)
public abstract class ObjectAnimationRunnerMixin {
    @Shadow private ObjectAnimation animation;

    @ModifyArg(method = "update(Z)V",
            at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/client/animation/ObjectAnimationRunner;updateProgress(J)V"),
            index = 0, require = 0)
    private long bigAmmoBox$quickChargeFirstPerson(long deltaNs) {
        if (animation == null || !ClientQuickChargeState.isReloadAnimationName(animation.name)) return deltaNs;
        float speed = ClientQuickChargeState.localReloadSpeed();
        if (speed <= 1.0F || deltaNs <= 0L) return deltaNs;
        double accelerated = deltaNs * (double) speed;
        if (!Double.isFinite(accelerated) || accelerated >= Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.max(deltaNs, (long) accelerated);
    }
}
