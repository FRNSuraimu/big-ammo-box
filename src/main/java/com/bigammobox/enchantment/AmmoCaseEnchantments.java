package com.bigammobox.enchantment;

import com.bigammobox.config.BigAmmoBoxConfig;
import com.bigammobox.item.AmmoBoxCaseItem;
import com.bigammobox.item.AmmoBoxCaseTier;
import com.bigammobox.registry.ModEnchantments;
import com.bigammobox.storage.AmmoBoxCaseStorage;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** Centralized enchantment level selection and safety math for Ammo Box Box effects. */
public final class AmmoCaseEnchantments {
    private static final int MAX_MENDING_ROUNDS_PER_TRIGGER = 1_000_000;
    private static final double MAX_RELOAD_SPEED = 1_000_000.0D;

    private AmmoCaseEnchantments() {}

    public static int rawLevel(ItemStack stack, Enchantment enchantment) {
        if (stack.isEmpty() || enchantment == null) return 0;
        return Math.max(0, EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack));
    }

    public static int effectiveLevel(ItemStack stack, Enchantment enchantment) {
        int raw = rawLevel(stack, enchantment);
        if (raw <= 0) return 0;
        if (BigAmmoBoxConfig.RESPECT_ENCHANTMENT_MAX_LEVEL.get()) {
            return Math.min(raw, Math.max(1, enchantment.getMaxLevel()));
        }
        return raw;
    }

    public static boolean hasInfinity(ItemStack caseStack) {
        return BigAmmoBoxConfig.ALLOW_INFINITY_ON_AMMO_CASES.get()
                && rawLevel(caseStack, Enchantments.INFINITY_ARROWS) > 0;
    }

    public static int ammoRetentionLevel(ItemStack caseStack) {
        return effectiveLevel(caseStack, ModEnchantments.AMMO_RETENTION.get());
    }

    public static double ammoRetentionChance(ItemStack caseStack) {
        return Math.min(1.0D, Math.max(0.0D, ammoRetentionLevel(caseStack) * 0.05D));
    }

    public static int autoReloadLevel(ItemStack caseStack) {
        return effectiveLevel(caseStack, ModEnchantments.AUTO_RELOAD.get());
    }

    public static long autoReloadCooldownTicks(int level) {
        if (level <= 0) return Long.MAX_VALUE;
        if (level == 1) return 1_200L;
        if (level == 2) return 600L;
        if (level == 3) return 200L;
        double ticks = 200.0D / Math.pow(2.0D, Math.min(60, level - 3));
        if (!Double.isFinite(ticks) || ticks <= 1.0D) return 1L;
        return Math.max(1L, Math.round(ticks));
    }

    public static int ammoMagnetLevel(ItemStack caseStack) {
        return effectiveLevel(caseStack, ModEnchantments.AMMO_MAGNET.get());
    }

    public static double ammoMagnetRange(int level) {
        if (level <= 0) return 0.0D;
        return Math.min(64.0D, Math.max(0.0D, 4.0D * (double) level));
    }

    public static int mendingRounds(ItemStack caseStack) {
        int level = effectiveLevel(caseStack, Enchantments.MENDING);
        if (level <= 0) return 0;
        return Math.min(MAX_MENDING_ROUNDS_PER_TRIGGER, level);
    }

    public static int bulkStorageLevel(ItemStack caseStack) {
        return effectiveLevel(caseStack, ModEnchantments.BULK_AMMO_STORAGE.get());
    }

    public static int highestLevel(LivingEntity living, ItemStack gunStack, Enchantment enchantment) {
        if (!(living instanceof Player player)) return 0;
        int best = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (!(candidate.getItem() instanceof AmmoBoxCaseItem)) continue;
            if (!AmmoBoxCaseStorage.canSupplyGun(candidate, gunStack)) continue;
            best = Math.max(best, effectiveLevel(candidate, enchantment));
        }
        return best;
    }

    public static double quickChargeMultiplier(LivingEntity living, ItemStack gunStack) {
        int level = highestLevel(living, gunStack, Enchantments.QUICK_CHARGE);
        if (level <= 0) return 1.0D;
        double speed = 1.0D + 0.10D * (double) level;
        if (!Double.isFinite(speed)) return MAX_RELOAD_SPEED;
        return Math.min(MAX_RELOAD_SPEED, Math.max(1.0D, speed));
    }

    public static Effects resolveShootingEffects(LivingEntity living, ItemStack gunStack) {
        if (!(living instanceof Player player)) return Effects.NONE;
        int power = 0;
        int piercing = 0;
        boolean stellar = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (!(candidate.getItem() instanceof AmmoBoxCaseItem caseItem)) continue;
            if (!AmmoBoxCaseStorage.canSupplyGun(candidate, gunStack)) continue;
            power = Math.max(power, effectiveLevel(candidate, Enchantments.POWER_ARROWS));
            piercing = Math.max(piercing, effectiveLevel(candidate, Enchantments.PIERCING));
            if (caseItem.tier().ordinal() >= AmmoBoxCaseTier.NETHERITE.ordinal()
                    && rawLevel(candidate, ModEnchantments.STELLAR_OVERDRIVE.get()) > 0) {
                stellar = true;
            }
        }
        double powerMultiplier = 1.0D + Math.max(0, power) * 0.05D;
        if (!Double.isFinite(powerMultiplier) || powerMultiplier < 0.0D) powerMultiplier = Double.MAX_VALUE;
        double stellarMultiplier = stellar ? Math.max(0.0D, BigAmmoBoxConfig.STELLAR_OVERDRIVE_DAMAGE_MULTIPLIER.get()) : 1.0D;
        if (!Double.isFinite(stellarMultiplier)) stellarMultiplier = Double.MAX_VALUE;
        return new Effects(powerMultiplier, stellarMultiplier, piercing,
                Math.min(1.0D, Math.max(0.0D, piercing * 0.005D)),
                Math.max(0.0D, piercing * 0.01D));
    }

    public record Effects(double powerMultiplier, double stellarMultiplier, int piercingLevel,
                          double armorIgnoreBonus, double headshotMultiplierBonus) {
        public static final Effects NONE = new Effects(1.0D, 1.0D, 0, 0.0D, 0.0D);

        public double finalDamageMultiplier() {
            double value = powerMultiplier * stellarMultiplier;
            if (!Double.isFinite(value)) return Double.MAX_VALUE;
            return Math.max(0.0D, value);
        }
    }
}
