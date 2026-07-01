package kyoeconomy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;

import java.util.HashMap;
import java.util.UUID;

public class KyoEconomyState extends SavedData {

    public final HashMap<UUID, Long> balances = new HashMap<>();

    // 1. Tạo Codec cho UUID để chuyển đổi tự động giữa String và UUID
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    // 2. Tạo Codec cho KyoEconomyState (Thay thế hoàn toàn cho hàm load/save NBT thủ công)
    public static final Codec<KyoEconomyState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(UUID_CODEC, Codec.LONG).fieldOf("balances").forGetter(state -> state.balances)
    ).apply(instance, map -> {
        KyoEconomyState state = new KyoEconomyState();
        state.balances.putAll(map);
        return state;
    }));

    // 3. Khai báo TYPE truyền CODEC vào tham số thứ 3
    private static final SavedDataType<KyoEconomyState> TYPE = new SavedDataType<>(
            Identifier.parse("kyoeconomy:data"), // Tên file được lưu (sẽ thành kyoeconomy/data.dat)
            KyoEconomyState::new,                      // Hàm tạo mới khi file chưa tồn tại
            CODEC,                                     // Codec tự động xử lý đọc/ghi
            null                                       // DataFixTypes
    );

    // 4. Hàm lấy dữ liệu từ Server (Đã được tối ưu cho 26.2)
    public static KyoEconomyState getServerState(MinecraftServer server) {
        SavedDataStorage dataStorage = server.overworld().getDataStorage();
        return dataStorage.computeIfAbsent(TYPE);
    }

    // --- Các hàm tiện ích thao tác Tiền tệ ---

    public long getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0L);
    }

    public void addBalance(UUID uuid, long amount) {
        balances.put(uuid, getBalance(uuid) + amount);
        this.setDirty(); // setDirty() sẽ báo cho Codec biết để tự động lưu file
    }

    public boolean removeBalance(UUID uuid, long amount) {
        long current = getBalance(uuid);
        if (current >= amount) {
            balances.put(uuid, current - amount);
            this.setDirty();
            return true;
        }
        return false;
    }

    public void setBalance(UUID uuid, long amount) {
        balances.put(uuid, amount);
        this.setDirty();
    }
}