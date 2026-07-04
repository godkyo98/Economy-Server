package kyoeconomy.config;

import com.google.gson.*;
import kyoeconomy.KyoEconomy;
import kyoeconomy.data.ShopItem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ShopConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("kyoeconomy");
    private static final File FILE = CONFIG_DIR.resolve("shop.json").toFile();

    public static final List<ShopItem> SHOP_ITEMS = new ArrayList<>();
    private static long lastUpdateTime = 0;
    private static final long PRICE_CHANGE_INTERVAL_MS = 60 * 60 * 1000;

    public static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime >= PRICE_CHANGE_INTERVAL_MS && !SHOP_ITEMS.isEmpty()) {
            for (ShopItem item : SHOP_ITEMS) {
                item.rollNewPrice();
            }
            server.getPlayerList().broadcastSystemMessage(
                Component.literal("[Cửa Hàng] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                    .append(Component.literal("Thị trường vừa biến động! Giá cả vật phẩm đã được cập nhật mới.").withStyle(ChatFormatting.YELLOW)),
                false
            );
            lastUpdateTime = now;
        }
    }

    public static void loadShop() {
        SHOP_ITEMS.clear();
        if (!FILE.exists()) {
            createDefaultConfig();
        }

        try (FileReader reader = new FileReader(FILE)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            if (jsonObject.has("items")) {
                JsonArray itemsArray = jsonObject.getAsJsonArray("items");
                for (int i = 0; i < itemsArray.size(); i++) {
                    JsonObject itemObj = itemsArray.get(i).getAsJsonObject();

                    String itemIdStr = itemObj.get("item_id").getAsString();
                    Identifier resourceLocation = Identifier.tryParse(itemIdStr);

                    Item item = Items.AIR;
                    if (resourceLocation != null) {
                        var opt = BuiltInRegistries.ITEM.get(resourceLocation);
                        if (opt.isPresent()) {
                            item = opt.get().value();
                        }
                    }

                    if (item != Items.AIR) {
                        String name = itemObj.has("name") ? itemObj.get("name").getAsString() : "Vật phẩm";
                        long minPrice = itemObj.has("min_price") ? (long)(itemObj.get("min_price").getAsDouble() * 1000) : 0;
                        long maxPrice = itemObj.has("max_price") ? (long)(itemObj.get("max_price").getAsDouble() * 1000) : 0;
                        long fixedPrice = itemObj.has("price") ? (long)(itemObj.get("price").getAsDouble() * 1000) : 0;
                        int slot = itemObj.has("slot") ? itemObj.get("slot").getAsInt() : i;

                        // Đọc danh mục (mặc định là "khac" nếu trống)
                        String category = itemObj.has("category") ? itemObj.get("category").getAsString().toLowerCase() : "khac";

                        SHOP_ITEMS.add(new ShopItem(resourceLocation.toString(), item, name, fixedPrice, minPrice, maxPrice, slot, category));
                    }
                }
            }
            KyoEconomy.LOGGER.info("[KyoEconomy] Đã tải thành công {} vật phẩm vào Cửa hàng!", SHOP_ITEMS.size());
            if (lastUpdateTime == 0) lastUpdateTime = System.currentTimeMillis();
        } catch (Exception e) {
            KyoEconomy.LOGGER.error("[KyoEconomy] Lỗi khi đọc file shop.json!", e);
        }
    }

    public static boolean addAndSaveItem(Item item, String name, double minPriceXu, double maxPriceXu, double fixedPriceXu, double customSellPriceXu) {
        try {
            JsonObject rootObject;
            if (FILE.exists()) {
                try (FileReader reader = new FileReader(FILE)) {
                    rootObject = JsonParser.parseReader(reader).getAsJsonObject();
                }
            } else {
                rootObject = new JsonObject();
            }

            if (!rootObject.has("items")) {
                rootObject.add("items", new JsonArray());
            }

            JsonArray itemsArray = rootObject.getAsJsonArray("items");
            String itemId = BuiltInRegistries.ITEM.getKey(item).toString();

            JsonObject newItemObj = new JsonObject();
            newItemObj.addProperty("item_id", itemId);
            newItemObj.addProperty("name", name);
            newItemObj.addProperty("category", "khac"); // Lệnh add nhanh sẽ mặc định ném vào mục khác

            if (fixedPriceXu > 0) {
                newItemObj.addProperty("price", fixedPriceXu);
            } else {
                newItemObj.addProperty("min_price", minPriceXu);
                newItemObj.addProperty("max_price", maxPriceXu);
            }

            itemsArray.add(newItemObj);

            try (FileWriter writer = new FileWriter(FILE)) {
                GSON.toJson(rootObject, writer);
            }

            loadShop();
            return true;
        } catch (IOException e) {
            KyoEconomy.LOGGER.error("[KyoEconomy] Lỗi khi thêm vật phẩm mới vào shop.json!", e);
            return false;
        }
    }

    private static void createDefaultConfig() {
        try {
            CONFIG_DIR.toFile().mkdirs();
            JsonObject rootObject = new JsonObject();
            rootObject.add("items", new JsonArray());
            try (FileWriter writer = new FileWriter(FILE)) {
                GSON.toJson(rootObject, writer);
            }
        } catch (IOException e) {
            KyoEconomy.LOGGER.error("[KyoEconomy] Không thể tạo file shop.json mặc định!", e);
        }
    }
}