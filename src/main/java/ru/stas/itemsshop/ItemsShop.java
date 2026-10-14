package ru.stas.itemsshop;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import ru.stas.itemsshop.network.ShopNetwork;
import ru.stas.itemsshop.menu.ShopMenu;

@Mod(ItemsShop.MOD_ID)
public final class ItemsShop {
    public static final String MOD_ID = "items_shop";
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);
    public static final RegistryObject<MenuType<ShopMenu>> SHOP_MENU = MENUS.register("shop", () -> IForgeMenuType.create(ShopMenu::new));

    public ItemsShop(FMLJavaModLoadingContext context) {
        MENUS.register(context.getModEventBus());
        context.getModEventBus().addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ShopNetwork::register);
    }
}
