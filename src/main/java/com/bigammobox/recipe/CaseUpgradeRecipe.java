package com.bigammobox.recipe;

import com.bigammobox.registry.ModRecipes;
import com.bigammobox.storage.AmmoBoxCaseStorage;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public final class CaseUpgradeRecipe extends CustomRecipe {
    private final Item lowerCase;
    private final Ingredient material;
    private final ItemStack result;

    public CaseUpgradeRecipe(ResourceLocation id, Item lowerCase, Ingredient material, ItemStack result) {
        super(id, CraftingBookCategory.MISC);
        this.lowerCase = lowerCase;
        this.material = material;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        if (inv.getWidth() < 3 || inv.getHeight() < 3) return false;
        // Require an otherwise empty 3x3 crafting footprint.
        for (int y = 0; y < inv.getHeight(); y++) {
            for (int x = 0; x < inv.getWidth(); x++) {
                ItemStack stack = inv.getItem(x + y * inv.getWidth());
                if (x < 3 && y < 3) continue;
                if (!stack.isEmpty()) return false;
            }
        }

        int[] chestSlots = {0, 2, 6, 8};
        int[] materialSlots = {1, 3, 5, 7};
        for (int slot : chestSlots) {
            if (!inv.getItem(slot).is(Items.CHEST)) return false;
        }
        for (int slot : materialSlots) {
            if (!material.test(inv.getItem(slot))) return false;
        }
        return inv.getItem(4).is(lowerCase);
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
        ItemStack out = result.copy();
        AmmoBoxCaseStorage.copyInventory(inv.getItem(4), out);
        return out;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(9, Ingredient.EMPTY);
        Ingredient chest = Ingredient.of(Items.CHEST);
        Ingredient lower = Ingredient.of(lowerCase);
        int[] chestSlots = {0, 2, 6, 8};
        int[] materialSlots = {1, 3, 5, 7};
        for (int slot : chestSlots) ingredients.set(slot, chest);
        for (int slot : materialSlots) ingredients.set(slot, material);
        ingredients.set(4, lower);
        return ingredients;
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CASE_UPGRADE.get();
    }

    public static final class Serializer implements RecipeSerializer<CaseUpgradeRecipe> {
        @Override
        public CaseUpgradeRecipe fromJson(ResourceLocation id, JsonObject json) {
            ResourceLocation lowerId = new ResourceLocation(GsonHelper.getAsString(json, "lower_case"));
            Item lower = ForgeRegistries.ITEMS.getValue(lowerId);
            if (lower == null || lower == Items.AIR) {
                throw new IllegalArgumentException("Unknown lower_case item: " + lowerId);
            }
            Ingredient material = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "material"));
            ResourceLocation resultId = new ResourceLocation(GsonHelper.getAsString(json, "result"));
            Item resultItem = ForgeRegistries.ITEMS.getValue(resultId);
            if (resultItem == null || resultItem == Items.AIR) {
                throw new IllegalArgumentException("Unknown result item: " + resultId);
            }
            return new CaseUpgradeRecipe(id, lower, material, new ItemStack(resultItem));
        }

        @Override
        public CaseUpgradeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Item lower = ForgeRegistries.ITEMS.getValue(buf.readResourceLocation());
            Ingredient material = Ingredient.fromNetwork(buf);
            ItemStack result = buf.readItem();
            return new CaseUpgradeRecipe(id, lower, material, result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, CaseUpgradeRecipe recipe) {
            buf.writeResourceLocation(ForgeRegistries.ITEMS.getKey(recipe.lowerCase));
            recipe.material.toNetwork(buf);
            buf.writeItem(recipe.result);
        }
    }
}
