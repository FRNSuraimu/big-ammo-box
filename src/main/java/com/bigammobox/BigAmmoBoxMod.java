package com.bigammobox;

import com.bigammobox.config.BigAmmoBoxConfig;
import com.bigammobox.network.ModNetwork;
import com.bigammobox.registry.ModEnchantments;
import com.bigammobox.registry.ModItems;
import com.bigammobox.registry.ModMenus;
import com.bigammobox.registry.ModRecipes;
import com.bigammobox.registry.ModTabs;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(BigAmmoBoxMod.MOD_ID)
public final class BigAmmoBoxMod {
    public static final String MOD_ID = "big_ammo_box";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BigAmmoBoxMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModEnchantments.ENCHANTMENTS.register(bus);
        ModMenus.MENUS.register(bus);
        ModRecipes.SERIALIZERS.register(bus);
        ModTabs.TABS.register(bus);
        ModNetwork.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, BigAmmoBoxConfig.CLIENT_SPEC, "big_ammo_box-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BigAmmoBoxConfig.COMMON_SPEC, "big_ammo_box-common.toml");
        LOGGER.info("BIG AMMO BOX initialized for Minecraft 1.20.1");
    }
}
