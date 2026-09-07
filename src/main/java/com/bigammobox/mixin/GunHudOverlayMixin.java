package com.bigammobox.mixin;

import com.bigammobox.client.hud.HudAmmoState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.tacz.guns.client.gui.overlay.GunHudOverlay", remap = false)
public abstract class GunHudOverlayMixin {
    @Shadow private static int cacheInventoryAmmoCount;

    /**
     * Keep TaCZ completely untouched unless BIG AMMO BOX items are actually present.
     * When present, reproduce its inventory scan with BigInteger-aware case traversal.
     */
    @Inject(method = "handleInventoryAmmo", at = @At("HEAD"), cancellable = true, require = 0)
    private static void bigAmmoBox$handleInventoryAmmo(ItemStack gunStack, Inventory inventory, CallbackInfo ci) {
        if (!HudAmmoState.inventoryNeedsBridge(inventory)) {
            HudAmmoState.clear();
            return;
        }
        cacheInventoryAmmoCount = HudAmmoState.recount(gunStack, inventory);
        ci.cancel();
    }

    /**
     * TaCZ clamps the numeric value to 9999 before formatting it, so replacing DecimalFormat output
     * is too late and is also fragile across branches.  Replace only the already-built text at the
     * actual drawString boundary instead.  The first drawString is the large/current counter.
     */
    @ModifyArg(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;FFIZ)I",
                    ordinal = 0, remap = false),
            index = 1, require = 0)
    private String bigAmmoBox$replacePrimaryAmmoText(String original) {
        return HudAmmoState.formatPrimaryForHud(original);
    }

    /**
     * The second drawString is TaCZ's reserve-ammo line for ordinary magazine-fed guns.
     * For useInventoryAmmo guns TaCZ leaves this line empty, so HudAmmoState routes the huge value
     * to the first draw call instead.
     */
    @ModifyArg(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;FFIZ)I",
                    ordinal = 1, remap = false),
            index = 1, require = 0)
    private String bigAmmoBox$replaceReserveAmmoText(String original) {
        return HudAmmoState.formatReserveForHud(original);
    }
}
