package kyoeconomy.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class KyoEconomyConfig {
    public static final Logger LOGGER = LoggerFactory.getLogger("kyoeconomy");

    // Tên lệnh mặc định
    public static String CMD_BAL = "bal";
    public static String CMD_PAY = "pay";
    public static String CMD_ECO = "eco";
    public static String CMD_SHOP = "shop";
    public static String CMD_AH = "ah";

    public static void loadConfig() {
        // 1. Định nghĩa thư mục con và file bên trong nó
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("kyoeconomy");
        Path configFile = configDir.resolve("commands.properties"); // Đổi tên file cho gọn

        Properties props = new Properties();
        boolean needsUpdate = false;

        try {
            // 2. Tạo thư mục 'config/kyoeconomy' nếu nó chưa tồn tại
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            // 3. Nếu file đã tồn tại, đọc nội dung cũ lên
            if (Files.exists(configFile)) {
                try (InputStream in = Files.newInputStream(configFile)) {
                    props.load(in);
                }
            } else {
                needsUpdate = true; // Nếu chưa có file thì chắc chắn phải tạo mới (update)
            }

            // 2. Kiểm tra sự tồn tại của từng key. Nếu thiếu, bổ sung giá trị mặc định và báo cần update
            if (!props.containsKey("command_balance")) { props.setProperty("command_balance", "bal"); needsUpdate = true; }
            if (!props.containsKey("command_pay")) { props.setProperty("command_pay", "pay"); needsUpdate = true; }
            if (!props.containsKey("command_admin")) { props.setProperty("command_admin", "eco"); needsUpdate = true; }
            if (!props.containsKey("command_shop")) { props.setProperty("command_shop", "shop"); needsUpdate = true; }
            if (!props.containsKey("command_ah")) { props.setProperty("command_ah", "ah"); needsUpdate = true; }

            // 3. Gán giá trị vào biến (lúc này chắc chắn 100% các key đều đã có dữ liệu)
            CMD_BAL = props.getProperty("command_balance");
            CMD_PAY = props.getProperty("command_pay");
            CMD_ECO = props.getProperty("command_admin");
            CMD_SHOP = props.getProperty("command_shop");
            CMD_AH = props.getProperty("command_ah");

            // 4. Nếu là file mới tinh HOẶC file cũ bị thiếu dòng (bản update mới) -> Mở ổ đĩa ghi lại!
            if (needsUpdate) {
                try (OutputStream out = Files.newOutputStream(configFile)) {
                    props.store(out, "Kyo Economy Commands Configuration (Thay doi ten lenh tai day)");
                }
                LOGGER.info("[KyoEconomy] Đã cập nhật/tạo mới file cấu hình lệnh (kyoeconomy.properties)!");
            }

        } catch (Exception e) {
            LOGGER.error("[KyoEconomy] Khong the doc/ghi file config cua KyoEconomy!", e);
        }
    }
}