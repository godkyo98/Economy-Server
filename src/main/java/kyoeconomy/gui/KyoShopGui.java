package kyoeconomy.gui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import kyoeconomy.config.ShopConfigManager;
import kyoeconomy.data.KyoEconomyState;
import kyoeconomy.data.ShopItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;
import java.util.stream.Collectors;

public class KyoShopGui extends SimpleGui {

    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 45;
    private static final double SELL_RATIO = 0.10; // Tỉ lệ thu mua 10% từ giá sàn

    private String currentCategory = "";

    // --- CONSTRUCTOR 1: Mở Sảnh Chọn Danh Mục Gốc (9x3) ---
    public KyoShopGui(ServerPlayer player) {
        super(MenuType.GENERIC_9x3, player, false);
        renderMenu();
    }

    // --- CONSTRUCTOR 2: Mở Cửa Hàng Chi Tiết Theo Nhóm (9x6) ---
    public KyoShopGui(ServerPlayer player, String categoryId) {
        super(MenuType.GENERIC_9x6, player, false);
        this.currentCategory = categoryId;
        this.currentPage = 0;
        renderPage();
    }

    // --- TẦNG 1: SẢNH CHỌN DANH MỤC (TAB) ---
    private void renderMenu() {
        this.setTitle(Component.literal("Danh Mục Cửa Hàng Máy Chủ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (int i = 0; i < this.getSize(); i++) {
            this.clearSlot(i);
        }

        this.setSlot(10, GuiElementBuilder.from(new ItemStack(Items.OAK_LOG))
            .setName(Component.literal("🧱 Khối Xây Dựng & Cảnh Quan").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .addLoreLine(Component.literal("Chuyên mục: Các loại gỗ, đá, đất, cát...").withStyle(ChatFormatting.GRAY))
            .setCallback((idx, t, act, gui) -> openCategoryTab("blocks")));

        this.setSlot(11, GuiElementBuilder.from(new ItemStack(Items.BREAD))
            .setName(Component.literal("🍎 Nông Sản & Thức Ăn").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
            .addLoreLine(Component.literal("Chuyên mục: Lúa mì, thịt, rau củ, hạt giống...").withStyle(ChatFormatting.GRAY))
            .setCallback((idx, t, act, gui) -> openCategoryTab("food")));

        this.setSlot(12, GuiElementBuilder.from(new ItemStack(Items.RAW_GOLD))
            .setName(Component.literal("⚙️ Nguyên Liệu Chế Tạo").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
            .addLoreLine(Component.literal("Chuyên mục: Đá lửa, phôi quặng, nguyên liệu gốc...").withStyle(ChatFormatting.GRAY))
            .setCallback((idx, t, act, gui) -> openCategoryTab("materials")));

        this.setSlot(14, GuiElementBuilder.from(new ItemStack(Items.DIAMOND_CHESTPLATE))
            .setName(Component.literal("🛡️ Trang Bị & Vũ Khí").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD))
            .addLoreLine(Component.literal("Chuyên mục: Áo giáp, kiếm, công cụ lao động...").withStyle(ChatFormatting.GRAY))
            .setCallback((idx, t, act, gui) -> openCategoryTab("equipment")));

        this.setSlot(15, GuiElementBuilder.from(new ItemStack(Items.ROTTEN_FLESH))
            .setName(Component.literal("💀 Chiến Lợi Phẩm Quái Vật").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
            .addLoreLine(Component.literal("Chuyên mục: Thịt thối, xương, thuốc súng, ngọc ender...").withStyle(ChatFormatting.GRAY))
            .setCallback((idx, t, act, gui) -> openCategoryTab("mob_drops")));

        this.setSlot(16, GuiElementBuilder.from(new ItemStack(Items.HEART_OF_THE_SEA))
            .setName(Component.literal("🌟 Đồ Đặc Biệt & Hiếm").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
            .addLoreLine(Component.literal("Chuyên mục: Trái tim biển cả, sách phù phép...").withStyle(ChatFormatting.GRAY))
            .setCallback((idx, t, act, gui) -> openCategoryTab("special")));
    }

    private void openCategoryTab(String catId) {
        this.close();
        KyoShopGui categoryMenu = new KyoShopGui(this.player, catId);
        categoryMenu.open();
        this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 0.5f, 1.0f);
    }

    // --- TẦNG 2: CỬA HÀNG BÁN CHI TIẾT THEO TAB ---
    private void renderPage() {
        this.setTitle(Component.literal("Cửa Hàng ➔ ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal(getCategoryFriendlyName(currentCategory)).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)));

        for (int i = 0; i < this.getSize(); i++) {
            this.clearSlot(i);
        }

        List<ShopItem> filteredItems = ShopConfigManager.SHOP_ITEMS.stream()
            .filter(item -> item.category.equals(currentCategory))
            .collect(Collectors.toList());

        int maxPages = (int) Math.ceil((double) filteredItems.size() / ITEMS_PER_PAGE);
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            ShopItem shopItem = filteredItems.get(i);

            long baseBuyPriceInHao = shopItem.getCurrentPrice();
            long fixedSellPriceInHao = getFixedSellPrice(shopItem);

            GuiElementBuilder elementBuilder = GuiElementBuilder.from(new ItemStack(shopItem.item))
                .setName(Component.literal(shopItem.name).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .addLoreLine(Component.literal(""))
                .addLoreLine(Component.literal("Giá mua: ").withStyle(ChatFormatting.GREEN).append(formatMoney(baseBuyPriceInHao)))
                .addLoreLine(Component.literal("Giá bán cơ bản: ").withStyle(ChatFormatting.RED).append(formatMoney(fixedSellPriceInHao)))
                .addLoreLine(Component.literal(""))
                .addLoreLine(Component.literal("🛒 Tương tác trên Shop (Khung trên):").withStyle(ChatFormatting.GRAY))
                .addLoreLine(Component.literal("   Chuột Trái: Mua 1 | Chuột Phải: Bán 1 | Shift+Phải: Bán hết").withStyle(ChatFormatting.DARK_GRAY))
                .addLoreLine(Component.literal("⚡ Mẹo tương tác đồ trong Túi của bạn (Khung dưới):").withStyle(ChatFormatting.LIGHT_PURPLE))
                .addLoreLine(Component.literal("   Chuột Phải: Thẩm định giá | Shift+Phải: BÁN ĐỨT").withStyle(ChatFormatting.YELLOW))
                .addLoreLine(Component.literal(""))
                .setCallback((indexSlot, type, actionSlot, gui) -> {
                    if (type.isLeft) {
                        handleBuy(this.player, shopItem.item, baseBuyPriceInHao, 1);
                    } else if (type.isRight) {
                        if (type.shift) {
                            handleSellAll(this.player, shopItem.item, fixedSellPriceInHao);
                        } else {
                            handleSell(this.player, shopItem.item, fixedSellPriceInHao, 1);
                        }
                    }
                });

            int displaySlot = i - startIndex;
            this.setSlot(displaySlot, elementBuilder);
        }

        if (currentPage > 0) {
            GuiElementBuilder prevButton = GuiElementBuilder.from(new ItemStack(Items.ARROW))
                .setName(Component.literal("Trang Trước").withStyle(ChatFormatting.YELLOW))
                .setCallback((index, type, action, gui) -> {
                    currentPage--;
                    this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
                    renderPage();
                });
            this.setSlot(45, prevButton);
        }

        if (currentPage < maxPages - 1 && maxPages > 0) {
            GuiElementBuilder nextButton = GuiElementBuilder.from(new ItemStack(Items.ARROW))
                .setName(Component.literal("Trang Sau").withStyle(ChatFormatting.YELLOW))
                .setCallback((index, type, action, gui) -> {
                    currentPage++;
                    this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
                    renderPage();
                });
            this.setSlot(53, nextButton);
        }

        GuiElementBuilder backButton = GuiElementBuilder.from(new ItemStack(Items.OAK_DOOR))
            .setName(Component.literal("🔙 Quay Lại Danh Mục").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
            .addLoreLine(Component.literal("Bấm để quay về Sảnh chọn loại cửa hàng.").withStyle(ChatFormatting.GRAY))
            .setCallback((index, type, action, gui) -> {
                this.close();
                KyoShopGui mainMenu = new KyoShopGui(this.player);
                mainMenu.open();
                this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.CHEST_CLOSE, SoundSource.PLAYERS, 0.5f, 0.8f);
            });
        this.setSlot(46, backButton);

        GuiElementBuilder infoButton = GuiElementBuilder.from(new ItemStack(Items.PAPER))
            .setName(Component.literal("Trang " + (currentPage + 1) + " / " + Math.max(1, maxPages)).withStyle(ChatFormatting.WHITE));
        this.setSlot(49, infoButton);
    }

    private String getCategoryFriendlyName(String catId) {
        switch (catId) {
            case "blocks": return "Khối Xây Dựng";
            case "food": return "Nông Sản & Thực Phẩm";
            case "materials": return "Nguyên Liệu Chế Tạo";
            case "equipment": return "Trang Bị & Vũ Khí";
            case "mob_drops": return "Chiến Lợi Phẩm Quái Vật";
            case "special": return "Đồ Đặc Biệt & Hiếm";
            default: return "Vật Phẩm Khác";
        }
    }

    // 🔥 TRUNG TÂM PHÂN LUỒNG TƯƠNG TÁC
    @Override
    public boolean onAnyClick(int index, eu.pb4.sgui.api.ClickType type, ContainerInput action) {
        if (index < 0) return super.onAnyClick(index, type, action);

        // Chặn tương tác nếu đang ở Sảnh Chính 9x3
        if (currentCategory.isEmpty() && index >= this.getSize()) {
            return true;
        }

        // 🎰 CƠ CHẾ 1: ĐƯA ĐỒ VÀO SHOP GUI ĐỂ BÁN (KÉO & THẢ)
        ItemStack carried = this.player.containerMenu.getCarried();
        if (index < this.getSize() && !carried.isEmpty()) {
            ShopItem targetShopItem = getShopItem(carried.getItem());
            if (targetShopItem != null) {
                long fixedBaseSellPrice = getFixedSellPrice(targetShopItem);
                int count = carried.getCount();
                long enchantBonus = calculateEnchantmentBonus(carried);
                long totalRevenue = (fixedBaseSellPrice * count) + (enchantBonus * count);

                this.player.containerMenu.setCarried(ItemStack.EMPTY);

                KyoEconomyState state = KyoEconomyState.getServerState(this.player.level().getServer());
                state.addBalance(this.player.getUUID(), totalRevenue);

                this.player.sendSystemMessage(Component.literal("Đã thu mua ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(count + "x ").withStyle(ChatFormatting.YELLOW))
                    .append(carried.getHoverName())
                    .append(Component.literal(" từ con trỏ, cộng thêm ").withStyle(ChatFormatting.GREEN))
                    .append(formatMoney(totalRevenue)).append(Component.literal("!").withStyle(ChatFormatting.GREEN)));
                this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.0f);
                return true;
            }
        }

        // 🎰 CƠ CHẾ 2: TƯƠNG TÁC TẠI TÚI ĐỒ NGƯỜI CHƠI
        if (index >= this.getSize()) {
            try {
                ItemStack itemInSlot = this.player.containerMenu.getSlot(index).getItem();
                if (!itemInSlot.isEmpty()) {
                    ShopItem targetShopItem = getShopItem(itemInSlot.getItem());

                    if (targetShopItem == null) {
                        if (type.isRight) {
                            this.player.sendSystemMessage(Component.literal("Vật phẩm này không được thu mua tại Cửa Hàng Máy Chủ!").withStyle(ChatFormatting.RED));
                            this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
                            return true;
                        }
                        return super.onAnyClick(index, type, action);
                    }

                    // THAO TÁC CỰC KỲ AN TOÀN BẰNG CHUỘT PHẢI
                    if (type.isRight) {
                        if (type.shift) {
                            // BÁN ĐỨT CẢ KHỐI ĐỒ (Hoặc 1 món nếu là đồ không cộng dồn)
                            handleInventorySellItem(itemInSlot, targetShopItem);
                        } else {
                            // THẨM ĐỊNH GIÁ (Price check)
                            handleInventoryPriceCheck(itemInSlot, targetShopItem);
                        }
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }

        return super.onAnyClick(index, type, action);
    }

    // Hàm xử lý bán đứt (Bất chấp stack hay lẻ)
    private void handleInventorySellItem(ItemStack stack, ShopItem targetShopItem) {
        long fixedBaseSellPrice = getFixedSellPrice(targetShopItem);
        int count = stack.getCount();
        long enchantBonus = calculateEnchantmentBonus(stack);
        long totalRevenue = (fixedBaseSellPrice * count) + (enchantBonus * count);

        // Bốc hơi lượng vật phẩm đó
        stack.setCount(0);
        this.player.getInventory().setChanged();

        KyoEconomyState state = KyoEconomyState.getServerState(this.player.level().getServer());
        state.addBalance(this.player.getUUID(), totalRevenue);

        this.player.sendSystemMessage(Component.literal("Đã bán đứt ").withStyle(ChatFormatting.GREEN)
            .append(Component.literal(count + "x ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(targetShopItem.name)).append(Component.literal(", thu về ").withStyle(ChatFormatting.GREEN))
            .append(formatMoney(totalRevenue)).append(Component.literal("!").withStyle(ChatFormatting.GREEN)));
        this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private void handleInventoryPriceCheck(ItemStack stack, ShopItem targetShopItem) {
        long fixedBaseSellPrice = getFixedSellPrice(targetShopItem);
        long enchantBonus = calculateEnchantmentBonus(stack);
        long finalPriceInHao = fixedBaseSellPrice + enchantBonus;

        this.player.sendSystemMessage(Component.literal("------- 🔍 KẾT QUẢ THẨM ĐỊNH GIÁ CƠ BẢN -------").withStyle(ChatFormatting.LIGHT_PURPLE));
        this.player.sendSystemMessage(Component.literal("Vật phẩm: ").withStyle(ChatFormatting.GRAY).append(stack.getHoverName()));
        this.player.sendSystemMessage(Component.literal("• Giá trị phế liệu gốc (1x): ").withStyle(ChatFormatting.GRAY).append(formatMoney(fixedBaseSellPrice)));
        this.player.sendSystemMessage(Component.literal("• Giá trị phù phép tích hợp (1x): ").withStyle(ChatFormatting.GRAY).append(formatMoney(enchantBonus)));
        this.player.sendSystemMessage(Component.literal("➡ Tổng thu mua dự kiến cho MỘT món: ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD).append(formatMoney(finalPriceInHao)));
        this.player.sendSystemMessage(Component.literal("--------------------------------------------").withStyle(ChatFormatting.LIGHT_PURPLE));
        this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    // --- CÁC HÀM TIỆN ÍCH DƯỚI ĐÂY GIỮ NGUYÊN HOẠT ĐỘNG HOÀN HẢO ---
    private MutableComponent formatMoney(long totalHao) {
        long xu = totalHao / 1000;
        long hao = totalHao % 1000;
        if (xu > 0 && hao > 0) {
            return Component.literal(String.format("%,d Xu ", xu)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal(String.format("%,d Hào", hao)).withStyle(ChatFormatting.YELLOW));
        }
        if (xu > 0) return Component.literal(String.format("%,d Xu", xu)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        return Component.literal(String.format("%,d Hào", hao)).withStyle(ChatFormatting.YELLOW);
    }

    private long calculateEnchantmentBonus(ItemStack stack) {
        long bonus = 0;
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) enchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> enchantHolder = entry.getKey();
            int level = entry.getIntValue();
            String enchantDescription = enchantHolder.toString().toLowerCase();

            if (enchantDescription.contains("curse_of_binding") || enchantDescription.contains("curse_of_vanishing")) {
                bonus += 1000L;
            } else if (enchantDescription.contains("mending")) {
                bonus += 200000L;
            } else if (enchantDescription.contains("fortune") || enchantDescription.contains("looting") || enchantDescription.contains("silk_touch") ||
                enchantDescription.contains("infinity") || enchantDescription.contains("wind_burst") || enchantDescription.contains("swift_sneak")) {
                bonus += (level * 50000L);
            } else if (enchantDescription.contains("sharpness") || enchantDescription.contains("efficiency") || enchantDescription.contains("protection") ||
                enchantDescription.contains("unbreaking") || enchantDescription.contains("power") || enchantDescription.contains("breach") ||
                enchantDescription.contains("density") || enchantDescription.contains("sweeping_edge") || enchantDescription.contains("lunge") ||
                enchantDescription.contains("channeling") || enchantDescription.contains("multishot") || enchantDescription.contains("piercing") ||
                enchantDescription.contains("quick_charge") || enchantDescription.contains("riptide") || enchantDescription.contains("loyalty")) {
                bonus += (level * 25000L);
            } else if (enchantDescription.contains("smite") || enchantDescription.contains("bane_of_arthropods") || enchantDescription.contains("knockback") ||
                enchantDescription.contains("punch") || enchantDescription.contains("fire_aspect") || enchantDescription.contains("flame") ||
                enchantDescription.contains("thorns") || enchantDescription.contains("feather_falling") || enchantDescription.contains("depth_strider") ||
                enchantDescription.contains("frost_walker") || enchantDescription.contains("soul_speed") || enchantDescription.contains("respiration") ||
                enchantDescription.contains("aqua_affinity") || enchantDescription.contains("luck_of_the_sea") || enchantDescription.contains("lure") ||
                enchantDescription.contains("impaling")) {
                bonus += (level * 10000L);
            } else {
                bonus += (level * 5000L);
            }
        }
        return bonus;
    }

    private ShopItem getShopItem(Item item) {
        for (ShopItem shopItem : ShopConfigManager.SHOP_ITEMS) {
            if (shopItem.item == item) return shopItem;
        }
        return null;
    }

    private long getFixedSellPrice(ShopItem item) {
        long referencePrice = item.minPrice > 0 ? item.minPrice : item.price;
        return (long) Math.max(1, referencePrice * SELL_RATIO);
    }

    private void handleSell(ServerPlayer seller, Item itemType, long basePriceInHao, int amount) {
        int slotWithItem = -1;
        long enchantBonus = 0;
        for (int i = 0; i < seller.getInventory().getContainerSize(); i++) {
            ItemStack stack = seller.getInventory().getItem(i);
            if (stack.getItem() == itemType && stack.getCount() >= amount) {
                slotWithItem = i;
                enchantBonus = calculateEnchantmentBonus(stack);
                break;
            }
        }

        if (slotWithItem != -1) {
            seller.getInventory().getItem(slotWithItem).shrink(amount);
            seller.getInventory().setChanged();
            long finalPrice = basePriceInHao + enchantBonus;
            KyoEconomyState state = KyoEconomyState.getServerState(seller.level().getServer());
            state.addBalance(seller.getUUID(), finalPrice);
            seller.sendSystemMessage(Component.literal("Đã bán thành công, thu về ").withStyle(ChatFormatting.GREEN)
                .append(formatMoney(finalPrice)).append(Component.literal("!").withStyle(ChatFormatting.GREEN)));
            seller.level().playSound(null, seller.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            seller.sendSystemMessage(Component.literal("Bạn không có vật phẩm này trong hành trang để bán!").withStyle(ChatFormatting.RED));
            seller.level().playSound(null, seller.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    private void handleSellAll(ServerPlayer seller, Item itemType, long basePriceInHao) {
        int totalAmount = 0;
        long totalRevenue = 0;
        for (int i = 0; i < seller.getInventory().getContainerSize(); i++) {
            ItemStack stack = seller.getInventory().getItem(i);
            if (stack.getItem() == itemType) {
                int count = stack.getCount();
                long itemEnchantBonus = calculateEnchantmentBonus(stack);
                totalRevenue += (basePriceInHao * count) + (itemEnchantBonus * count);
                totalAmount += count;
                stack.setCount(0);
            }
        }

        if (totalAmount > 0) {
            seller.getInventory().setChanged();
            KyoEconomyState state = KyoEconomyState.getServerState(seller.level().getServer());
            state.addBalance(seller.getUUID(), totalRevenue);
            seller.sendSystemMessage(Component.literal("Đã xả kho thành công " + totalAmount + " vật phẩm, thu về ").withStyle(ChatFormatting.GREEN)
                .append(formatMoney(totalRevenue)).append(Component.literal("!").withStyle(ChatFormatting.GREEN)));
            seller.level().playSound(null, seller.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            seller.sendSystemMessage(Component.literal("Bạn không có vật phẩm này trong hành trang để xả kho!").withStyle(ChatFormatting.RED));
            seller.level().playSound(null, seller.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    private void handleBuy(ServerPlayer buyer, Item itemType, long priceInHao, int amount) {
        KyoEconomyState state = KyoEconomyState.getServerState(buyer.level().getServer());
        long currentBal = state.getBalance(buyer.getUUID());

        if (currentBal >= priceInHao) {
            state.removeBalance(buyer.getUUID(), priceInHao);
            ItemStack purchasedItem = new ItemStack(itemType, amount);
            if (!buyer.getInventory().add(purchasedItem)) buyer.drop(purchasedItem, false);
            buyer.sendSystemMessage(Component.literal("Đã mua thành công với giá ").withStyle(ChatFormatting.GREEN)
                .append(formatMoney(priceInHao)).append(Component.literal("!").withStyle(ChatFormatting.GREEN)));
            buyer.level().playSound(null, buyer.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.0f);
        } else {
            buyer.sendSystemMessage(Component.literal("Bạn không có đủ ").withStyle(ChatFormatting.RED)
                .append(formatMoney(priceInHao)).append(Component.literal(" để mua vật phẩm này!").withStyle(ChatFormatting.RED)));
            buyer.level().playSound(null, buyer.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }
}