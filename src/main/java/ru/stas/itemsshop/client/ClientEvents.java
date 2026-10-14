package ru.stas.itemsshop.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;
import ru.stas.itemsshop.ItemsShop;

public final class ClientEvents {
    public static final KeyMapping OPEN_SHOP = new KeyMapping(
            "key.items_shop.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.items_shop"
    );

    private ClientEvents() {}

    @Mod.EventBusSubscriber(modid = ItemsShop.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_SHOP);
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> MenuScreens.register(ItemsShop.SHOP_MENU.get(), ShopScreen::new));
        }
    }

    @Mod.EventBusSubscriber(modid = ItemsShop.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBusEvents {
        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft minecraft = Minecraft.getInstance();
            while (OPEN_SHOP.consumeClick()) {
                if (minecraft.player != null && minecraft.screen == null) ru.stas.itemsshop.network.ShopNetwork.openShop();
            }
        }

        @SubscribeEvent
        public static void itemTooltip(ItemTooltipEvent event) {
            if (event.getItemStack().is(Items.SPAWNER)) {
                event.getToolTip().add(Component.translatable("tooltip.items_shop.empty_spawner")
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
