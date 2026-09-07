package com.bigammobox.client.hud;

import com.bigammobox.config.BigAmmoBoxConfig;
import com.bigammobox.item.AmmoBoxCaseItem;
import com.bigammobox.item.BigAmmoBoxItem;
import com.bigammobox.storage.AmmoBoxCaseStorage;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;

/** Client-side read-only reserve-ammo aggregation for TaCZ's HUD. */
public final class HudAmmoState {
    private static final BigInteger INT_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
    private static BigInteger exactFiniteCount = BigInteger.ZERO;
    private static boolean active;
    private static boolean creative;
    private static int proxyCount;
    private static boolean inventoryAmmoMode;

    private HudAmmoState() {}

    public static void clear() {
        exactFiniteCount = BigInteger.ZERO;
        active = false;
        creative = false;
        proxyCount = 0;
        inventoryAmmoMode = false;
    }

    public static boolean inventoryNeedsBridge(Inventory inventory) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof BigAmmoBoxItem || stack.getItem() instanceof AmmoBoxCaseItem) {
                return true;
            }
            if (stack.getItem() instanceof IAmmoBox box
                    && (box.isAllTypeCreative(stack) || box.isCreative(stack))) {
                return true;
            }
        }
        return false;
    }

    public static int recount(ItemStack gunStack, Inventory inventory) {
        inventoryAmmoMode = gunStack.getItem() instanceof IGun gun && gun.useInventoryAmmo(gunStack);
        BigInteger total = BigInteger.ZERO;
        boolean foundCreative = false;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(gunStack, stack)) {
                total = total.add(BigInteger.valueOf(stack.getCount()));
            }

            if (stack.getItem() instanceof AmmoBoxCaseItem) {
                CountResult inner = countCase(gunStack, stack);
                if (inner.creative()) {
                    foundCreative = true;
                    break;
                }
                total = total.add(inner.count());
                continue;
            }

            if (stack.getItem() instanceof IAmmoBox box && box.isAmmoBoxOfGun(gunStack, stack)) {
                if (box.isAllTypeCreative(stack) || box.isCreative(stack)) {
                    foundCreative = true;
                    break;
                }
                total = total.add(exactCount(box, stack));
            }
        }

        active = true;
        creative = foundCreative;
        exactFiniteCount = foundCreative ? BigInteger.valueOf(9999) : total.max(BigInteger.ZERO);
        if (foundCreative) {
            proxyCount = 9999;
            return proxyCount;
        }
        proxyCount = exactFiniteCount.min(INT_MAX).intValue();
        return proxyCount;
    }

    private static CountResult countCase(ItemStack gunStack, ItemStack caseStack) {
        BigInteger total = BigInteger.ZERO;
        NonNullList<ItemStack> contents = AmmoBoxCaseStorage.read(caseStack);
        for (ItemStack inner : contents) {
            if (!(inner.getItem() instanceof IAmmoBox box)) continue;
            if (!box.isAmmoBoxOfGun(gunStack, inner)) continue;
            if (box.isAllTypeCreative(inner) || box.isCreative(inner)) {
                return new CountResult(BigInteger.ZERO, true);
            }
            if (AmmoBoxCaseStorage.providesInfiniteAmmo(caseStack, gunStack)) {
                return new CountResult(BigInteger.ZERO, true);
            }
            total = total.add(exactCount(box, inner));
        }
        return new CountResult(total, false);
    }

    private static BigInteger exactCount(IAmmoBox box, ItemStack stack) {
        if (stack.getItem() instanceof BigAmmoBoxItem bigBox) {
            return bigBox.getExactAmmoCount(stack);
        }
        return BigInteger.valueOf(Math.max(0, box.getAmmoCount(stack)));
    }

    public static String formatPrimaryForHud(String original) {
        return inventoryAmmoMode ? hugeTextOrOriginal(original) : original;
    }

    public static String formatReserveForHud(String original) {
        return inventoryAmmoMode ? original : hugeTextOrOriginal(original);
    }

    private static String hugeTextOrOriginal(String original) {
        if (!active) return original;
        if (creative) {
            return BigAmmoBoxConfig.SHOW_HUGE_HUD_COUNTS.get() ? "∞" : "9999";
        }
        if (exactFiniteCount.compareTo(BigInteger.valueOf(9999)) <= 0) return original;
        if (!BigAmmoBoxConfig.SHOW_HUGE_HUD_COUNTS.get()) return "9999+";
        if (exactFiniteCount.toString().length() <= 10) return exactFiniteCount.toString();
        return BigAmmoBoxItem.scientific(exactFiniteCount, 4);
    }

    private record CountResult(BigInteger count, boolean creative) {}
}
