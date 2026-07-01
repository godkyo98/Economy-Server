package kyoeconomy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class MarketItem {
    // SỬA CÁC CHỮ BÊN TRONG fieldOf() THÀNH CHỮ HOA ĐỂ KHỚP VỚI FILE SAVE CŨ
    public static final Codec<MarketItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("Id").forGetter(MarketItem::getId),
        UUIDUtil.CODEC.fieldOf("SellerId").forGetter(MarketItem::getSellerUuid),
        Codec.STRING.fieldOf("SellerName").forGetter(MarketItem::getSellerName),
        ItemStack.CODEC.fieldOf("Item").forGetter(MarketItem::getItemStack),
        Codec.LONG.fieldOf("Price").forGetter(MarketItem::getPrice)
    ).apply(instance, MarketItem::new));

    private final UUID id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack itemStack;
    private final long price;

    public MarketItem(UUID id, UUID sellerUuid, String sellerName, ItemStack itemStack, long price) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.itemStack = itemStack;
        this.price = price;
    }

    public UUID getId() { return id; }
    public UUID getSellerUuid() { return sellerUuid; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItemStack() { return itemStack; }
    public long getPrice() { return price; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MarketItem that = (MarketItem) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}