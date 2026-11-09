package ru.stas.itemsshop.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import ru.stas.itemsshop.economy.ShopService;
import ru.stas.itemsshop.menu.ShopMenu;

import java.util.function.Supplier;

public record OpenShopPacket() {
    public static void encode(OpenShopPacket packet, FriendlyByteBuf buffer) {}

    public static OpenShopPacket decode(FriendlyByteBuf buffer) {
        return new OpenShopPacket();
    }

    public static void handle(OpenShopPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            NetworkHooks.openScreen(player, new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new ShopMenu(containerId, inventory),
                    Component.translatable("screen.items_shop.title")
            ));
            ShopService.sync(player);
        }
        contextSupplier.get().setPacketHandled(true);
    }
}
