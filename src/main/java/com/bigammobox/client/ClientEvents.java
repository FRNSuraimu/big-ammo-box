package com.bigammobox.client;

import com.bigammobox.BigAmmoBoxMod;
import com.bigammobox.item.AmmoBoxCaseItem;
import com.bigammobox.item.BigAmmoBoxItem;
import com.bigammobox.registry.ModItems;
import com.bigammobox.registry.ModMenus;
import com.tacz.guns.api.item.IAmmoBox;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.awt.Color;

@Mod.EventBusSubscriber(modid = BigAmmoBoxMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.AMMO_BOX_CASE.get(), AmmoBoxCaseScreen::new);
            ResourceLocation loaded = new ResourceLocation(BigAmmoBoxMod.MOD_ID, "loaded");
            registerLoadedProperty(ModItems.NETHERITE_AMMO_BOX.get(), loaded);
            registerLoadedProperty(ModItems.NETHER_STAR_AMMO_BOX.get(), loaded);
            registerLoadedProperty(ModItems.COMPRESSED_AMMO_BOX.get(), loaded);
            registerLoadedProperty(ModItems.AMMO_BOX_21B.get(), loaded);
            registerLoadedProperty(ModItems.AMMO_BOX_922_KEI.get(), loaded);
            registerLoadedProperty(ModItems.AMMO_BOX_340_KAN.get(), loaded);
            registerLoadedProperty(ModItems.AMMO_BOX_DOUBLE_MAX.get(), loaded);
            registerLoadedProperty(ModItems.AMMO_BOX_PRACTICAL_INFINITY.get(), loaded);
        });
    }

    private static void registerLoadedProperty(Item item, ResourceLocation property) {
        ItemProperties.register(item, property, (stack, level, living, seed) -> {
            if (stack.getItem() instanceof IAmmoBox box && box.getAmmoCount(stack) > 0) {
                return 1.0F;
            }
            return 0.0F;
        });
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0 ? rainbow(5200L) : 0xFFFFFF,
                ModItems.COMPRESSED_NETHER_STAR.get());

        ItemColor ammoBoxColor = (stack, tintIndex) -> {
            if (!(stack.getItem() instanceof BigAmmoBoxItem item) || tintIndex != 0) return 0xFFFFFF;
            return item.tier().isGaming() ? rainbow(4200L) : item.tier().bodyColor();
        };
        event.register(ammoBoxColor,
                ModItems.NETHERITE_AMMO_BOX.get(), ModItems.NETHER_STAR_AMMO_BOX.get(),
                ModItems.COMPRESSED_AMMO_BOX.get(), ModItems.AMMO_BOX_21B.get(),
                ModItems.AMMO_BOX_922_KEI.get(), ModItems.AMMO_BOX_340_KAN.get(),
                ModItems.AMMO_BOX_DOUBLE_MAX.get(), ModItems.AMMO_BOX_PRACTICAL_INFINITY.get());

        ItemColor caseColor = (stack, tintIndex) -> {
            if (!(stack.getItem() instanceof AmmoBoxCaseItem item)) return 0xFFFFFF;
            if (tintIndex == 0) return item.tier().bodyColor();
            if (tintIndex == 1) return item.tier().isGamingAccent() ? rainbow(4600L) : item.tier().accentColor();
            return 0xFFFFFF;
        };
        event.register(caseColor,
                ModItems.IRON_AMMO_BOX_CASE.get(), ModItems.GOLD_AMMO_BOX_CASE.get(),
                ModItems.DIAMOND_AMMO_BOX_CASE.get(), ModItems.NETHERITE_AMMO_BOX_CASE.get(),
                ModItems.NETHER_STAR_AMMO_BOX_CASE.get(), ModItems.COMPRESSED_AMMO_BOX_CASE.get());
    }

    public static int rainbow(long periodMs) {
        float hue = (float) ((System.currentTimeMillis() % periodMs) / (double) periodMs);
        return Color.HSBtoRGB(hue, 0.82F, 1.0F) & 0xFFFFFF;
    }
}
