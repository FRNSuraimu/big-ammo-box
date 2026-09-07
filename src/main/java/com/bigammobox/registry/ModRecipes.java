package com.bigammobox.registry;

import com.bigammobox.BigAmmoBoxMod;
import com.bigammobox.recipe.CaseUpgradeRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, BigAmmoBoxMod.MOD_ID);
    public static final RegistryObject<RecipeSerializer<CaseUpgradeRecipe>> CASE_UPGRADE = SERIALIZERS.register("case_upgrade",
            CaseUpgradeRecipe.Serializer::new);
    private ModRecipes() {}
}
