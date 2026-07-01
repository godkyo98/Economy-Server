package kyoeconomy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier; // Sử dụng Identifier theo đúng chuẩn 26.2
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KyoMarketState extends SavedData {

    private final List<MarketItem> activeListings = new ArrayList<>();

    // 1. Khởi tạo Codec cho toàn bộ State Chợ Đen
    public static final Codec<KyoMarketState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    MarketItem.CODEC.listOf().fieldOf("Listings").forGetter(KyoMarketState::getListings)
            ).apply(instance, KyoMarketState::new)
    );

    // 2. Khai báo TYPE chuẩn 26.2: Truyền vào Identifier và hằng số DataFixTypes là null
    public static final SavedDataType<KyoMarketState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("kyoeconomy", "market"), // Tham số 1: Identifier định danh dữ liệu chợ
            KyoMarketState::new,                                    // Tham số 2: Supplier khởi tạo mặc định
            CODEC,                                                  // Tham số 3: Bộ giải mã Codec
            null                                                    // Tham số 4: DataFixTypes truyền null (vì là dữ liệu custom)
    );

    public KyoMarketState() {}

    public KyoMarketState(List<MarketItem> listings) {
        this.activeListings.addAll(listings);
    }

    public List<MarketItem> getListings() {
        return activeListings;
    }

    public void addListing(MarketItem item) {
        activeListings.add(item);
        this.setDirty(); // Đánh dấu dữ liệu thay đổi để tự động lưu file .dat
    }

    public void removeListing(UUID listingId) {
        activeListings.removeIf(item -> item.getId().equals(listingId));
        this.setDirty();
    }

    // 3. Hàm getServerState chuẩn 26.2 (Chỉ nhận duy nhất 1 đối số là TYPE)
    public static KyoMarketState getServerState(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(TYPE);
    }
}