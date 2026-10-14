package ru.stas.itemsshop.economy;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Calculates progression-aware prices from the recipes loaded on both sides. */
public final class PriceCatalog {
    private static final int MAX_PRICE = 10_000_000;
    private final RegistryAccess registryAccess;
    private final Map<Item, List<Recipe<?>>> recipesByOutput = new IdentityHashMap<>();
    private final Map<Item, Integer> buyCache = new IdentityHashMap<>();
    private final Map<Item, Integer> sellCache = new IdentityHashMap<>();

    PriceCatalog(RecipeManager recipes, RegistryAccess registryAccess) {
        this.registryAccess = registryAccess;
        List<Recipe<?>> orderedRecipes = new ArrayList<>(recipes.getRecipes());
        orderedRecipes.sort(Comparator.comparing(recipe -> recipe.getId().toString()));
        for (Recipe<?> recipe : orderedRecipes) {
            ItemStack result;
            try {
                result = recipe.getResultItem(registryAccess);
            } catch (RuntimeException ignored) {
                continue;
            }
            if (!result.isEmpty() && !recipe.getIngredients().isEmpty()) {
                recipesByOutput.computeIfAbsent(result.getItem(), ignored -> new ArrayList<>()).add(recipe);
            }
        }
    }

    public int buyPrice(Item item) {
        return computeBuy(item, new HashSet<>(), 0);
    }

    public int buyPrice(ItemStack stack) {
        int enchantedBookPrice = PriceManager.enchantedBookBuyPrice(stack);
        return enchantedBookPrice > 0 ? enchantedBookPrice : buyPrice(stack.getItem());
    }

    public int sellPrice(Item item) {
        return computeSell(item, new HashSet<>(), 0);
    }

    public int sellPrice(ItemStack stack) {
        int enchantedBookPrice = PriceManager.enchantedBookSellPrice(stack);
        if (enchantedBookPrice > 0) return enchantedBookPrice;
        long price = Math.min(MAX_PRICE,
                (long) sellPrice(stack.getItem()) + PriceManager.enchantedItemSellBonus(stack));
        if (!stack.isDamageableItem()) return (int) price;
        int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
        return Math.max(1, (int) Math.ceil((double) price * remaining / stack.getMaxDamage()));
    }

    private int computeBuy(Item item, Set<Item> visiting, int depth) {
        Integer cached = buyCache.get(item);
        if (cached != null) return cached;

        Integer exact = PriceManager.exactPrice(item);
        if (exact != null) {
            buyCache.put(item, exact);
            return exact;
        }
        if (depth > 20 || !visiting.add(item)) return PriceManager.fallbackBuyPrice(item);

        long bestRecipe = Long.MAX_VALUE;
        for (Recipe<?> recipe : recipesByOutput.getOrDefault(item, List.of())) {
            long ingredientTotal = 0L;
            boolean valid = true;
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) continue;
                long cheapest = Long.MAX_VALUE;
                for (ItemStack choice : ingredient.getItems()) {
                    if (choice.isEmpty()) continue;
                    long option = (long) computeBuy(choice.getItem(), visiting, depth + 1) * Math.max(1, choice.getCount());
                    Item remainder = choice.getItem().getCraftingRemainingItem();
                    if (remainder != null) {
                        option = Math.max(0L, option - (long) computeBuy(remainder, visiting, depth + 1) * Math.max(1, choice.getCount()));
                    }
                    cheapest = Math.min(cheapest, option);
                }
                if (cheapest == Long.MAX_VALUE) {
                    valid = false;
                    break;
                }
                ingredientTotal = Math.min((long) MAX_PRICE * 64L, ingredientTotal + cheapest);
            }
            if (!valid || ingredientTotal <= 0) continue;

            ItemStack result = recipe.getResultItem(registryAccess);
            int outputCount = Math.max(1, result.getCount());
            int markup = recipeMarkup(recipe.getType());
            long processingCost = processingCost(recipe.getType());
            long calculated = ((ingredientTotal + processingCost) * markup + outputCount * 100L - 1L) / (outputCount * 100L);
            bestRecipe = Math.min(bestRecipe, calculated);
        }
        visiting.remove(item);

        int price = bestRecipe == Long.MAX_VALUE
                ? PriceManager.fallbackBuyPrice(item)
                : (int) Math.min(MAX_PRICE, Math.max(PriceManager.progressionFloor(item), bestRecipe));
        buyCache.put(item, price);
        return price;
    }

    /**
     * Crafted goods may never sell for more than the ingredients consumed by
     * their cheapest recipe. This keeps crafting, stonecutting and container
     * recipes from becoming money generators while still allowing manually
     * priced raw resources to reward gathering and processing.
     */
    private int computeSell(Item item, Set<Item> visiting, int depth) {
        Integer cached = sellCache.get(item);
        if (cached != null) return cached;

        Integer exact = PriceManager.exactSellPrice(item);
        int fallback = exact != null ? exact : PriceManager.fallbackSellPrice(item, buyPrice(item));
        if (depth > 20 || !visiting.add(item)) return fallback;

        long cheapestRecipeValue = Long.MAX_VALUE;
        for (Recipe<?> recipe : recipesByOutput.getOrDefault(item, List.of())) {
            // Explicit raw-to-processed prices may include a small smelting
            // premium. Lossless crafting and stonecutting are always capped.
            if (exact != null && isCooking(recipe.getType())) continue;
            long ingredientTotal = 0L;
            boolean valid = true;
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) continue;
                long cheapest = Long.MAX_VALUE;
                for (ItemStack choice : ingredient.getItems()) {
                    if (choice.isEmpty()) continue;
                    int count = Math.max(1, choice.getCount());
                    long option = (long) computeSell(choice.getItem(), visiting, depth + 1) * count;
                    Item remainder = choice.getItem().getCraftingRemainingItem();
                    if (remainder != null) {
                        option = Math.max(0L, option - (long) computeSell(remainder, visiting, depth + 1) * count);
                    }
                    cheapest = Math.min(cheapest, option);
                }
                if (cheapest == Long.MAX_VALUE) {
                    valid = false;
                    break;
                }
                ingredientTotal = Math.min((long) MAX_PRICE * 64L, ingredientTotal + cheapest);
            }
            if (!valid || ingredientTotal <= 0) continue;

            int outputCount = Math.max(1, recipe.getResultItem(registryAccess).getCount());
            long perItem = ingredientTotal / outputCount;
            if (perItem > 0) cheapestRecipeValue = Math.min(cheapestRecipeValue, perItem);
        }
        visiting.remove(item);

        int price = cheapestRecipeValue == Long.MAX_VALUE
                ? fallback
                : (int) Math.max(1L, Math.min(fallback, cheapestRecipeValue));
        sellCache.put(item, price);
        return price;
    }

    private static int recipeMarkup(RecipeType<?> type) {
        if (type == RecipeType.STONECUTTING) return 105;
        if (type == RecipeType.SMITHING) return 110;
        if (type == RecipeType.SMELTING || type == RecipeType.BLASTING
                || type == RecipeType.SMOKING) return 110;
        if (type == RecipeType.CAMPFIRE_COOKING) return 105;
        return 115;
    }

    private static int processingCost(RecipeType<?> type) {
        if (type == RecipeType.SMELTING || type == RecipeType.BLASTING || type == RecipeType.SMOKING) return 10;
        return 0;
    }

    private static boolean isCooking(RecipeType<?> type) {
        return type == RecipeType.SMELTING || type == RecipeType.BLASTING
                || type == RecipeType.SMOKING || type == RecipeType.CAMPFIRE_COOKING;
    }
}
