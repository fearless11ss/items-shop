package ru.stas.itemsshop.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ru.stas.itemsshop.menu.ShopMenu;

import java.util.function.Supplier;

public record SetShopTabPacket(int containerId, int tab) {
    public static void encode(SetShopTabPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeVarInt(packet.tab);
    }

    public static SetShopTabPacket decode(FriendlyByteBuf buffer) {
        return new SetShopTabPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(SetShopTabPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && player.containerMenu.containerId == packet.containerId && player.containerMenu instanceof ShopMenu menu) {
            menu.setTab(packet.tab);
        }
        contextSupplier.get().setPacketHandled(true);
    }
}
