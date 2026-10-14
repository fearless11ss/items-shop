package ru.stas.itemsshop.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import ru.stas.itemsshop.ItemsShop;
import ru.stas.itemsshop.economy.PriceManager;
import ru.stas.itemsshop.economy.ShopService;

public final class ShopMenu extends AbstractContainerMenu {
    public static final int TAB_BUY = 0;
    public static final int TAB_SELL = 1;
    public static final int PURCHASE_SLOT = 0;
    public static final int SALE_SLOT = 1;

    private final SimpleContainer shopSlots = new SimpleContainer(2);
    private final ContainerData state = new SimpleContainerData(2); // tab, purchase paid

    public ShopMenu(int containerId, Inventory inventory, FriendlyByteBuf ignored) {
        this(containerId, inventory);
    }

    public ShopMenu(int containerId, Inventory inventory) {
        super(ItemsShop.SHOP_MENU.get(), containerId);
        checkContainerSize(shopSlots, 2);

        addSlot(new PurchaseSlot(shopSlots, PURCHASE_SLOT, 280, 35));
        addSlot(new SaleSlot(shopSlots, SALE_SLOT, 171, 65));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 100 + column * 18, 176 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 100 + column * 18, 235));
        }
        addDataSlots(state);
    }

    public int tab() {
        return state.get(0);
    }

    public boolean purchasePaid() {
        return state.get(1) == 1;
    }

    public void setTab(int tab) {
        state.set(0, tab == TAB_SELL ? TAB_SELL : TAB_BUY);
        broadcastChanges();
    }

    public void setTabClient(int tab) {
        state.set(0, tab == TAB_SELL ? TAB_SELL : TAB_BUY);
    }

    public void selectPurchase(ItemStack product, int quantity) {
        if (tab() != TAB_BUY || !PriceManager.isValidPurchase(product)
                || quantity < 1 || quantity > PriceManager.maxPurchaseQuantity(product)) return;
        ItemStack current = shopSlots.getItem(PURCHASE_SLOT);
        if (purchasePaid() && !current.isEmpty()) return;
        state.set(1, 0);
        ItemStack preview = product.copy();
        preview.setCount(quantity);
        shopSlots.setItem(PURCHASE_SLOT, preview);
        broadcastChanges();
    }

    public void buy(ServerPlayer player) {
        if (tab() != TAB_BUY || purchasePaid()) return;
        ItemStack preview = shopSlots.getItem(PURCHASE_SLOT);
        if (preview.isEmpty()) return;
        if (ShopService.chargePurchase(player, preview)) {
            state.set(1, 1);
            broadcastChanges();
        }
    }

    public void sell(ServerPlayer player) {
        if (tab() != TAB_SELL) return;
        ItemStack offered = shopSlots.getItem(SALE_SLOT);
        if (offered.isEmpty() || !PriceManager.isTradable(offered.getItem())) return;
        ItemStack sold = offered.copy();
        shopSlots.setItem(SALE_SLOT, ItemStack.EMPTY);
        ShopService.creditSale(player, sold);
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack result = original.copy();

        if (index == PURCHASE_SLOT) {
            if (!purchasePaid() || !moveItemStackTo(original, 2, 38, true)) return ItemStack.EMPTY;
        } else if (index == SALE_SLOT) {
            if (!moveItemStackTo(original, 2, 38, true)) return ItemStack.EMPTY;
        } else {
            if (tab() != TAB_SELL || !moveItemStackTo(original, SALE_SLOT, SALE_SLOT + 1, false)) return ItemStack.EMPTY;
        }

        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, original);
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide) return;

        ItemStack purchase = shopSlots.removeItemNoUpdate(PURCHASE_SLOT);
        if (purchasePaid() && !purchase.isEmpty()) returnToPlayer(player, purchase);
        ItemStack sale = shopSlots.removeItemNoUpdate(SALE_SLOT);
        if (!sale.isEmpty()) returnToPlayer(player, sale);
    }

    private static void returnToPlayer(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack) && !stack.isEmpty()) player.drop(stack, false);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private final class PurchaseSlot extends Slot {
        private PurchaseSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return isActive() && purchasePaid();
        }

        @Override
        public boolean isActive() {
            return tab() == TAB_BUY;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            if (!hasItem()) state.set(1, 0);
        }
    }

    private final class SaleSlot extends Slot {
        private SaleSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return PriceManager.isTradable(stack.getItem());
        }

        @Override
        public boolean isActive() {
            return tab() == TAB_SELL;
        }
    }
}
