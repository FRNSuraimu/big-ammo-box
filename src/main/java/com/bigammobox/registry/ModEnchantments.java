package com.bigammobox.registry;

import com.bigammobox.BigAmmoBoxMod;
import com.bigammobox.enchantment.AmmoCaseEnchantment;
import com.bigammobox.enchantment.AmmoRetentionEnchantment;
import com.bigammobox.enchantment.StellarOverdriveEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, BigAmmoBoxMod.MOD_ID);

    public static final RegistryObject<Enchantment> AMMO_RETENTION = ENCHANTMENTS.register(
            "ammo_retention", AmmoRetentionEnchantment::new);
    public static final RegistryObject<Enchantment> BULK_AMMO_STORAGE = ENCHANTMENTS.register(
            "bulk_ammo_storage", () -> new AmmoCaseEnchantment(Enchantment.Rarity.UNCOMMON, 1));
    public static final RegistryObject<Enchantment> AUTO_RELOAD = ENCHANTMENTS.register(
            "auto_reload", () -> new AmmoCaseEnchantment(Enchantment.Rarity.RARE, 3));
    public static final RegistryObject<Enchantment> AMMO_MAGNET = ENCHANTMENTS.register(
            "ammo_magnet", () -> new AmmoCaseEnchantment(Enchantment.Rarity.RARE, 3));
    public static final RegistryObject<Enchantment> STELLAR_OVERDRIVE = ENCHANTMENTS.register(
            "stellar_overdrive", StellarOverdriveEnchantment::new);

    private ModEnchantments() {}
}
