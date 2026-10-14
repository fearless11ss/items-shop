package ru.stas.itemsshop.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import ru.stas.itemsshop.network.BalanceSyncPacket;
import ru.stas.itemsshop.network.ShopNetwork;

public final class ShopService {
    private ShopService() {}

    public static void sync(ServerPlayer player) {
        send(player, Component.empty(), true);
    }

    public static boolean chargePurchase(ServerPlayer player, ItemStack purchase) {
        int quantity = purchase.getCount();
        if (!PriceManager.isValidPurchase(purchase)
                || quantity < 1 || quantity > PriceManager.maxPurchaseQuantity(purchase)) {
            send(player, Component.translatable("message.items_shop.invalid_request"), false);
            return false;
        }
        PriceCatalog prices = prices(player);
        long total = safeTotal(prices.buyPrice(purchase), quantity);
        BalanceData data = BalanceData.get(player);
        if (!data.withdraw(player.getUUID(), total)) {
            send(player, Component.translatable("message.items_shop.not_enough_money"), false);
            return false;
        }
        send(player, Component.translatable("message.items_shop.bought", quantity, PriceManager.displayName(purchase), total), true);
        return true;
    }

    public static void creditSale(ServerPlayer player, ItemStack sold) {
        if (sold.isEmpty() || !PriceManager.isTradable(sold.getItem())) {
            send(player, Component.translatable("message.items_shop.invalid_request"), false);
            return;
        }
        long total = safeTotal(prices(player).sellPrice(sold), sold.getCount());
        BalanceData.get(player).deposit(player.getUUID(), total);
        send(player, Component.translatable("message.items_shop.sold", sold.getCount(), PriceManager.displayName(sold), total), true);
    }

    private static long safeTotal(int unitPrice, int quantity) {
        return Math.multiplyExact((long) unitPrice, quantity);
    }

    private static PriceCatalog prices(ServerPlayer player) {
        var server = player.getServer();
        if (server == null) throw new IllegalStateException("Player has no server");
        return PriceManager.catalog(server.getRecipeManager(), server.registryAccess());
    }

    private static void send(ServerPlayer player, Component message, boolean success) {
        long balance = BalanceData.get(player).getBalance(player.getUUID());
        ShopNetwork.sendToPlayer(player, new BalanceSyncPacket(balance, message, success));
    }
}
