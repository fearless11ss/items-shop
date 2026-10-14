# Items Shop

Items Shop adds a secure in-game marketplace for Minecraft. Earn coins by selling items you collect, then spend them on almost any vanilla block or item without leaving your world.

The shop is designed to make survival less grindy while preserving normal progression. Prices are based on resource value, crafting recipes, rarity, utility, and the stage of the game in which an item normally becomes available.

## Features

- Buy and sell almost every vanilla item and block from Minecraft 1.20.1.
- Open the shop with a configurable keybind (`B` by default).
- Browse a compact, inventory-style catalog organized into familiar creative-mode categories.
- Search by localized item name or registry ID.
- Use separate Buy and Sell tabs with clear prices before confirming a transaction.
- Buy every level of every vanilla enchanted book from the Ingredients category.
- Receive additional sale value for enchantments on tools, weapons, and armor.
- Sell damaged equipment at a price adjusted to its remaining durability.
- Keep a separate persistent coin balance for every player.
- Use farms and ordinary resource gathering without daily limits or diminishing returns.
- Play safely in multiplayer: balances, item quantities, and prices are validated by the server.
- Use the interface in English or Russian.

## How to use

1. Press `B` to open the shop. The key can be changed in Minecraft's Controls menu.
2. Open the **Sell** tab, place an item stack in the sale slot, review its value, and confirm the sale to earn coins.
3. Open the **Buy** tab, select an item and quantity, pay the displayed price, and take the item from the output slot.

Every player starts with **0 coins**, so the first purchase must be funded by selling gathered, crafted, farmed, or looted items.

## Economy

All prices are whole numbers. Buy and sell prices are intentionally different, and crafted products are checked against the value of their ingredients to prevent simple crafting loops from generating free money.

Mass-gathered materials remain valid sources of income, while tools, equipment, rare loot, late-game resources, spawn eggs, and other progression-sensitive items are priced according to their usefulness and availability. Enchanted books have individual prices based on enchantment, level, rarity, and normal acquisition methods such as librarian trading or exploration.

There are no daily sale limits, farm penalties, or dynamically decreasing prices.

## Important information

- Requires **Minecraft 1.20.1** and **Forge 47 or newer**.
- The mod must be installed on both the client and the server. For single-player, install it in the local Minecraft instance as usual.
- No additional mods are required.
- Administrative and technical items such as command blocks, barriers, structure blocks, light blocks, jigsaw blocks, the debug stick, and knowledge books are not available in the shop.
- A purchased spawner is empty. Place it and use a spawn egg on it to select a mob. Spawners can only be purchased one at a time.
- Selling an item transfers the entire stack to the shop. Any custom name, enchantments, stored contents, or other NBT data attached to that stack are removed with it. Empty containers before selling them if you want to keep their contents.

Items Shop is intended for survival worlds that want a convenient, progression-aware economy without turning resource gathering and automation into obsolete mechanics.
