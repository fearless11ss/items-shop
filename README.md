# Items Shop

Items Shop is a Forge 1.20.1 mod that adds a secure in-game marketplace for buying and selling almost every vanilla item. Players earn coins by selling gathered, crafted, farmed, or looted items and can spend them without leaving the world.

The economy is designed to reduce repetitive grinding without replacing normal survival progression.

## Features

- Configurable shop keybind (`B` by default).
- Separate Buy and Sell tabs with the player's inventory on screen.
- Creative-mode-inspired categories and search by localized name or registry ID.
- Almost every vanilla block and item, excluding unsafe administrative and technical items.
- Every valid level of every vanilla enchanted book.
- Persistent individual balances starting at 0 coins.
- Integer, progression-aware prices based on rarity, usefulness, and crafting recipes.
- Sale values adjusted for equipment enchantments and remaining durability.
- No daily limits, farm penalties, or diminishing returns.
- Server-authoritative validation of balances, prices, quantities, and transactions.
- English and Russian localization.

## Requirements

- Minecraft 1.20.1
- Forge 47 or newer for Minecraft 1.20.1
- Installation on both the client and the server

No additional mods are required. For single-player, install the mod in the local Minecraft instance as usual.

## Installation

1. Install Forge for Minecraft 1.20.1.
2. Download the latest Items Shop `.jar`.
3. Place it in the `mods` folder on every client and on the server.
4. Start Minecraft and press `B` to open the shop.

## Important behavior

- A purchased spawner is empty. Place it and apply a spawn egg to select a mob.
- Administrative items such as command blocks, barriers, structure blocks, light blocks, the debug stick, and knowledge books are not sold.
- The dragon egg cannot be bought or sold because vanilla portal duplication would allow unlimited copies to generate money.
- Selling an item removes the entire offered stack together with its custom name, enchantments, stored contents, and other NBT data. Empty containers before selling them if you want to keep their contents.

## Building from source

JDK 17-21 is required.

```powershell
./gradlew.bat --gradle-user-home C:\GradleItemsShop build
```

The compiled mod is written to `build/libs/items_shop-1.6.8.jar`.

## Reporting bugs

Please use GitHub Issues and include the Minecraft, Forge, and Items Shop versions, clear reproduction steps, the expected result, and relevant logs or crash reports. Remove access tokens, server addresses, and other private information before attaching logs.

## AI assistance disclosure

Generative AI assistance was used for parts of the implementation and code review, as well as for drafting documentation and localization text. The project concept, feature decisions, economy design, testing, icon, and gallery images were created or directed by the author.

## License

Items Shop is available under the [MIT License](LICENSE.md).

---

## Русский

Items Shop — мод для Forge 1.20.1, добавляющий внутриигровой магазин почти всех ванильных предметов и блоков Minecraft 1.20.1.

- Магазин открывается клавишей `B`, которую можно переназначить.
- Стартовый баланс каждого игрока — 0 монет.
- Доступны поиск, категории, покупка, продажа и все уровни ванильных зачарованных книг.
- Цены, балансы и транзакции проверяются сервером.
- Ограничений на обычную добычу и автоматические фермы нет.

Для игры мод нужно установить на клиент и сервер. Дополнительные моды не требуются.
