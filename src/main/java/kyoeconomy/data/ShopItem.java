package kyoeconomy.data;

import net.minecraft.world.item.Item;
import java.util.Random;

public class ShopItem {
    public final String id;       // Ví dụ: "minecraft:diamond"
    public final Item item;       // Item thực tế trong game
    public final String name;     // Tên hiển thị ("Kim Cương Mát Lạnh")
    public final long price;      // Giá bán cố định
    public final long minPrice;   // Giá thấp nhất (nếu muốn random)
    public final long maxPrice;   // Giá cao nhất (nếu muốn random)
    public final int slot;        // Vị trí trong Rương (0 - 26)

    // 🔒 BIẾN NỘI BỘ ĐỂ KHÓA GIÁ CHỐT CỐ ĐỊNH
    private long lockedPrice;
    private static final Random RANDOM = new Random();

    public ShopItem(String id, Item item, String name, long price, long minPrice, long maxPrice, int slot) {
        this.id = id;
        this.item = item;
        this.name = name;
        this.price = price;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.slot = slot;

        rollNewPrice(); // Chốt giá lần đầu tiên khi Server vừa nạp Cửa hàng
    }

    // Hàm tung xúc xắc chốt giá mới (Sẽ được gọi từ đồng hồ chạy ngầm tick() của Server)
    public void rollNewPrice() {
        if (this.price > 0) {
            this.lockedPrice = this.price; // Nếu thiết lập giá cố định thì không random
        } else if (this.minPrice > 0 && this.maxPrice > this.minPrice) {
            // Tính toán giá ngẫu nhiên nằm trong khoảng [minPrice, maxPrice]
            this.lockedPrice = this.minPrice + (long)(RANDOM.nextDouble() * (this.maxPrice - this.minPrice + 1));
        } else {
            this.lockedPrice = this.minPrice; // Fallback an toàn
        }
    }

    // 🎯 GUI hiển thị thông tin và hàm xử lý Mua sẽ gọi hàm này
    // Đảm bảo trả về đúng cái giá ĐÃ CHỐT cố định, không bị nhảy bậy bạ nữa!
    public long getCurrentPrice() {
        return this.lockedPrice;
    }
}