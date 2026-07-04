package kyoeconomy.data;

import net.minecraft.world.item.Item;
import java.util.Random;

public class ShopItem {
    public final String id;
    public final Item item;
    public final String name;
    public final long price;
    public final long minPrice;
    public final long maxPrice;
    public final int slot;
    public final String category; // 🧱 Biến mới để phân loại Tab

    private long lockedPrice;
    private static final Random RANDOM = new Random();

    public ShopItem(String id, Item item, String name, long price, long minPrice, long maxPrice, int slot, String category) {
        this.id = id;
        this.item = item;
        this.name = name;
        this.price = price;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.slot = slot;
        this.category = category; // Gán thể loại

        rollNewPrice();
    }

    public void rollNewPrice() {
        if (this.price > 0) {
            this.lockedPrice = this.price;
        } else if (this.minPrice > 0 && this.maxPrice > this.minPrice) {
            this.lockedPrice = this.minPrice + (long)(RANDOM.nextDouble() * (this.maxPrice - this.minPrice + 1));
        } else {
            this.lockedPrice = this.minPrice;
        }
    }

    public long getCurrentPrice() {
        return this.lockedPrice;
    }
}