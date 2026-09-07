package com.bigammobox.mixin;

import com.bigammobox.storage.AmmoBoxCaseStorage;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.tacz.guns.api.item.gun.AbstractGunItem", remap = false)
public abstract class AbstractGunItemMixin {
    private static final String CAP_TARGET = "Lnet/minecraft/world/entity/LivingEntity;getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;";

    /**
     * TaCZ normally scans only the player's direct item handler. After TaCZ has
     * consumed direct ammo, consume any remainder from BIG AMMO BOX cases.
     */
    @Inject(method = "findAndExtractInventoryAmmo", at = @At("RETURN"), cancellable = true, require = 0)
    private void bigAmmoBox$extractFromCases(IItemHandler handler, ItemStack gunStack, int requested,
                                              CallbackInfoReturnable<Integer> cir) {
        int direct = Math.max(0, cir.getReturnValue());
        int remaining = Math.max(0, requested - direct);
        if (remaining <= 0) return;
        int fromCases = AmmoBoxCaseStorage.extractFromCases(handler, gunStack, remaining);
        if (fromCases > 0) {
            cir.setReturnValue(direct + fromCases);
        }
    }

    /**
     * This injection point is after TaCZ has already checked gun fullness,
     * reload mode, infinite reload and dummy ammo. It only replaces the final
     * "inventory contains compatible ammo" decision.
     */
    @Inject(method = "canReload",
            at = @At(value = "INVOKE", target = CAP_TARGET, shift = At.Shift.BEFORE, remap = false),
            cancellable = true, require = 0)
    private void bigAmmoBox$caseCanReload(LivingEntity entity, ItemStack gunStack,
                                           CallbackInfoReturnable<Boolean> cir) {
        boolean found = entity.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .map(handler -> AmmoBoxCaseStorage.handlerHasCompatibleCase(handler, gunStack))
                .orElse(false);
        if (found) cir.setReturnValue(true);
    }

    /** Same compatibility bridge for TaCZ's inventory-ammo presence query. */
    @Inject(method = "hasInventoryAmmo",
            at = @At(value = "INVOKE", target = CAP_TARGET, shift = At.Shift.BEFORE, remap = false),
            cancellable = true, require = 0)
    private void bigAmmoBox$caseHasAmmo(LivingEntity entity, ItemStack gunStack, boolean needCheck,
                                         CallbackInfoReturnable<Boolean> cir) {
        boolean found = entity.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .map(handler -> AmmoBoxCaseStorage.handlerHasCompatibleCase(handler, gunStack))
                .orElse(false);
        if (found) cir.setReturnValue(true);
    }
}
