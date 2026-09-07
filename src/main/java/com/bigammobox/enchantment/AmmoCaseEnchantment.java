package com.bigammobox.enchantment;

import com.bigammobox.item.AmmoBoxCaseItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/** Base class for ordinary enchantments that are meaningful only on Ammo Box Box cases. */
public class AmmoCaseEnchantment extends Enchantment {
    public static final EnchantmentCategory CASE_CATEGORY = EnchantmentCategory.create(
            "big_ammo_box_case", item -> item instanceof AmmoBoxCaseItem);

    private final int maxLevel;

    public AmmoCaseEnchantment(Rarity rarity, int maxLevel) {
        super(rarity, CASE_CATEGORY, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
        this.maxLevel = Math.max(1, maxLevel);
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public int getMinCost(int level) {
        return 5 + Math.max(0, level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 25;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof AmmoBoxCaseItem;
    }
}
