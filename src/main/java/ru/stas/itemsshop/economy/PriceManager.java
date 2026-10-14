package ru.stas.itemsshop.economy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Collections;

/**
 * One deterministic price table is used on both sides. The server always
 * recalculates prices itself; values sent by a client are never accepted.
 */
public final class PriceManager {
    private static final int ENCHANTED_ITEM_SELL_PERCENT = 15;
    private static final int MAX_ENCHANTMENT_PRICE = 10_000_000;
    private static final Map<Item, Integer> BUY_OVERRIDES = new HashMap<>();
    private static final Map<Item, Integer> SELL_OVERRIDES = new HashMap<>();
    private static final Map<RecipeManager, PriceCatalog> CATALOGS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<String> UNTRADABLE_ITEMS = Set.of(
            "barrier", "chain_command_block", "command_block", "command_block_minecart",
            "debug_stick", "jigsaw", "knowledge_book", "light",
            "repeating_command_block", "structure_block", "structure_void"
    );
    private static final Set<String> TECHNICAL_ITEMS = Set.of(
            "barrier", "bedrock", "budding_amethyst", "chain_command_block",
            "command_block", "command_block_minecart", "debug_stick", "end_portal_frame",
            "jigsaw", "knowledge_book", "light", "petrified_oak_slab",
            "reinforced_deepslate", "repeating_command_block", "spawner",
            "structure_block", "structure_void"
    );

    static {
        // Smallest units and mass-gathered terrain.
        price(Items.STICK, 2, 1);
        price(Items.SAND, 4, 1);                 price(Items.RED_SAND, 5, 1);
        price(Items.DIRT, 3, 1);                 price(Items.COARSE_DIRT, 4, 1);
        price(Items.ROOTED_DIRT, 5, 1);          price(Items.GRAVEL, 4, 1);
        price(Items.COBBLESTONE, 5, 2);          price(Items.STONE, 6, 2);
        price(Items.COBBLED_DEEPSLATE, 7, 2);    price(Items.DEEPSLATE, 8, 2);
        price(Items.NETHERRACK, 3, 1);           price(Items.END_STONE, 8, 2);
        price(Items.GLASS, 8, 3);                price(Items.GLASS_PANE, 3, 1);
        price(Items.CLAY_BALL, 4, 1);            price(Items.CLAY, 16, 4);
        price(Items.SNOWBALL, 2, 1);             price(Items.SNOW_BLOCK, 8, 4);
        price(Items.ICE, 4, 1);                  price(Items.PACKED_ICE, 36, 9);
        price(Items.BLUE_ICE, 324, 81);

        // Crops and farm products remain worthwhile unlimited income sources.
        price(Items.WHEAT, 8, 2);                price(Items.HAY_BLOCK, 72, 18);
        price(Items.CARROT, 5, 1);               price(Items.POTATO, 5, 1);
        price(Items.BEETROOT, 5, 1);             price(Items.SUGAR_CANE, 4, 1);
        price(Items.BAMBOO, 2, 1);               price(Items.CACTUS, 4, 1);
        price(Items.KELP, 3, 1);                 price(Items.DRIED_KELP, 4, 1);
        price(Items.DRIED_KELP_BLOCK, 36, 9);    price(Items.PUMPKIN, 16, 4);
        price(Items.MELON_SLICE, 4, 1);          price(Items.MELON, 24, 9);

        // Minerals. Reversible 9:1 forms use exact integer conservation.
        price(Items.COAL, 80, 10);               price(Items.COAL_BLOCK, 720, 90);
        price(Items.CHARCOAL, 100, 10);
        price(Items.RAW_COPPER, 30, 5);          price(Items.RAW_COPPER_BLOCK, 270, 45);
        price(Items.COPPER_INGOT, 50, 8);        price(Items.COPPER_BLOCK, 450, 72);
        price(Items.RAW_IRON, 400, 80);          price(Items.RAW_IRON_BLOCK, 3_600, 720);
        price(Items.IRON_NUGGET, 60, 11);        price(Items.IRON_INGOT, 500, 99);
        price(Items.IRON_BLOCK, 4_500, 891);
        price(Items.RAW_GOLD, 800, 150);         price(Items.RAW_GOLD_BLOCK, 7_200, 1_350);
        price(Items.GOLD_NUGGET, 120, 22);       price(Items.GOLD_INGOT, 1_000, 198);
        price(Items.GOLD_BLOCK, 9_000, 1_782);
        price(Items.REDSTONE, 15, 2);            price(Items.REDSTONE_BLOCK, 135, 18);
        price(Items.LAPIS_LAZULI, 30, 5);        price(Items.LAPIS_BLOCK, 270, 45);
        price(Items.QUARTZ, 60, 10);
        price(Items.AMETHYST_SHARD, 100, 20);    price(Items.AMETHYST_BLOCK, 460, 80);
        price(Items.EMERALD, 6_000, 1_200);      price(Items.EMERALD_BLOCK, 54_000, 10_800);
        price(Items.DIAMOND, 12_000, 3_000);     price(Items.DIAMOND_BLOCK, 108_000, 27_000);
        price(Items.ANCIENT_DEBRIS, 35_000, 25_000);
        price(Items.NETHERITE_SCRAP, 40_000, 30_000);
        price(Items.NETHERITE_INGOT, 180_000, 120_000);
        price(Items.NETHERITE_BLOCK, 1_620_000, 1_080_000);
        price(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 100_000, 20_000);

        // Ore blocks are priced above their maximum Fortune sale yield.
        price(Items.COAL_ORE, 100, 10);          price(Items.DEEPSLATE_COAL_ORE, 100, 10);
        price(Items.COPPER_ORE, 120, 10);        price(Items.DEEPSLATE_COPPER_ORE, 120, 10);
        price(Items.IRON_ORE, 500, 80);          price(Items.DEEPSLATE_IRON_ORE, 500, 80);
        price(Items.GOLD_ORE, 800, 150);         price(Items.DEEPSLATE_GOLD_ORE, 800, 150);
        price(Items.REDSTONE_ORE, 60, 2);        price(Items.DEEPSLATE_REDSTONE_ORE, 60, 2);
        price(Items.LAPIS_ORE, 200, 5);          price(Items.DEEPSLATE_LAPIS_ORE, 200, 5);
        price(Items.DIAMOND_ORE, 15_000, 3_000); price(Items.DEEPSLATE_DIAMOND_ORE, 15_000, 3_000);
        price(Items.EMERALD_ORE, 6_000, 1_200);  price(Items.DEEPSLATE_EMERALD_ORE, 6_000, 1_200);
        price(Items.NETHER_GOLD_ORE, 600, 100);  price(Items.NETHER_QUARTZ_ORE, 60, 10);

        // Reversible mob drops and compact forms.
        price(Items.SLIME_BALL, 20, 4);          price(Items.SLIME_BLOCK, 180, 36);
        price(Items.BONE_MEAL, 4, 1);            price(Items.BONE, 12, 3);
        price(Items.BONE_BLOCK, 36, 9);          price(Items.GLOWSTONE_DUST, 20, 4);
        price(Items.GLOWSTONE, 80, 16);          price(Items.STRING, 12, 3);
        price(Items.WHITE_WOOL, 48, 12);

        // Progression and dimensions.
        price(Items.ENDER_PEARL, 4_000, 400);    price(Items.BLAZE_ROD, 6_000, 600);
        price(Items.BLAZE_POWDER, 3_000, 300);   price(Items.ENDER_EYE, 9_000, 700);
        price(Items.OBSIDIAN, 2_000, 200);        price(Items.CRYING_OBSIDIAN, 3_000, 300);
        price(Items.ANVIL, 18_000, 3_000);        price(Items.CHIPPED_ANVIL, 12_000, 2_000);
        price(Items.DAMAGED_ANVIL, 6_000, 1_000);
        price(Items.ENCHANTING_TABLE, 60_000, 6_500);
        price(Items.SADDLE, 20_000, 4_000);       price(Items.NAME_TAG, 20_000, 4_000);
        price(Items.SHULKER_SHELL, 25_000, 5_000);

        // Rare and end-game rewards.
        price(Items.TRIDENT, 150_000, 30_000);    price(Items.TOTEM_OF_UNDYING, 200_000, 40_000);
        price(Items.ELYTRA, 500_000, 100_000);    price(Items.NETHER_STAR, 500_000, 100_000);
        price(Items.BEACON, 600_000, 100_000);    price(Items.ENCHANTED_GOLDEN_APPLE, 750_000, 150_000);
        price(Items.DRAGON_EGG, 2_000_000, 400_000);
        price(Items.HEART_OF_THE_SEA, 100_000, 20_000); price(Items.CONDUIT, 250_000, 36_000);
        price(Items.EXPERIENCE_BOTTLE, 2_000, 400);     price(Items.END_CRYSTAL, 50_000, 1_900);
        price(Items.RESPAWN_ANCHOR, 40_000, 1_800);
        price(Items.WITHER_SKELETON_SKULL, 100_000, 20_000); price(Items.DRAGON_HEAD, 150_000, 30_000);
        price(Items.CREEPER_HEAD, 30_000, 6_000);       price(Items.SKELETON_SKULL, 30_000, 6_000);
        price(Items.ZOMBIE_HEAD, 30_000, 6_000);        price(Items.PIGLIN_HEAD, 30_000, 6_000);
        price(Items.DRAGON_BREATH, 10_000, 2_000);      price(Items.WITHER_ROSE, 20_000, 4_000);
        price(Items.NAUTILUS_SHELL, 10_000, 2_000);     price(Items.ECHO_SHARD, 20_000, 4_000);
        price(Items.PHANTOM_MEMBRANE, 4_000, 800);      price(Items.GHAST_TEAR, 6_000, 1_200);
        price(Items.SCUTE, 3_000, 600);

        // Containers: renewable fluids cannot be resold repeatedly for money.
        price(Items.BUCKET, 300, 60);             price(Items.WATER_BUCKET, 320, 60);
        price(Items.MILK_BUCKET, 340, 60);        price(Items.LAVA_BUCKET, 800, 150);
        price(Items.POWDER_SNOW_BUCKET, 400, 60);
        price(Items.EGG, 4, 1);

        // Spawners are sold empty. NBT does not change their resale value.
        price(Items.SPAWNER, 10_000_000, 2_000_000);

        // Kept in the catalogue at the hard cap; forbidden admin items remain excluded.
        price(Items.BEDROCK, 10_000_000);
    }

    private PriceManager() {}

    private static void price(Item item, int buyPrice) {
        BUY_OVERRIDES.put(item, buyPrice);
    }

    private static void price(Item item, int buyPrice, int sellPrice) {
        BUY_OVERRIDES.put(item, buyPrice);
        SELL_OVERRIDES.put(item, sellPrice);
    }

    static Integer exactPrice(Item item) {
        return BUY_OVERRIDES.get(item);
    }

    static Integer exactSellPrice(Item item) {
        return SELL_OVERRIDES.get(item);
    }

    static int fallbackSellPrice(Item item, int buyPrice) {
        String name = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (name.endsWith("_log") || name.endsWith("_stem")) return 8;
        if (name.endsWith("_wood") || name.endsWith("_hyphae")) return 10;
        if (name.endsWith("_planks")) return 2;
        if (name.endsWith("_leaves") || name.endsWith("_sapling") || name.endsWith("_seeds")) return 1;
        if (name.contains("sand") || name.contains("dirt") || name.contains("gravel")
                || name.contains("cobblestone") || name.equals("stone") || name.equals("deepslate")
                || name.equals("netherrack") || name.equals("mud") || name.equals("clay")) return 1;
        if (name.equals("bamboo") || name.equals("cactus") || name.equals("sugar_cane")
                || name.contains("kelp") || name.contains("seagrass") || name.endsWith("_flower")
                || name.endsWith("_tulip") || name.endsWith("_mushroom") || name.contains("pumpkin")) return 1;

        return Math.max(1, buyPrice / 5);
    }

    public static PriceCatalog catalog(RecipeManager recipes, RegistryAccess registryAccess) {
        synchronized (CATALOGS) {
            return CATALOGS.computeIfAbsent(recipes, ignored -> new PriceCatalog(recipes, registryAccess));
        }
    }

    public static boolean isTradable(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return item != Items.AIR
                && "minecraft".equals(id.getNamespace())
                && !UNTRADABLE_ITEMS.contains(id.getPath());
    }

    static int fallbackBuyPrice(Item item) {
        Integer exact = BUY_OVERRIDES.get(item);
        if (exact != null) return exact;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String name = id.getPath();

        if (TECHNICAL_ITEMS.contains(name)) return 10_000_000;
        if (name.equals("wither_spawn_egg") || name.equals("ender_dragon_spawn_egg")) return 5_000_000;
        if (name.equals("warden_spawn_egg")) return 2_000_000;
        if (name.equals("elder_guardian_spawn_egg")) return 750_000;
        if (name.equals("ravager_spawn_egg") || name.equals("evoker_spawn_egg")
                || name.equals("vindicator_spawn_egg")) return 300_000;
        if (name.endsWith("_spawn_egg")) return isPassiveSpawnEgg(name) ? 75_000 : 100_000;
        if (name.startsWith("music_disc_")) return 25_000;
        if (name.endsWith("_smithing_template")) return 50_000;
        if (name.endsWith("_ore") || name.endsWith("_ore_block")) return 100;
        if (name.endsWith("_log") || name.endsWith("_stem")) return 16;
        if (name.endsWith("_hyphae") || name.endsWith("_wood")) return 25;
        if (name.endsWith("_planks")) return 5;
        if (name.endsWith("_slab")) return 4;
        if (name.endsWith("_stairs")) return 9;
        if (name.endsWith("_wall")) return 6;
        if (name.endsWith("_door")) return 12;
        if (name.endsWith("_trapdoor")) return 18;
        if (name.endsWith("_fence") || name.endsWith("_fence_gate")) return 12;
        if (name.endsWith("_button")) return 6;
        if (name.endsWith("_pressure_plate")) return 12;
        if (name.endsWith("_sapling") || name.endsWith("_seeds")) return 3;
        if (name.endsWith("_banner")) return 100;
        if (name.endsWith("_bed")) return 120;
        if (name.endsWith("_boat") || name.endsWith("_raft")) return 35;
        if (name.endsWith("_sign") || name.endsWith("_hanging_sign")) return 18;
        if (name.endsWith("_candle")) return 20;
        if (name.contains("shulker_box")) return 35_000;
        if (name.contains("diamond_")) return 42_000;
        if (name.contains("netherite_")) return 350_000;
        if (name.contains("golden_")) return 3_000;
        if (name.contains("iron_")) return 2_000;
        if (name.contains("chainmail_")) return 500;
        if (name.contains("leather_")) return 120;

        ItemStack stack = item.getDefaultInstance();
        if (stack.isEdible() && stack.getFoodProperties(null) != null) {
            var food = stack.getFoodProperties(null);
            return 8 + food.getNutrition() * 4 + Math.round(food.getSaturationModifier() * 4);
        }
        if (stack.isDamageableItem()) return Math.min(10_000, 20 + stack.getMaxDamage());
        if (item instanceof BlockItem) return 12;

        return switch (stack.getRarity()) {
            case EPIC -> 100_000;
            case RARE -> 20_000;
            case UNCOMMON -> 500;
            default -> 20;
        };
    }

    static int progressionFloor(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String name = id.getPath();
        if (TECHNICAL_ITEMS.contains(name)) return 10_000_000;
        if (name.equals("wither_spawn_egg") || name.equals("ender_dragon_spawn_egg")) return 5_000_000;
        if (name.equals("warden_spawn_egg")) return 2_000_000;
        if (name.equals("elder_guardian_spawn_egg")) return 750_000;
        if (name.equals("ravager_spawn_egg") || name.equals("evoker_spawn_egg")
                || name.equals("vindicator_spawn_egg")) return 300_000;
        if (name.endsWith("_spawn_egg")) return isPassiveSpawnEgg(name) ? 75_000 : 100_000;
        if (name.equals("netherite_upgrade_smithing_template")) return 100_000;
        if (name.endsWith("_smithing_template")) return 50_000;

        if (isTierTool(name)) {
            if (name.startsWith("wooden_")) return toolFloor(name, 10, 20, 20, 15, 15);
            if (name.startsWith("stone_")) return toolFloor(name, 15, 25, 25, 20, 20);
            if (name.startsWith("golden_")) return toolFloor(name, 1_200, 3_600, 3_600, 2_400, 2_400);
            if (name.startsWith("iron_")) return toolFloor(name, 600, 1_800, 1_800, 1_200, 1_200);
            if (name.startsWith("diamond_")) return toolFloor(name, 14_000, 42_000, 42_000, 28_000, 28_000);
            if (name.startsWith("netherite_")) return toolFloor(name, 320_000, 350_000, 350_000, 335_000, 335_000);
        }

        if (name.contains("netherite_")) return 180_000;
        if (name.contains("diamond_")) return 12_000;
        return item instanceof BlockItem ? 3 : 1;
    }

    /** Creates the exact server-authoritative book represented by a shop entry. */
    public static ItemStack enchantedBook(ResourceLocation enchantmentId, int level) {
        Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.getOptional(enchantmentId).orElse(null);
        if (enchantment == null || !"minecraft".equals(enchantmentId.getNamespace())
                || !enchantment.isAllowedOnBooks()
                || level < enchantment.getMinLevel() || level > enchantment.getMaxLevel()) return ItemStack.EMPTY;
        return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level));
    }

    /** Returns the only stored enchantment, or null for blank/multi-enchantment books. */
    public static BookEnchantment singleBookEnchantment(ItemStack stack) {
        if (!stack.is(Items.ENCHANTED_BOOK)) return null;
        Map<Enchantment, Integer> enchantments = storedEnchantments(stack);
        if (enchantments.size() != 1) return null;
        Map.Entry<Enchantment, Integer> entry = enchantments.entrySet().iterator().next();
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(entry.getKey());
        int level = entry.getValue();
        if (!"minecraft".equals(id.getNamespace()) || !entry.getKey().isAllowedOnBooks()
                || level < entry.getKey().getMinLevel() || level > entry.getKey().getMaxLevel()) return null;
        return new BookEnchantment(id, entry.getKey(), level);
    }

    public static boolean isValidPurchase(ItemStack stack) {
        if (stack.isEmpty() || !isTradable(stack.getItem())) return false;
        return !stack.is(Items.ENCHANTED_BOOK) || singleBookEnchantment(stack) != null;
    }

    public static int maxPurchaseQuantity(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(Items.SPAWNER)) return 1;
        return Math.min(64, stack.getMaxStackSize());
    }

    public static int enchantedBookBuyPrice(ItemStack stack) {
        return enchantedBookPrice(stack, true);
    }

    public static int enchantedBookSellPrice(ItemStack stack) {
        return enchantedBookPrice(stack, false);
    }

    private static int enchantedBookPrice(ItemStack stack, boolean buying) {
        if (!stack.is(Items.ENCHANTED_BOOK)) return 0;
        return enchantmentPrice(storedEnchantments(stack), buying);
    }

    /**
     * Enchantments on equipment retain part of their book resale value. The
     * deliberately small share rewards enchanted loot without making it
     * profitable to buy a book, apply it and immediately sell the result.
     */
    public static int enchantedItemSellBonus(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.ENCHANTED_BOOK) || !stack.isEnchanted()) return 0;
        long bookValue = enchantmentPrice(EnchantmentHelper.getEnchantments(stack), false);
        return (int) Math.min(MAX_ENCHANTMENT_PRICE,
                bookValue * ENCHANTED_ITEM_SELL_PERCENT / 100L);
    }

    private static int enchantmentPrice(Map<Enchantment, Integer> enchantments, boolean buying) {
        long total = 0L;
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
            int level = entry.getValue();
            if (!"minecraft".equals(id.getNamespace()) || !enchantment.isAllowedOnBooks()
                    || level < enchantment.getMinLevel() || level > enchantment.getMaxLevel()) continue;

            int combinations = 1 << Math.min(20, level - enchantment.getMinLevel());
            int base = enchantedBookBasePrice(id.getPath(), enchantment);
            long price = buying
                    ? (long) base * combinations * (level == enchantment.getMinLevel() ? 100L : 115L) / 100L
                    : (long) Math.max(1, base / 5) * combinations;
            total = Math.min(MAX_ENCHANTMENT_PRICE, total + price);
        }
        return (int) total;
    }

    private static int enchantedBookBasePrice(String name, Enchantment enchantment) {
        return switch (name) {
            // Guaranteed shop access remains more expensive than investing in
            // a librarian, but villager-obtainable books are no longer priced
            // as if they were exclusive exploration loot.
            case "mending" -> 60_000;
            case "silk_touch" -> 32_000;
            case "infinity" -> 28_000;
            case "channeling" -> 20_000;
            case "fortune", "frost_walker" -> 8_000;
            case "looting" -> 6_000;
            case "multishot" -> 10_000;
            case "thorns" -> 5_000;
            case "quick_charge" -> 4_000;
            case "unbreaking", "feather_falling", "depth_strider" -> 3_500;
            case "efficiency", "sharpness", "protection", "power", "respiration", "aqua_affinity" -> 2_500;

            // These books retain an exploration premium because librarians do
            // not provide their normal acquisition route.
            case "swift_sneak" -> 17_500;
            case "soul_speed" -> 12_000;
            case "binding_curse", "vanishing_curse" -> 1_000;
            default -> switch (enchantment.getRarity()) {
                case COMMON -> 2_000;
                case UNCOMMON -> 3_000;
                case RARE -> 5_000;
                case VERY_RARE -> 8_000;
            };
        };
    }

    public static Component displayName(ItemStack stack) {
        BookEnchantment book = singleBookEnchantment(stack);
        if (book == null) return stack.getHoverName();
        return Component.translatable(Items.ENCHANTED_BOOK.getDescriptionId()).copy()
                .append(" — ").append(book.enchantment().getFullname(book.level()));
    }

    private static Map<Enchantment, Integer> storedEnchantments(ItemStack stack) {
        return EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(stack));
    }

    public record BookEnchantment(ResourceLocation id, Enchantment enchantment, int level) {}

    private static boolean isPassiveSpawnEgg(String name) {
        return name.equals("allay_spawn_egg") || name.equals("axolotl_spawn_egg")
                || name.equals("bat_spawn_egg") || name.equals("bee_spawn_egg")
                || name.equals("camel_spawn_egg") || name.equals("cat_spawn_egg")
                || name.equals("chicken_spawn_egg") || name.equals("cod_spawn_egg")
                || name.equals("cow_spawn_egg") || name.equals("dolphin_spawn_egg")
                || name.equals("donkey_spawn_egg") || name.equals("fox_spawn_egg")
                || name.equals("frog_spawn_egg") || name.equals("glow_squid_spawn_egg")
                || name.equals("horse_spawn_egg") || name.equals("mooshroom_spawn_egg")
                || name.equals("mule_spawn_egg") || name.equals("ocelot_spawn_egg")
                || name.equals("panda_spawn_egg") || name.equals("parrot_spawn_egg")
                || name.equals("pig_spawn_egg") || name.equals("pufferfish_spawn_egg")
                || name.equals("rabbit_spawn_egg") || name.equals("salmon_spawn_egg")
                || name.equals("sheep_spawn_egg") || name.equals("sniffer_spawn_egg")
                || name.equals("snow_golem_spawn_egg") || name.equals("squid_spawn_egg")
                || name.equals("strider_spawn_egg") || name.equals("tadpole_spawn_egg")
                || name.equals("tropical_fish_spawn_egg") || name.equals("turtle_spawn_egg")
                || name.equals("villager_spawn_egg") || name.equals("wandering_trader_spawn_egg");
    }

    private static int toolFloor(String name, int shovel, int pickaxe, int axe, int sword, int hoe) {
        if (name.endsWith("_shovel")) return shovel;
        if (name.endsWith("_pickaxe")) return pickaxe;
        if (name.endsWith("_axe")) return axe;
        if (name.endsWith("_sword")) return sword;
        if (name.endsWith("_hoe")) return hoe;
        return 1;
    }

    private static boolean isTierTool(String name) {
        return name.endsWith("_shovel") || name.endsWith("_pickaxe") || name.endsWith("_axe")
                || name.endsWith("_sword") || name.endsWith("_hoe");
    }

    public static List<Item> allVanillaItems() {
        List<Item> result = new ArrayList<>();
        BuiltInRegistries.ITEM.forEach(item -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (isTradable(item)) result.add(item);
        });
        result.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        return List.copyOf(result);
    }

    /** One entry per ordinary item plus every valid level of every vanilla enchantment. */
    public static List<ItemStack> allShopEntries() {
        List<ItemStack> result = new ArrayList<>();
        for (Item item : allVanillaItems()) {
            if (item != Items.ENCHANTED_BOOK) {
                result.add(item.getDefaultInstance());
                continue;
            }
            List<Enchantment> enchantments = new ArrayList<>();
            BuiltInRegistries.ENCHANTMENT.forEach(enchantment -> {
                ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
                if ("minecraft".equals(id.getNamespace()) && enchantment.isAllowedOnBooks()) enchantments.add(enchantment);
            });
            enchantments.sort(Comparator.comparing(enchantment -> BuiltInRegistries.ENCHANTMENT.getKey(enchantment).toString()));
            for (Enchantment enchantment : enchantments) {
                for (int level = enchantment.getMinLevel(); level <= enchantment.getMaxLevel(); level++) {
                    result.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level)));
                }
            }
        }
        return List.copyOf(result);
    }
}
