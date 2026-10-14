package ru.stas.itemsshop.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ru.stas.itemsshop.economy.ShopService;

import java.util.function.Supplier;

public record RequestBalancePacket() {
    public static void encode(RequestBalancePacket packet, FriendlyByteBuf buffer) {}

    public static RequestBalancePacket decode(FriendlyByteBuf buffer) {
        return new RequestBalancePacket();
    }

    public static void handle(RequestBalancePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender != null) ShopService.sync(sender);
        contextSupplier.get().setPacketHandled(true);
    }
}
