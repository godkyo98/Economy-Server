package kyoeconomy.api; // Đảm bảo file này nằm trong thư mục: src/main/java/kyoeconomy/api/

import kyoeconomy.data.KyoEconomyState;
import net.minecraft.server.MinecraftServer;
import java.util.UUID;

/**
 * API Của KyoEconomy dành cho các Mod khác gọi dữ liệu thế giới
 * Hệ thống quy đổi chuẩn: 1 Xu = 1000 Hào
 */
public class KyoEconomyAPI {

    // ==========================================
    // NHÓM GIAO DỊCH THEO ĐƠN VỊ XU (THẬP PHÂN)
    // ==========================================

    /**
     * Lấy số dư hiện tại của người chơi (Trả về đơn vị Xu dạng Double)
     * Ví dụ: Tài khoản có 1500 Hào -> Trả về 1.5 Xu
     */
    public static double getBalanceInXu(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) return 0.0;

        KyoEconomyState state = KyoEconomyState.getServerState(server);
        long currentHao = state.getBalance(uuid);

        // Chia cho 1000.0 để giữ nguyên phần thập phân của Xu
        return currentHao / 1000.0;
    }

    /**
     * Cộng tiền theo đơn vị Xu cho người chơi (Hỗ trợ số lẻ)
     * Ví dụ: addMoneyInXu(server, uuid, 2.5) -> Tự động quy đổi thành +2500 Hào vào DB
     */
    public static void addMoneyInXu(MinecraftServer server, UUID uuid, double amountInXu) {
        if (server == null || uuid == null || amountInXu <= 0) return;

        long haoToAdd = (long) (amountInXu * 1000);
        KyoEconomyState state = KyoEconomyState.getServerState(server);
        state.addBalance(uuid, haoToAdd);
    }

    /**
     * Trừ tiền theo đơn vị Xu của người chơi (Hỗ trợ số lẻ)
     * Ví dụ: removeMoneyInXu(server, uuid, 1.25) -> Tự động quy đổi thành -1250 Hào trong DB
     */
    public static boolean removeMoneyInXu(MinecraftServer server, UUID uuid, double amountInXu) {
        if (server == null || uuid == null || amountInXu <= 0) return false;

        long haoToRemove = (long) (amountInXu * 1000);
        KyoEconomyState state = KyoEconomyState.getServerState(server);

        if (state.getBalance(uuid) >= haoToRemove) {
            state.removeBalance(uuid, haoToRemove);
            return true;
        }
        return false; // Trả về false nếu người chơi không đủ tiền để trừ
    }


    // ==========================================
    // NHÓM GIAO DỊCH THEO ĐƠN VỊ HÀO (SỐ NGUYÊN)
    // ==========================================

    /**
     * Lấy số dư hiện tại của người chơi theo đơn vị Hào thô gốc trong Database
     */
    public static long getBalanceInHao(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) return 0L;

        KyoEconomyState state = KyoEconomyState.getServerState(server);
        return state.getBalance(uuid);
    }

    /**
     * Cộng tiền trực tiếp bằng đơn vị Hào thô vào Database
     */
    public static void addMoneyInHao(MinecraftServer server, UUID uuid, long amountInHao) {
        if (server == null || uuid == null || amountInHao <= 0) return;

        KyoEconomyState state = KyoEconomyState.getServerState(server);
        state.addBalance(uuid, amountInHao);
    }

    /**
     * Trừ tiền trực tiếp bằng đơn vị Hào thô trong Database
     */
    public static boolean removeMoneyInHao(MinecraftServer server, UUID uuid, long amountInHao) {
        if (server == null || uuid == null || amountInHao <= 0) return false;

        KyoEconomyState state = KyoEconomyState.getServerState(server);
        if (state.getBalance(uuid) >= amountInHao) {
            state.removeBalance(uuid, amountInHao);
            return true;
        }
        return false;
    }
}