package ru.stas.itemsshop.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;
import ru.stas.itemsshop.menu.ShopMenu;
import ru.stas.itemsshop.economy.PriceManager;

import java.util.function.Supplier;

public record SelectPurchasePacket(int containerId, ResourceLocation itemId,
                                   ResourceLocation enchantmentId, int enchantmentLevel, int quantity) {
    public static void encode(SelectPurchasePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeResourceLocation(packet.itemId);
        buffer.writeBoolean(packet.enchantmentId != null);
        if (packet.enchantmentId != null) {
            buffer.writeResourceLocation(packet.enchantmentId);
            buffer.writeVarInt(packet.enchantmentLevel);
        }
        buffer.writeVarInt(packet.quantity);
    }

    public static SelectPurchasePacket decode(FriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        ResourceLocation itemId = buffer.readResourceLocation();
        ResourceLocation enchantmentId = null;
        int enchantmentLevel = 0;
        if (buffer.readBoolean()) {
            enchantmentId = buffer.readResourceLocation();
            enchantmentLevel = buffer.readVarInt();
        }
        return new SelectPurchasePacket(containerId, itemId, enchantmentId, enchantmentLevel, buffer.readVarInt());
    }

    public static void handle(SelectPurchasePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && player.containerMenu.containerId == packet.containerId && player.containerMenu instanceof ShopMenu menu
                && "minecraft".equals(packet.itemId.getNamespace())) {
            Item item = BuiltInRegistries.ITEM.getOptional(packet.itemId).orElse(Items.AIR);
            if (item != Items.AIR) {
                ItemStack product;
                if (packet.enchantmentId != null) {
                    if (item != Items.ENCHANTED_BOOK) {
                        contextSupplier.get().setPacketHandled(true);
                        return;
                    }
                    product = PriceManager.enchantedBook(packet.enchantmentId, packet.enchantmentLevel);
                } else {
                    if (item == Items.ENCHANTED_BOOK) {
                        contextSupplier.get().setPacketHandled(true);
                        return;
                    }
                    product = item.getDefaultInstance();
                }
                if (!product.isEmpty()) menu.selectPurchase(product, packet.quantity);
            }
        }
        contextSupplier.get().setPacketHandled(true);
    }
}
