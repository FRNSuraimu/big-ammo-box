package com.bigammobox.mixin;

import com.bigammobox.enchantment.AmmoCaseEnchantments;
import com.bigammobox.reload.ClientReloadSpeedSnapshot;
import com.bigammobox.server.reload.ServerQuickChargeState;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public abstract class ModernKineticGunScriptApiMixin {
    @Shadow private LivingEntity shooter;
    @Shadow private ItemStack itemStack;

    @Inject(method = "getReloadTime", at = @At("RETURN"), cancellable = true, require = 0)
    private void bigAmmoBox$quickChargeReloadTime(CallbackInfoReturnable<Long> cir) {
        if (shooter == null || itemStack == null || itemStack.isEmpty()) return;
        double live = AmmoCaseEnchantments.quickChargeMultiplier(shooter, itemStack);
        double speed = shooter.level().isClientSide
                ? ClientReloadSpeedSnapshot.current(shooter, live)
                : ServerQuickChargeState.current(shooter, live);
        if (speed <= 1.0D) return;
        long original = Math.max(0L, cir.getReturnValue());
        double accelerated = original * speed;
        long safe = !Double.isFinite(accelerated) || accelerated >= Long.MAX_VALUE
                ? Long.MAX_VALUE : Math.max(original, (long) accelerated);
        cir.setReturnValue(safe);
    }
}
