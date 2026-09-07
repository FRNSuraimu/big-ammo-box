package com.bigammobox.enchantment;

import net.minecraft.world.item.ItemStack;

/** Anvil-only endgame enchantment. It is deliberately absent from table/trade/book discovery. */
public final class StellarOverdriveEnchantment extends AmmoCaseEnchantment {
    public StellarOverdriveEnchantment() {
        super(Rarity.VERY_RARE, 1);
    }

    @Override
    public boolean isTreasureOnly() {
        return true;
    }

    @Override
    public boolean isTradeable() {
        return false;
    }

    @Override
    public boolean isDiscoverable() {
        return false;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return false;
    }
}
