package com.bigammobox.enchantment;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** Chance to provide a requested round without consuming it from the case. */
public final class AmmoRetentionEnchantment extends AmmoCaseEnchantment {
    public AmmoRetentionEnchantment() {
        super(Rarity.UNCOMMON, 3);
    }


    @Override
    public boolean canEnchant(ItemStack stack) {
        return super.canEnchant(stack)
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, stack) <= 0;
    }

    @Override
    protected boolean checkCompatibility(Enchantment other) {
        return other != Enchantments.INFINITY_ARROWS && super.checkCompatibility(other);
    }
}
