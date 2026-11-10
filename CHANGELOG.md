# Changelog

This file records the history of published Items Shop releases. Version numbers that do not appear here were not published.

## 1.6.14 — 2026-11-10

### Economy fixes

- Added individual prices for buckets containing cod, salmon, tropical fish, pufferfish, tadpoles, and axolotls.
- Accounted for the empty bucket left behind after releasing a mob.
- Prevented an unlimited-money loop caused by buying mob buckets at the fallback price and selling the empty buckets.

## 1.6.13 — 2026-10-30

### Economy fixes

- Rebalanced Magma Block prices to 100 coins for buying and 2 coins for selling.
- Rebalanced Magma Cream prices to 3,500 coins for buying and 50 coins for selling.
- Prevented naturally abundant Magma Blocks from providing excessive income.

## 1.6.12 — 2026-10-19

### Economy and potion update

- Added effect potions to the shop: normal, extended, strong, splash, and lingering variants.
- Excluded mundane, thick, and awkward potions from the shop.
- Added individual potion prices based on the effect and potion variant.
- Changed purchases so items are delivered directly to the player's inventory.
- Kept the selected item and quantity after a purchase to make repeat purchases faster.

## 1.6.9 — 2026-10-15

### Compatibility fixes

- Fixed Inventory Profiles Next buttons and inventory actions appearing in the shop.
- Hidden the JEI item list, bookmarks, and interface buttons while the shop is open.
- Kept Inventory Profiles Next and JEI optional; neither mod is required to use Items Shop.
- Preserved existing player balances when updating.

## 1.6.7 — 2026-10-15

### Economy safety and UI fixes

- Removed the Dragon Egg from buying and selling to prevent unlimited money generation through vanilla portal duplication.
- Fixed the player inventory frame so its outer border has equal thickness on every side.

## 1.6.6 — 2026-10-15

### Initial public release

- Added an in-game marketplace for buying and selling almost every vanilla Minecraft 1.20.1 item.
- Added persistent individual player balances starting at 0 coins.
- Added separate Buy and Sell tabs with an inventory-style interface.
- Added creative-mode-inspired categories and localized item search.
- Added every valid level of every vanilla enchanted book.
- Added enchantment and durability adjustments to equipment sale prices.
- Added progression-aware, recipe-based integer pricing.
- Added unrestricted income from ordinary resource gathering and automatic farms.
- Added empty purchasable spawners that can be configured with spawn eggs after placement.
- Added server-authoritative validation for prices, balances, quantities, and transactions.
- Added English and Russian localization.
- Added a configurable shop keybind, set to `B` by default.
- Added support for Forge on Minecraft 1.20.1.
