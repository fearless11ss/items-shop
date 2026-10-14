package ru.stas.itemsshop.client;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import ru.stas.itemsshop.economy.PriceManager;
import ru.stas.itemsshop.economy.PriceCatalog;
import ru.stas.itemsshop.menu.ShopMenu;
import ru.stas.itemsshop.network.ShopNetwork;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private static final int COLUMNS = 9;
    private static final int ROWS = 5;
    private static final int GRID_X = 31;
    private static final int GRID_Y = 49;
    private static final int CELL = 18;
    private static final int CATEGORY_LEFT_X = 10;
    private static final int CATEGORY_RIGHT_X = 197;
    private static final int CATEGORY_Y = 27;
    private static final int CATEGORY_STEP = 19;
    private static final int CATEGORY_SPLIT = 6;

    private final List<ItemStack> allItems = PriceManager.allShopEntries();
    private final List<ItemStack> filteredItems = new ArrayList<>();
    private final List<CreativeModeTab> categories = new ArrayList<>();
    private final List<CategoryButton> categoryButtons = new ArrayList<>();
    private final PriceCatalog prices;
    private EditBox searchBox;
    private Button buyTabButton;
    private Button sellTabButton;
    private Button quantityButton;
    private Button buyButton;
    private Button sellButton;
    private ItemStack selected = ItemStack.EMPTY;
    private int selectedCategory;
    private int quantity = 1;
    private int scrollRow;
    private Component notification = Component.empty();
    private boolean notificationSuccess;
    private long notificationUntil;

    public ShopScreen(ShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 360;
        imageHeight = 261;
        inventoryLabelX = 100;
        inventoryLabelY = 164;
        var level = Minecraft.getInstance().level;
        if (level == null) throw new IllegalStateException("Shop opened without a client level");
        prices = PriceManager.catalog(level.getRecipeManager(), level.registryAccess());
        CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), false, level.registryAccess());
        addCategory(CreativeModeTabs.SEARCH);
        addCategory(CreativeModeTabs.BUILDING_BLOCKS);
        addCategory(CreativeModeTabs.COLORED_BLOCKS);
        addCategory(CreativeModeTabs.NATURAL_BLOCKS);
        addCategory(CreativeModeTabs.FUNCTIONAL_BLOCKS);
        addCategory(CreativeModeTabs.REDSTONE_BLOCKS);
        addCategory(CreativeModeTabs.TOOLS_AND_UTILITIES);
        addCategory(CreativeModeTabs.COMBAT);
        addCategory(CreativeModeTabs.FOOD_AND_DRINKS);
        addCategory(CreativeModeTabs.INGREDIENTS);
        addCategory(CreativeModeTabs.SPAWN_EGGS);
        rebuildFilter();
    }

    private void addCategory(net.minecraft.resources.ResourceKey<CreativeModeTab> key) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(key);
        if (tab != null && tab.hasAnyItems()) categories.add(tab);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 8;
        titleLabelY = 9;

        buyTabButton = addRenderableWidget(Button.builder(Component.translatable("screen.items_shop.tab_buy"), button -> changeTab(ShopMenu.TAB_BUY))
                .bounds(leftPos + 8, topPos + 5, 82, 18).build());
        sellTabButton = addRenderableWidget(Button.builder(Component.translatable("screen.items_shop.tab_sell"), button -> changeTab(ShopMenu.TAB_SELL))
                .bounds(leftPos + 94, topPos + 5, 82, 18).build());

        searchBox = new EditBox(font, leftPos + GRID_X, topPos + 27, COLUMNS * CELL, 18,
                Component.translatable("screen.items_shop.search"));
        searchBox.setHint(Component.translatable("screen.items_shop.search_short"));
        searchBox.setMaxLength(80);
        searchBox.setResponder(value -> rebuildFilter());
        addRenderableWidget(searchBox);

        categoryButtons.clear();
        for (int index = 0; index < categories.size(); index++) {
            boolean leftSide = index < CATEGORY_SPLIT;
            int categoryX = leftSide ? CATEGORY_LEFT_X : CATEGORY_RIGHT_X;
            int categoryRow = leftSide ? index : index - CATEGORY_SPLIT + 1;
            CategoryButton button = new CategoryButton(index, leftPos + categoryX,
                    topPos + CATEGORY_Y + categoryRow * CATEGORY_STEP, categories.get(index));
            categoryButtons.add(addRenderableWidget(button));
        }

        quantityButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleQuantity())
                .bounds(leftPos + 236, topPos + 70, 108, 20).build());
        buyButton = addRenderableWidget(Button.builder(Component.empty(), button -> ShopNetwork.menuAction(menu.containerId, true))
                .bounds(leftPos + 236, topPos + 94, 108, 20).build());
        sellButton = addRenderableWidget(Button.builder(Component.empty(), button -> ShopNetwork.menuAction(menu.containerId, false))
                .bounds(leftPos + 126, topPos + 104, 108, 20).build());

        updateControls();
        ShopNetwork.requestBalance();
    }

    private void changeTab(int tab) {
        menu.setTabClient(tab);
        ShopNetwork.setTab(menu.containerId, tab);
        updateControls();
    }

    private void rebuildFilter() {
        String needle = searchBox == null ? "" : searchBox.getValue().toLowerCase(Locale.ROOT).trim();
        CreativeModeTab category = categories.isEmpty() ? null : categories.get(selectedCategory);
        filteredItems.clear();
        CreativeModeTab ingredients = BuiltInRegistries.CREATIVE_MODE_TAB.get(CreativeModeTabs.INGREDIENTS);
        for (ItemStack stack : allItems) {
            Item item = stack.getItem();
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            PriceManager.BookEnchantment book = PriceManager.singleBookEnchantment(stack);
            boolean inCategory = category == null || selectedCategory == 0
                    || (book != null ? category == ingredients
                    : category.getDisplayItems().stream().anyMatch(displayed -> displayed.is(item)));
            String bookSearch = book == null ? "" : book.id() + " "
                    + book.enchantment().getFullname(book.level()).getString().toLowerCase(Locale.ROOT);
            boolean matchesSearch = needle.isEmpty() || id.toString().contains(needle)
                    || PriceManager.displayName(stack).getString().toLowerCase(Locale.ROOT).contains(needle)
                    || bookSearch.contains(needle);
            if (inCategory && matchesSearch) filteredItems.add(stack);
        }
        scrollRow = 0;
    }

    private void selectCategory(int index) {
        if (index < 0 || index >= categories.size() || index == selectedCategory) return;
        selectedCategory = index;
        rebuildFilter();
    }

    private void select(ItemStack item) {
        if (menu.purchasePaid() && menu.getSlot(ShopMenu.PURCHASE_SLOT).hasItem()) return;
        selected = item.copy();
        quantity = Math.min(quantity, PriceManager.maxPurchaseQuantity(item));
        ShopNetwork.selectPurchase(menu.containerId, selected, quantity);
        updateControls();
    }

    private void cycleQuantity() {
        if (selected.isEmpty()) return;
        int max = PriceManager.maxPurchaseQuantity(selected);
        if (quantity < Math.min(16, max)) quantity = Math.min(16, max);
        else if (quantity < max) quantity = max;
        else quantity = 1;
        ShopNetwork.selectPurchase(menu.containerId, selected, quantity);
        updateControls();
    }

    public void onServerResponse(Component message, boolean success) {
        updateControls();
        if (!message.getString().isEmpty()) {
            notification = message;
            notificationSuccess = success;
            notificationUntil = Util.getMillis() + 4_000L;
        }
    }

    private void updateControls() {
        if (buyTabButton == null) return;
        boolean buying = menu.tab() == ShopMenu.TAB_BUY;
        buyTabButton.active = !buying;
        sellTabButton.active = buying;
        searchBox.visible = buying;
        int maxQuantity = PriceManager.maxPurchaseQuantity(selected);
        boolean quantityChoice = buying && maxQuantity > 1;
        quantityButton.visible = quantityChoice;
        buyButton.visible = buying;
        buyButton.setY(topPos + (quantityChoice ? 94 : 70));
        sellButton.visible = !buying;
        for (CategoryButton categoryButton : categoryButtons) categoryButton.visible = buying;

        quantityButton.setMessage(Component.translatable("screen.items_shop.quantity", quantity));
        ItemStack preview = menu.getSlot(ShopMenu.PURCHASE_SLOT).getItem();
        int count = preview.isEmpty() ? 0 : preview.getCount();
        quantityButton.active = !selected.isEmpty() && !menu.purchasePaid();
        buyButton.active = !preview.isEmpty() && !menu.purchasePaid();
        buyButton.setMessage(menu.purchasePaid()
                ? Component.translatable("screen.items_shop.take_item")
                : Component.translatable("screen.items_shop.pay", preview.isEmpty() ? 0L : (long) prices.buyPrice(preview) * count));

        ItemStack offered = menu.getSlot(ShopMenu.SALE_SLOT).getItem();
        sellButton.active = !offered.isEmpty();
        long salePrice = offered.isEmpty() ? 0L : (long) prices.sellPrice(offered) * offered.getCount();
        sellButton.setMessage(Component.translatable("screen.items_shop.sell_for", salePrice));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        searchBox.tick();
        if (!notification.getString().isEmpty() && Util.getMillis() >= notificationUntil) {
            notification = Component.empty();
        }
        updateControls();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xEE111722);
        graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFF202938);
        graphics.fill(x + 7, y + 25, x + imageWidth - 7, y + 142, 0xFF111722);
        graphics.fill(x + 95, y + 174, x + 265, y + 231, 0xFF111722);

        drawPlayerSlotFrames(graphics, x, y);
        if (menu.tab() == ShopMenu.TAB_BUY) drawPurchasePanel(graphics, x, y);
        else drawSalePanel(graphics, x, y);
        drawNotification(graphics, x, y);
    }

    private void drawPurchasePanel(GuiGraphics graphics, int x, int y) {
        int first = scrollRow * COLUMNS;
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int cellX = x + GRID_X + column * CELL;
                int cellY = y + GRID_Y + row * CELL;
                int index = first + row * COLUMNS + column;
                graphics.fill(cellX, cellY, cellX + 17, cellY + 17, 0xFF394557);
                graphics.fill(cellX + 1, cellY + 1, cellX + 16, cellY + 16, 0xFF18202D);
                if (index >= filteredItems.size()) continue;
                ItemStack item = filteredItems.get(index);
                if (ItemStack.isSameItemSameTags(item, selected)) graphics.fill(cellX, cellY, cellX + 17, cellY + 17, 0xAAE3B341);
                graphics.renderItem(item, cellX + 1, cellY + 1);
            }
        }
        drawSlotFrame(graphics, x + 279, y + 34, menu.purchasePaid() ? 0xFF57B56A : 0xFFE3B341);
        graphics.drawCenteredString(font, Component.translatable(menu.purchasePaid()
                        ? "screen.items_shop.paid_slot" : "screen.items_shop.purchase_slot"),
                x + 288, y + 56, menu.purchasePaid() ? 0x71E987 : 0xFFD966);
    }

    private void drawSalePanel(GuiGraphics graphics, int x, int y) {
        drawSlotFrame(graphics, x + 170, y + 64, 0xFFE06C75);
        ItemStack offered = menu.getSlot(ShopMenu.SALE_SLOT).getItem();
        Component hint = offered.isEmpty()
                ? Component.translatable("screen.items_shop.put_item")
                : Component.translatable("screen.items_shop.sale_value",
                    (long) prices.sellPrice(offered) * offered.getCount());
        graphics.drawCenteredString(font, hint, x + imageWidth / 2, y + 48, offered.isEmpty() ? 0xAAB4C4 : 0x71E987);
    }

    private void drawPlayerSlotFrames(GuiGraphics graphics, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) drawSlotFrame(graphics, x + 99 + column * 18, y + 175 + row * 18, 0xFF394557);
        }
        for (int column = 0; column < 9; column++) drawSlotFrame(graphics, x + 99 + column * 18, y + 234, 0xFF607793);
    }

    private void drawNotification(GuiGraphics graphics, int x, int y) {
        if (notification.getString().isEmpty() || Util.getMillis() >= notificationUntil) return;
        int border = notificationSuccess ? 0xFF57B56A : 0xFFE06C75;
        int textColor = notificationSuccess ? 0xFF9BF0AA : 0xFFFFA8AE;
        graphics.fill(x + 7, y + 143, x + imageWidth - 7, y + 162, border);
        graphics.fill(x + 9, y + 145, x + imageWidth - 9, y + 160, 0xF0111722);

        var lines = font.split(notification, imageWidth - 28);
        int shownLines = Math.min(2, lines.size());
        int textY = y + (shownLines == 1 ? 148 : 144);
        for (int line = 0; line < shownLines; line++) {
            graphics.drawCenteredString(font, lines.get(line), x + imageWidth / 2, textY + line * 9, textColor);
        }
    }

    private static void drawSlotFrame(GuiGraphics graphics, int x, int y, int border) {
        graphics.fill(x, y, x + 18, y + 18, border);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF0D121B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component balance = Component.translatable("screen.items_shop.balance", ClientState.balance());
        graphics.drawString(font, balance, imageWidth - 8 - font.width(balance), 11, 0xFFD966, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xCBD5E1, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (menu.tab() == ShopMenu.TAB_BUY) {
            ItemStack hovered = itemAt(mouseX, mouseY);
            if (hovered != null) {
                List<Component> tooltip = new ArrayList<>(getTooltipFromContainerItem(hovered));
                tooltip.add(Component.translatable("screen.items_shop.tooltip_price", prices.buyPrice(hovered), prices.sellPrice(hovered)));
                graphics.renderTooltip(font, tooltip, hovered.getTooltipImage(), mouseX, mouseY);
            }
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private ItemStack itemAt(double mouseX, double mouseY) {
        int localX = (int) mouseX - leftPos - GRID_X;
        int localY = (int) mouseY - topPos - GRID_Y;
        if (localX < 0 || localY < 0 || localX >= COLUMNS * CELL || localY >= ROWS * CELL) return null;
        int index = scrollRow * COLUMNS + (localY / CELL) * COLUMNS + localX / CELL;
        return index >= 0 && index < filteredItems.size() ? filteredItems.get(index) : null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchBox != null && !searchBox.isMouseOver(mouseX, mouseY)) {
            searchBox.setFocused(false);
            if (getFocused() == searchBox) setFocused(null);
        }
        if (menu.tab() == ShopMenu.TAB_BUY && button == 0) {
            ItemStack item = itemAt(mouseX, mouseY);
            if (item != null) {
                select(item);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int localX = (int) mouseX - leftPos - GRID_X;
        int localY = (int) mouseY - topPos - GRID_Y;
        boolean overGrid = localX >= 0 && localY >= 0 && localX < COLUMNS * CELL && localY < ROWS * CELL;
        if (menu.tab() == ShopMenu.TAB_BUY && overGrid) {
            int maxRows = Math.max(0, (filteredItems.size() + COLUMNS - 1) / COLUMNS - ROWS);
            scrollRow = Mth.clamp(scrollRow - (int) Math.signum(amount), 0, maxRows);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (searchBox != null && searchBox.isFocused()) {
            searchBox.keyPressed(keyCode, scanCode, modifiers);
            // While typing, do not pass keys to AbstractContainerScreen: it
            // treats inventory/hotbar key bindings as container actions.
            return true;
        }
        if (ClientEvents.OPEN_SHOP.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private final class CategoryButton extends AbstractButton {
        private final int categoryIndex;
        private final ItemStack icon;

        private CategoryButton(int categoryIndex, int x, int y, CreativeModeTab category) {
            super(x, y, 18, 18, category.getDisplayName());
            this.categoryIndex = categoryIndex;
            this.icon = category.getIconItem();
            setTooltip(Tooltip.create(category.getDisplayName()));
        }

        @Override
        public void onPress() {
            selectCategory(categoryIndex);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int border = categoryIndex == selectedCategory ? 0xFFE3B341 : isHovered() ? 0xFF718096 : 0xFF394557;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xFF18202D);
            graphics.renderItem(icon, getX() + 1, getY() + 1);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
