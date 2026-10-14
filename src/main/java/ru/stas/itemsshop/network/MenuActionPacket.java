package ru.stas.itemsshop.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ru.stas.itemsshop.menu.ShopMenu;

import java.util.function.Supplier;

public record MenuActionPacket(int containerId, boolean buy) {
    public static void encode(MenuActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeBoolean(packet.buy);
    }

    public static MenuActionPacket decode(FriendlyByteBuf buffer) {
        return new MenuActionPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(MenuActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && player.containerMenu.containerId == packet.containerId && player.containerMenu instanceof ShopMenu menu) {
            if (packet.buy) menu.buy(player);
            else menu.sell(player);
        }
        contextSupplier.get().setPacketHandled(true);
    }
}
