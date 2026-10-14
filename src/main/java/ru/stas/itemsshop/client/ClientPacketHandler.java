package ru.stas.itemsshop.client;

import net.minecraft.client.Minecraft;
import ru.stas.itemsshop.network.BalanceSyncPacket;

public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void handle(BalanceSyncPacket packet) {
        ClientState.setBalance(packet.balance());
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ShopScreen screen) {
            screen.onServerResponse(packet.message(), packet.success());
        } else if (minecraft.player != null && !packet.message().getString().isEmpty()) {
            minecraft.player.displayClientMessage(packet.message(), true);
        }
    }
}
