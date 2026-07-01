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
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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

		// Đăng ký Event chạy ngầm mỗi khi Server hoàn thành 1 Tick
		ServerTickEvents.END_SERVER_TICK.register(KyoLeaderboardManager::tick);

		// Bên trong hàm onInitialize():
		ServerTickEvents.END_SERVER_TICK.register(ShopConfigManager::tick);

		// 2. Đăng ký biến Placeholder API chuẩn v3 (TRẢ VỀ COMPONENT ĐA MÀU SẮC)
		Placeholders.registerServer(Identifier.fromNamespaceAndPath(MOD_ID, "balance"), (context, argument) -> {
			if (!context.hasPlayer()) {
				return PlaceholderResult.invalid("No player found");
			}

			ServerPlayer player = (ServerPlayer) context.player();
			if (player == null) {
				return PlaceholderResult.invalid("Player object is null");
			}

			MinecraftServer server = context.server();
			if (server == null) {
				server = player.level().getServer();
			}

			if (server == null) {
				return PlaceholderResult.invalid("Minecraft server is null");
			}

			KyoEconomyState state = KyoEconomyState.getServerState(server);
			long totalHao = state.getBalance(player.getUUID());

			// TÍNH TOÁN QUY ĐỔI XU VÀ HÀO
			long xu = totalHao / 1000;
			long hao = totalHao % 1000;

			// XUẤT RA COMPONENT ĐA MÀU SẮC
			MutableComponent moneyComponent;

			if (xu > 0 && hao > 0) {
				moneyComponent = Component.literal(String.format("%,d Xu ", xu)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
						.append(Component.literal(String.format("%,d Hào", hao)).withStyle(ChatFormatting.YELLOW));
			} else if (xu > 0) {
				moneyComponent = Component.literal(String.format("%,d Xu", xu)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
			} else {
				moneyComponent = Component.literal(String.format("%,d Hào", hao)).withStyle(ChatFormatting.YELLOW);
			}

			// Trả về thẳng Component (Hệ thống PB4 sẽ lo liệu phần màu sắc)
			return PlaceholderResult.value(moneyComponent);
		});

		// 3. Đợi Server chạy xong mới nạp Admin Shop để tránh lỗi Registry
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LOGGER.info("[KyoEconomy] Máy chủ đã sẵn sàng. Tiến hành nạp dữ liệu Admin Shop...");
			ShopConfigManager.loadShop();
			LOGGER.info("[KyoEconomy] Nạp dữ liệu Admin Shop hoàn tất!");
		});
	}
}