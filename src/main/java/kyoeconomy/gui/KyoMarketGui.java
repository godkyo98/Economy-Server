package kyoeconomy.gui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import kyoeconomy.data.KyoEconomyState;
import kyoeconomy.data.KyoMarketState;
import kyoeconomy.data.MarketItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class KyoMarketGui extends SimpleGui {

    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 45;

    public KyoMarketGui(ServerPlayer player) {
        super(MenuType.GENERIC_9x6, player, false);
        this.setTitle(Component.literal("Chợ Đen Người Chơi").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        renderPage();
    }

    // Hàm tiện ích: Trả về Component hỗ trợ Đa Màu Sắc (Xu màu đậm, Hào màu nhạt)
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

    private void renderPage() {
        for (int i = 0; i < this.getSize(); i++) {
            this.clearSlot(i);
        }

        KyoMarketState marketState = KyoMarketState.getServerState(this.player.level().getServer());
        List<MarketItem> allItems = marketState.getListings();

        int maxPages = (int) Math.ceil((double) allItems.size() / ITEMS_PER_PAGE);
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            MarketItem listing = allItems.get(i);
            long priceInHao = listing.getPrice();

            GuiElementBuilder elementBuilder = GuiElementBuilder.from(listing.getItemStack().copy())
                .addLoreLine(Component.literal(""))
                .addLoreLine(Component.literal("Người bán: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(listing.getSellerName()).withStyle(ChatFormatting.WHITE)))
                .addLoreLine(Component.literal("Giá: ").withStyle(ChatFormatting.GRAY)
                    .append(formatMoney(priceInHao)))
                .addLoreLine(Component.literal(""));

            // So khớp UUID bằng cách ép chuỗi String dứt điểm lỗi lệch Class Type Reference trong Java
            if (listing.getSellerUuid().toString().equals(this.player.getUUID().toString())) {
                elementBuilder.addLoreLine(Component.literal("Chuột Trái: Thu hồi vật phẩm này").withStyle(ChatFormatting.YELLOW));
                elementBuilder.setCallback((index, type, action, gui) -> {
                    reclaimItem(listing);
                });
            } else {
                elementBuilder.addLoreLine(Component.literal("Chuột Trái: Mua vật phẩm").withStyle(ChatFormatting.GREEN));
                elementBuilder.setCallback((index, type, action, gui) -> {
                    buyItem(listing);
                });
            }

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

    private void buyItem(MarketItem listing) {
        KyoEconomyState economyState = KyoEconomyState.getServerState(this.player.level().getServer());
        long priceInHao = listing.getPrice();

        if (economyState.getBalance(this.player.getUUID()) >= priceInHao) {
            // 1. Trừ tiền người mua (Trừ full 100% giá gốc)
            economyState.removeBalance(this.player.getUUID(), priceInHao);

            // 2. TÍNH THUẾ 5% (Cơ chế chống lạm phát)
            long taxInHao = priceInHao * 5 / 100;
            long realReceiveInHao = priceInHao - taxInHao;

            // 3. Cộng tiền cho người bán (Chỉ cộng số tiền thực nhận sau khi trừ thuế)
            economyState.addBalance(listing.getSellerUuid(), realReceiveInHao);

            // 4. Lấy đồ về cho người mua và xóa khỏi Chợ
            KyoMarketState marketState = KyoMarketState.getServerState(this.player.level().getServer());
            marketState.removeListing(listing.getId());
            if (!this.player.getInventory().add(listing.getItemStack().copy())) {
                this.player.drop(listing.getItemStack().copy(), false);
            }

            // 5. Thông báo cho người mua
            this.player.sendSystemMessage(Component.literal("Bạn đã mua thành công với giá ").withStyle(ChatFormatting.GREEN)
                .append(formatMoney(priceInHao)).append(Component.literal("!").withStyle(ChatFormatting.GREEN)));
            this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);

            // 6. Thông báo cho người bán (nếu họ đang online)
            ServerPlayer seller = this.player.level().getServer().getPlayerList().getPlayer(listing.getSellerUuid());
            if (seller != null) {
                seller.sendSystemMessage(Component.literal("[Chợ Đen] ").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                    .append(Component.literal("Hàng của bạn đã được bán! Giá: ").withStyle(ChatFormatting.GREEN))
                    .append(formatMoney(priceInHao))
                    .append(Component.literal(" (Bị trừ 5% thuế, thực nhận: ").withStyle(ChatFormatting.RED))
                    .append(formatMoney(realReceiveInHao))
                    .append(Component.literal(")").withStyle(ChatFormatting.RED)));
                seller.level().playSound(null, seller.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
            }

            renderPage();
        } else {
            this.player.sendSystemMessage(Component.literal("Bạn không có đủ ").withStyle(ChatFormatting.RED)
                .append(formatMoney(priceInHao)).append(Component.literal(" để mua vật phẩm này!").withStyle(ChatFormatting.RED)));
            this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    private void reclaimItem(MarketItem listing) {
        KyoMarketState marketState = KyoMarketState.getServerState(this.player.level().getServer());

        marketState.removeListing(listing.getId());

        if (!this.player.getInventory().add(listing.getItemStack().copy())) {
            this.player.drop(listing.getItemStack().copy(), false);
        }
        this.player.sendSystemMessage(Component.literal("Đã thu hồi vật phẩm treo bán thành công!").withStyle(ChatFormatting.YELLOW));
        renderPage();
    }
}