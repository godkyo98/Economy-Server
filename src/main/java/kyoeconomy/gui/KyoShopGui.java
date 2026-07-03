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

public class KyoShopGui extends SimpleGui {

    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 45;
    private static final double SELL_RATIO = 0.10; // Tỉ lệ thu mua 10% từ giá cơ bản gốc

    public KyoShopGui(ServerPlayer player) {
        super(MenuType.GENERIC_9x6, player, false);
        this.setTitle(Component.literal("Cửa Hàng Máy Chủ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        renderPage();
    }

    @Override
    public boolean onAnyClick(int index, eu.pb4.sgui.api.ClickType type, ContainerInput action) {
        if (index < 0) {
            return super.onAnyClick(index, type, action);
        }

        // Tương tác trực tiếp trên các ô túi đồ của người chơi (Khung dưới)
        if (index >= this.getSize()) {
            if (type.isRight) {
                try {
                    ItemStack itemInInventory = this.player.containerMenu.getSlot(index).getItem();
                    if (!itemInInventory.isEmpty()) {

                        // Chỉ áp dụng cho đồ không chồng được (Công cụ, vũ khí, giáp...)
                        if (itemInInventory.getMaxStackSize() > 1) {
                            return super.onAnyClick(index, type, action);
                        }

                        if (type.shift) {
                            // SHIFT + PHẢI: Tiến hành bán đứt vật phẩm
                            handleInventoryRightClickSell(itemInInventory);
                        } else {
                            // CHUỘT PHẢI ĐƠN THUẦN: Chỉ kiểm tra và thẩm định giá, KHÔNG bán đồ
                            handleInventoryPriceCheck(itemInInventory);
                        }
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
            return super.onAnyClick(index, type, action);
        }
        return super.onAnyClick(index, type, action);
    }

    private MutableComponent formatMoney(long totalHao) {
        long xu = totalHao / 1000;
        long hao = totalHao % 1000;

        if (xu > 0 && hao > 0) {
            return Component.literal(String.format("%,d Xu ", xu)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal(String.format("%,d Hào", hao)).withStyle(ChatFormatting.YELLOW));
        }
        if (xu > 0) {
            return Component.literal(String.format("%,d Xu", xu)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        }
        return Component.literal(String.format("%,d Hào", hao)).withStyle(ChatFormatting.YELLOW);
    }

    private long calculateEnchantmentBonus(ItemStack stack) {
        long bonus = 0;
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) {
            enchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        }

        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> enchantHolder = entry.getKey();
            int level = entry.getIntValue();
            String enchantDescription = enchantHolder.toString().toLowerCase();

            // 📈 ĐÃ NÂNG TOÀN BỘ GIÁ TRỊ PHÙ PHÉP LÊN 1 BẬC LỚN ĐỂ TĂNG GIÁ TRỊ CÀY CUỐC
            if (enchantDescription.contains("curse_of_binding") || enchantDescription.contains("curse_of_vanishing")) {
                bonus += 1000L; // Lời nguyền: 1 Xu
            } else if (enchantDescription.contains("mending")) {
                bonus += 200000L; // Sửa chữa (Thần cấp): 200 Xu
            } else if (enchantDescription.contains("fortune") || enchantDescription.contains("looting") || enchantDescription.contains("silk_touch") ||
                enchantDescription.contains("infinity") || enchantDescription.contains("wind_burst") || enchantDescription.contains("swift_sneak")) {
                bonus += (level * 50000L); // Bậc Tối Thượng: 50 Xu / Cấp độ
            } else if (enchantDescription.contains("sharpness") || enchantDescription.contains("efficiency") || enchantDescription.contains("protection") ||
                enchantDescription.contains("unbreaking") || enchantDescription.contains("power") || enchantDescription.contains("breach") ||
                enchantDescription.contains("density") || enchantDescription.contains("sweeping_edge") || enchantDescription.contains("lunge") ||
                enchantDescription.contains("channeling") || enchantDescription.contains("multishot") || enchantDescription.contains("piercing") ||
                enchantDescription.contains("quick_charge") || enchantDescription.contains("riptide") || enchantDescription.contains("loyalty")) {
                bonus += (level * 25000L); // Bậc Cao Cấp: 25 Xu / Cấp độ
            } else if (enchantDescription.contains("smite") || enchantDescription.contains("bane_of_arthropods") || enchantDescription.contains("knockback") ||
                enchantDescription.contains("punch") || enchantDescription.contains("fire_aspect") || enchantDescription.contains("flame") ||
                enchantDescription.contains("thorns") || enchantDescription.contains("feather_falling") || enchantDescription.contains("depth_strider") ||
                enchantDescription.contains("frost_walker") || enchantDescription.contains("soul_speed") || enchantDescription.contains("respiration") ||
                enchantDescription.contains("aqua_affinity") || enchantDescription.contains("luck_of_the_sea") || enchantDescription.contains("lure") ||
                enchantDescription.contains("impaling")) {
                bonus += (level * 10000L); // Bậc Trung Cấp: 10 Xu / Cấp độ
            } else {
                bonus += (level * 5000L); // Bậc Thấp Nhất (Bậc kém nhất): Tăng lên chuẩn 5 Xu (5,000 Hào) theo yêu cầu!
            }
        }
        return bonus;
    }

    private ShopItem getShopItem(Item item) {
        for (ShopItem shopItem : ShopConfigManager.SHOP_ITEMS) {
            if (shopItem.item == item) {
                return shopItem;
            }
        }
        return null;
    }

    private long getFixedSellPrice(ShopItem item) {
        long referencePrice = item.minPrice > 0 ? item.minPrice : item.price;
        return (long) Math.max(1, referencePrice * SELL_RATIO);
    }

    // 🔍 MÁY THẨM ĐỊNH GIÁ TRỊ VẬT PHẨM (KHÔNG THU HỒI ĐỒ)
    private void handleInventoryPriceCheck(ItemStack stack) {
        ShopItem targetShopItem = getShopItem(stack.getItem());

        if (targetShopItem == null) {
            this.player.sendSystemMessage(Component.literal("Vật phẩm này không có trên sàn giao dịch máy chủ!").withStyle(ChatFormatting.RED));
            this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }

        long fixedBaseSellPrice = getFixedSellPrice(targetShopItem);
        long enchantBonus = calculateEnchantmentBonus(stack);
        long finalPriceInHao = fixedBaseSellPrice + enchantBonus;

        // In ra hóa đơn thẩm định chuyên nghiệp vào khung chat cá nhân
        this.player.sendSystemMessage(Component.literal("------- 🔍 KẾT QUẢ THẨM ĐỊNH GIÁ CƠ BẢN -------").withStyle(ChatFormatting.LIGHT_PURPLE));
        this.player.sendSystemMessage(Component.literal("Vật phẩm: ").withStyle(ChatFormatting.GRAY).append(stack.getHoverName()));
        this.player.sendSystemMessage(Component.literal("• Giá trị phế liệu gốc: ").withStyle(ChatFormatting.GRAY).append(formatMoney(fixedBaseSellPrice)));
        this.player.sendSystemMessage(Component.literal("• Giá trị phù phép tích hợp: ").withStyle(ChatFormatting.GRAY).append(formatMoney(enchantBonus)));
        this.player.sendSystemMessage(Component.literal("➡ Tổng giá trị thu mua dự kiến: ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD).append(formatMoney(finalPriceInHao)));
        this.player.sendSystemMessage(Component.literal("--------------------------------------------").withStyle(ChatFormatting.LIGHT_PURPLE));

        this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    private void handleInventoryRightClickSell(ItemStack stack) {
        ShopItem targetShopItem = getShopItem(stack.getItem());

        if (targetShopItem == null) {
            this.player.sendSystemMessage(Component.literal("Vật phẩm này không được thu mua tại Cửa Hàng Máy Chủ!").withStyle(ChatFormatting.RED));
            this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }

        long fixedBaseSellPrice = getFixedSellPrice(targetShopItem);
        long enchantBonus = calculateEnchantmentBonus(stack);
        long finalPriceInHao = fixedBaseSellPrice + enchantBonus;

        stack.shrink(1);
        this.player.getInventory().setChanged();

        KyoEconomyState state = KyoEconomyState.getServerState(this.player.level().getServer());
        state.addBalance(this.player.getUUID(), finalPriceInHao);

        this.player.sendSystemMessage(Component.literal("Đã bán nhanh " + targetShopItem.name + ", thu về ").withStyle(ChatFormatting.GREEN)
            .append(formatMoney(finalPriceInHao)).append(Component.literal("!").withStyle(ChatFormatting.GREEN)));
        this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private void handleSellAllFromInventory(ItemStack clickedStack) {
        Item itemType = clickedStack.getItem();
        ShopItem targetShopItem = getShopItem(itemType);

        if (targetShopItem == null) {
            this.player.sendSystemMessage(Component.literal("Vật phẩm này không được thu mua tại Cửa Hàng Máy Chủ!").withStyle(ChatFormatting.RED));
            this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }

        long fixedBaseSellPrice = getFixedSellPrice(targetShopItem);

        int totalAmount = 0;
        long totalRevenue = 0;

        for (int i = 0; i < this.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.player.getInventory().getItem(i);
            if (stack.getItem() == itemType) {
                int count = stack.getCount();
                long itemEnchantBonus = calculateEnchantmentBonus(stack);
                totalRevenue += (fixedBaseSellPrice * count) + (itemEnchantBonus * count);
                totalAmount += count;
                stack.setCount(0);
            }
        }

        if (totalAmount > 0) {
            this.player.getInventory().setChanged();
            KyoEconomyState state = KyoEconomyState.getServerState(this.player.level().getServer());
            state.addBalance(this.player.getUUID(), totalRevenue);

            this.player.sendSystemMessage(Component.literal("Đã xả kho thành công " + totalAmount + "x " + targetShopItem.name + ", thu về ").withStyle(ChatFormatting.GREEN)
                .append(formatMoney(totalRevenue)).append(Component.literal("!").withStyle(ChatFormatting.GREEN)));
            this.player.getInventory().setChanged();
            this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
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

    private void renderPage() {
        for (int i = 0; i < this.getSize(); i++) {
            this.clearSlot(i);
        }

        List<ShopItem> allItems = ShopConfigManager.SHOP_ITEMS;
        int maxPages = (int) Math.ceil((double) allItems.size() / ITEMS_PER_PAGE);
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            ShopItem shopItem = allItems.get(i);

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
                .addLoreLine(Component.literal("   Chuột Phải: Thẩm định giá phù phép | Shift+Phải: Bán nhanh đồ").withStyle(ChatFormatting.YELLOW))
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

        GuiElementBuilder infoButton = GuiElementBuilder.from(new ItemStack(Items.PAPER))
            .setName(Component.literal("Trang " + (currentPage + 1) + " / " + Math.max(1, maxPages)).withStyle(ChatFormatting.WHITE));
        this.setSlot(49, infoButton);
    }

    private void handleBuy(ServerPlayer buyer, Item itemType, long priceInHao, int amount) {
        KyoEconomyState state = KyoEconomyState.getServerState(buyer.level().getServer());
        long currentBal = state.getBalance(buyer.getUUID());

        if (currentBal >= priceInHao) {
            state.removeBalance(buyer.getUUID(), priceInHao);
            ItemStack purchasedItem = new ItemStack(itemType, amount);

            if (!buyer.getInventory().add(purchasedItem)) {
                buyer.drop(purchasedItem, false);
            }
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