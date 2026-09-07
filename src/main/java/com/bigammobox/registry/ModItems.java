package com.bigammobox.registry;

import com.bigammobox.BigAmmoBoxMod;
import com.bigammobox.item.AmmoBoxCaseItem;
import com.bigammobox.item.AmmoBoxCaseTier;
import com.bigammobox.item.AmmoBoxTier;
import com.bigammobox.item.BigAmmoBoxItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BigAmmoBoxMod.MOD_ID);

    public static final RegistryObject<Item> COMPRESSED_NETHER_STAR = ITEMS.register("compressed_nether_star",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> NETHERITE_AMMO_BOX = ITEMS.register("netherite_ammo_box",
            () -> new BigAmmoBoxItem(AmmoBoxTier.NETHERITE));
    public static final RegistryObject<Item> NETHER_STAR_AMMO_BOX = ITEMS.register("nether_star_ammo_box",
            () -> new BigAmmoBoxItem(AmmoBoxTier.NETHER_STAR));
    public static final RegistryObject<Item> COMPRESSED_AMMO_BOX = ITEMS.register("compressed_ammo_box",
            () -> new BigAmmoBoxItem(AmmoBoxTier.COMPRESSED));
    public static final RegistryObject<Item> AMMO_BOX_21B = ITEMS.register("ammo_box_21b",
            () -> new BigAmmoBoxItem(AmmoBoxTier.BILLION_21));
    public static final RegistryObject<Item> AMMO_BOX_922_KEI = ITEMS.register("ammo_box_922_kei",
            () -> new BigAmmoBoxItem(AmmoBoxTier.KEI_922));
    public static final RegistryObject<Item> AMMO_BOX_340_KAN = ITEMS.register("ammo_box_340_kan",
            () -> new BigAmmoBoxItem(AmmoBoxTier.KAN_340));
    public static final RegistryObject<Item> AMMO_BOX_DOUBLE_MAX = ITEMS.register("ammo_box_double_max",
            () -> new BigAmmoBoxItem(AmmoBoxTier.DOUBLE_MAX));
    public static final RegistryObject<Item> AMMO_BOX_PRACTICAL_INFINITY = ITEMS.register("ammo_box_practical_infinity",
            () -> new BigAmmoBoxItem(AmmoBoxTier.PRACTICAL_INFINITY));

    public static final RegistryObject<Item> IRON_AMMO_BOX_CASE = ITEMS.register("iron_ammo_box_case",
            () -> new AmmoBoxCaseItem(AmmoBoxCaseTier.IRON));
    public static final RegistryObject<Item> GOLD_AMMO_BOX_CASE = ITEMS.register("gold_ammo_box_case",
            () -> new AmmoBoxCaseItem(AmmoBoxCaseTier.GOLD));
    public static final RegistryObject<Item> DIAMOND_AMMO_BOX_CASE = ITEMS.register("diamond_ammo_box_case",
            () -> new AmmoBoxCaseItem(AmmoBoxCaseTier.DIAMOND));
    public static final RegistryObject<Item> NETHERITE_AMMO_BOX_CASE = ITEMS.register("netherite_ammo_box_case",
            () -> new AmmoBoxCaseItem(AmmoBoxCaseTier.NETHERITE));
    public static final RegistryObject<Item> NETHER_STAR_AMMO_BOX_CASE = ITEMS.register("nether_star_ammo_box_case",
            () -> new AmmoBoxCaseItem(AmmoBoxCaseTier.NETHER_STAR));
    public static final RegistryObject<Item> COMPRESSED_AMMO_BOX_CASE = ITEMS.register("compressed_ammo_box_case",
            () -> new AmmoBoxCaseItem(AmmoBoxCaseTier.COMPRESSED));

    private ModItems() {}
}
