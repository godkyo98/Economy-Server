package kyoeconomy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KyoEconomyConfig {
    public static final Logger LOGGER = LoggerFactory.getLogger("kyoeconomy");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 🚀 CHUYỂN SANG DÙNG LIST ĐỂ CHỨA ĐA NGÔN NGỮ (ALIAS)
    public static List<String> CMD_BAL = new ArrayList<>(Arrays.asList("bal", "balance", "sodu", "tien"));
    public static List<String> CMD_PAY = new ArrayList<>(Arrays.asList("pay", "chuyenkhoan", "chuyentien"));
    public static List<String> CMD_ECO = new ArrayList<>(Arrays.asList("eco", "economy", "kinhte"));
    public static List<String> CMD_SHOP = new ArrayList<>(Arrays.asList("shop", "cuahang", "taphoa"));
    public static List<String> CMD_AH = new ArrayList<>(Arrays.asList("ah", "choden", "daugia"));

    public static void loadConfig() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("kyoeconomy");
        Path configFile = configDir.resolve("commands.json"); // Đổi sang đuôi JSON

        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            if (Files.exists(configFile)) {
                // Nếu file đã tồn tại, đọc mảng JSON lên
                try (FileReader reader = new FileReader(configFile.toFile())) {
                    JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

                    CMD_BAL = getListFromJson(jsonObject, "command_balance", CMD_BAL);
                    CMD_PAY = getListFromJson(jsonObject, "command_pay", CMD_PAY);
                    CMD_ECO = getListFromJson(jsonObject, "command_admin", CMD_ECO);
                    CMD_SHOP = getListFromJson(jsonObject, "command_shop", CMD_SHOP);
                    CMD_AH = getListFromJson(jsonObject, "command_ah", CMD_AH);
                }
            } else {
                // Nếu file chưa tồn tại, tạo mới
                saveDefaultConfig(configFile);
            }
        } catch (Exception e) {
            LOGGER.error("[KyoEconomy] Lỗi khi nạp file cấu hình commands.json!", e);
        }
    }

    private static List<String> getListFromJson(JsonObject jsonObject, String key, List<String> defaultValue) {
        if (jsonObject.has(key) && jsonObject.get(key).isJsonArray()) {
            JsonArray array = jsonObject.getAsJsonArray(key);
            List<String> list = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                list.add(array.get(i).getAsString().toLowerCase()); // Ép về chữ thường cho an toàn
            }
            return list.isEmpty() ? defaultValue : list;
        }
        return defaultValue;
    }

    private static void saveDefaultConfig(Path configFile) {
        JsonObject rootObject = new JsonObject();

        rootObject.add("command_balance", createJsonArray(CMD_BAL));
        rootObject.add("command_pay", createJsonArray(CMD_PAY));
        rootObject.add("command_admin", createJsonArray(CMD_ECO));
        rootObject.add("command_shop", createJsonArray(CMD_SHOP));
        rootObject.add("command_ah", createJsonArray(CMD_AH));

        try (FileWriter writer = new FileWriter(configFile.toFile())) {
            GSON.toJson(rootObject, writer);
            LOGGER.info("[KyoEconomy] Đã tạo mới file cấu hình đa lệnh (commands.json)!");
        } catch (IOException e) {
            LOGGER.error("[KyoEconomy] Không thể lưu file cấu hình mặc định!", e);
        }
    }

    private static JsonArray createJsonArray(List<String> list) {
        JsonArray array = new JsonArray();
        for (String s : list) {
            array.add(s);
        }
        return array;
    }
}