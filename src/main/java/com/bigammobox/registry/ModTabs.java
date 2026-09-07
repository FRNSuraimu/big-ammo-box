package com.bigammobox.registry;

import com.bigammobox.BigAmmoBoxMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BigAmmoBoxMod.MOD_ID);
    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.big_ammo_box"))
            .icon(() -> ModItems.COMPRESSED_AMMO_BOX.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.COMPRESSED_NETHER_STAR.get());
                output.accept(ModItems.NETHERITE_AMMO_BOX.get());
                output.accept(ModItems.NETHER_STAR_AMMO_BOX.get());
                output.accept(ModItems.COMPRESSED_AMMO_BOX.get());
                output.accept(ModItems.AMMO_BOX_21B.get());
                output.accept(ModItems.AMMO_BOX_922_KEI.get());
                output.accept(ModItems.AMMO_BOX_340_KAN.get());
                output.accept(ModItems.AMMO_BOX_DOUBLE_MAX.get());
                output.accept(ModItems.AMMO_BOX_PRACTICAL_INFINITY.get());
                output.accept(ModItems.IRON_AMMO_BOX_CASE.get());
                output.accept(ModItems.GOLD_AMMO_BOX_CASE.get());
                output.accept(ModItems.DIAMOND_AMMO_BOX_CASE.get());
                output.accept(ModItems.NETHERITE_AMMO_BOX_CASE.get());
                output.accept(ModItems.NETHER_STAR_AMMO_BOX_CASE.get());
                output.accept(ModItems.COMPRESSED_AMMO_BOX_CASE.get());
            }).build());
    private ModTabs() {}
}
