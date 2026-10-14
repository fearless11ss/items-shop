package ru.stas.itemsshop.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import ru.stas.itemsshop.ItemsShop;
import ru.stas.itemsshop.economy.PriceManager;

public final class ShopNetwork {
    private static final String VERSION = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ItemsShop.MOD_ID, "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals
    );
    private static int packetId;

    private ShopNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(OpenShopPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenShopPacket::encode)
                .decoder(OpenShopPacket::decode)
                .consumerMainThread(OpenShopPacket::handle)
                .add();
        CHANNEL.messageBuilder(RequestBalancePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestBalancePacket::encode)
                .decoder(RequestBalancePacket::decode)
                .consumerMainThread(RequestBalancePacket::handle)
                .add();
        CHANNEL.messageBuilder(SelectPurchasePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectPurchasePacket::encode)
                .decoder(SelectPurchasePacket::decode)
                .consumerMainThread(SelectPurchasePacket::handle)
                .add();
        CHANNEL.messageBuilder(MenuActionPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MenuActionPacket::encode)
                .decoder(MenuActionPacket::decode)
                .consumerMainThread(MenuActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(SetShopTabPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetShopTabPacket::encode)
                .decoder(SetShopTabPacket::decode)
                .consumerMainThread(SetShopTabPacket::handle)
                .add();
        CHANNEL.messageBuilder(BalanceSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BalanceSyncPacket::encode)
                .decoder(BalanceSyncPacket::decode)
                .consumerMainThread(BalanceSyncPacket::handle)
                .add();
    }

    public static void requestBalance() {
        CHANNEL.sendToServer(new RequestBalancePacket());
    }

    public static void openShop() {
        CHANNEL.sendToServer(new OpenShopPacket());
    }

    public static void selectPurchase(int containerId, ItemStack item, int quantity) {
        PriceManager.BookEnchantment book = PriceManager.singleBookEnchantment(item);
        CHANNEL.sendToServer(new SelectPurchasePacket(containerId,
                BuiltInRegistries.ITEM.getKey(item.getItem()),
                book == null ? null : book.id(), book == null ? 0 : book.level(), quantity));
    }

    public static void menuAction(int containerId, boolean buy) {
        CHANNEL.sendToServer(new MenuActionPacket(containerId, buy));
    }

    public static void setTab(int containerId, int tab) {
        CHANNEL.sendToServer(new SetShopTabPacket(containerId, tab));
    }

    public static void sendToPlayer(ServerPlayer player, BalanceSyncPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
