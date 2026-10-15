package ru.stas.itemsshop.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import ru.stas.itemsshop.ItemsShop;
import ru.stas.itemsshop.client.ShopScreen;

@JeiPlugin
public final class ItemsShopJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(ItemsShop.MOD_ID, "jei_compat");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(ShopScreen.class, ItemsShopJeiPlugin::getGuiProperties);
    }

    @Nullable
    private static IGuiProperties getGuiProperties(ShopScreen screen) {
        if (screen.width <= 0 || screen.height <= 0) return null;
        return new FullscreenGuiProperties(screen.width, screen.height);
    }

    private record FullscreenGuiProperties(int width, int height) implements IGuiProperties {
        @Override
        public Class<? extends Screen> getScreenClass() {
            return ShopScreen.class;
        }

        @Override
        public int getGuiLeft() {
            return 0;
        }

        @Override
        public int getGuiTop() {
            return 0;
        }

        @Override
        public int getGuiXSize() {
            return width;
        }

        @Override
        public int getGuiYSize() {
            return height;
        }

        @Override
        public int getScreenWidth() {
            return width;
        }

        @Override
        public int getScreenHeight() {
            return height;
        }
    }
}
