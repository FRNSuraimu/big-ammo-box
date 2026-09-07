package com.bigammobox.registry;

import com.bigammobox.BigAmmoBoxMod;
import com.bigammobox.menu.AmmoBoxCaseMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, BigAmmoBoxMod.MOD_ID);
    public static final RegistryObject<MenuType<AmmoBoxCaseMenu>> AMMO_BOX_CASE = MENUS.register("ammo_box_case",
            () -> IForgeMenuType.create(AmmoBoxCaseMenu::fromNetwork));
    private ModMenus() {}
}
