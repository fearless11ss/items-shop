package ru.stas.itemsshop.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import ru.stas.itemsshop.client.ClientPacketHandler;

import java.util.function.Supplier;

public record BalanceSyncPacket(long balance, Component message, boolean success) {
    public static void encode(BalanceSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.balance);
        buffer.writeComponent(packet.message);
        buffer.writeBoolean(packet.success);
    }

    public static BalanceSyncPacket decode(FriendlyByteBuf buffer) {
        return new BalanceSyncPacket(buffer.readLong(), buffer.readComponent(), buffer.readBoolean());
    }

    public static void handle(BalanceSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handle(packet));
        contextSupplier.get().setPacketHandled(true);
    }
}
