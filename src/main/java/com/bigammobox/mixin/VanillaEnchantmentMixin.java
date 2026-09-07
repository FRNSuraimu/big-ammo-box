package com.bigammobox.mixin;

import com.bigammobox.config.BigAmmoBoxConfig;
import com.bigammobox.enchantment.AmmoCaseEnchantments;
import com.bigammobox.item.AmmoBoxCaseItem;
import com.bigammobox.registry.ModEnchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allows the selected vanilla enchantments to treat Ammo Box Box cases as valid targets in every
 * vanilla path that consults Enchantment#canEnchant (notably enchanted-book anvils and /enchant).
 * The scope is deliberately case-only and enchantment-only; Stellar Overdrive remains anvil-special.
 */
@Mixin(Enchantment.class)
public abstract class VanillaEnchantmentMixin {
    @Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
    private void bigAmmoBox$allowSupportedCaseEnchantments(ItemStack stack,
                                                            CallbackInfoReturnable<Boolean> cir) {
        if (!(stack.getItem() instanceof AmmoBoxCaseItem)) return;
        Enchantment self = (Enchantment) (Object) this;
        if (self == Enchantments.POWER_ARROWS
                || self == Enchantments.QUICK_CHARGE
                || self == Enchantments.PIERCING
                || self == Enchantments.MENDING) {
            cir.setReturnValue(true);
            return;
        }
        if (self == Enchantments.INFINITY_ARROWS) {
            cir.setReturnValue(BigAmmoBoxConfig.ALLOW_INFINITY_ON_AMMO_CASES.get()
                    && AmmoCaseEnchantments.rawLevel(stack, ModEnchantments.AMMO_RETENTION.get()) <= 0);
        }
    }
}
