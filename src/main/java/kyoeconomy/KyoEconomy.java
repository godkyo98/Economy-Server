package kyoeconomy;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import kyoeconomy.command.KyoEconomyCommands;
import kyoeconomy.config.KyoEconomyConfig;
import kyoeconomy.config.ShopConfigManager;
import kyoeconomy.data.KyoEconomyState;
import kyoeconomy.data.KyoLeaderboardManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KyoEconomy implements ModInitializer {
	public static final String MOD_ID = "kyoeconomy";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[$] Kyo Economy đang khởi động...");

		// 1. Nạp cấu hình lệnh trước để áp dụng tên lệnh custom
		KyoEconomyConfig.loadConfig();

		// Đăng ký hệ thống lệnh
		KyoEconomyCommands.register();

		// 2. Đăng ký Placeholder "balance" rút gọn thông minh cho Scoreboard / Tab List
		Placeholders.registerServer(Identifier.fromNamespaceAndPath(MOD_ID, "balance"), (context, argument) -> {
			if (context.player() == null) {
				return PlaceholderResult.invalid("Player object is null");
			}

			ServerPlayer player = context.serverPlayer();
			MinecraftServer server = context.server() != null ? context.server() : player.level().getServer();

			if (server == null) {
				return PlaceholderResult.invalid("Minecraft server is null");
			}

			KyoEconomyState state = KyoEconomyState.getServerState(server);
			long totalHao = state.getBalance(player.getUUID());

			// 🎯 GỌI HÀM RÚT GỌN KHI ĐẨY RA SCOREBOARD / TAB LIST
			return PlaceholderResult.value(formatCompactMoney(totalHao));
		});

		// 3. Đợi Server khởi động xong để nạp dữ liệu Admin Shop cố định mốc giá ban đầu
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ShopConfigManager.loadShop();
			LOGGER.info("[KyoEconomy] Cửa hàng máy chủ đã được đồng bộ dữ liệu thành công!");
		});

		// 4. Đăng ký bộ đếm ngầm: Cắm điện cho đồng hồ Sàn Chứng Khoán và Leaderboard Phú Hộ chạy mỗi tick
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ShopConfigManager.tick(server);
			KyoLeaderboardManager.tick(server);
		});
	}

	// 🪙 HÀM RÚT GỌN TIỀN TỆ ĐỂ HIỂN THỊ TRÊN SCOREBOARD / TAB LIST (TỐI ƯU GIAO DIỆN CHỐNG VỠ KHUNG)
	public static MutableComponent formatCompactMoney(long totalHao) {
		double xu = totalHao / 1000.0;
		String formattedStr;

		// Mốc Tỷ Xu (Billion)
		if (xu >= 1_000_000_000.0) {
			formattedStr = String.format(xu % 1_000_000_000.0 == 0 ? "%.0fB" : "%.1fB", xu / 1_000_000_000.0);
			return Component.literal(formattedStr + " Xu").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
		}
		// Mốc Triệu Xu (Million)
		else if (xu >= 1_000_000.0) {
			formattedStr = String.format(xu % 1_000_000.0 == 0 ? "%.0fM" : "%.1fM", xu / 1_000_000.0);
			return Component.literal(formattedStr + " Xu").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
		}
		// Mốc Ngàn Xu (Kilo)
		else if (xu >= 1_000.0) {
			formattedStr = String.format(xu % 1_000.0 == 0 ? "%.0fk" : "%.1fk", xu / 1_000.0);
			return Component.literal(formattedStr + " Xu").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
		}

		// Nếu dưới 1,000 Xu thì giữ nguyên định dạng chi tiết để người chơi dễ nhìn số dư lẻ
		long xuLong = totalHao / 1000;
		long haoLong = totalHao % 1000;

		if (xuLong > 0 && haoLong > 0) {
			return Component.literal(String.format("%,d Xu ", xuLong)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
					.append(Component.literal(String.format("%,d Hào", haoLong)).withStyle(ChatFormatting.YELLOW));
		} else if (xuLong > 0) {
			return Component.literal(String.format("%,d Xu", xuLong)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
		} else {
			return Component.literal(String.format("%,d Hào", haoLong)).withStyle(ChatFormatting.YELLOW);
		}
	}
}