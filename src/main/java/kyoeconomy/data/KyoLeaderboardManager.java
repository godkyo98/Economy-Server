package kyoeconomy.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.*;

public class KyoLeaderboardManager {
  private static long lastUpdateTime = 0;
  private static final long COOLDOWN_MS = 5 * 60 * 1000; // Bộ đếm: 5 phút quét xếp hạng lại 1 lần

  public static void tick(MinecraftServer server) {
    long now = System.currentTimeMillis();
    if (now - lastUpdateTime >= COOLDOWN_MS) {
      updateLeaderboardTeams(server);
      lastUpdateTime = now;
    }
  }

  private static void updateLeaderboardTeams(MinecraftServer server) {
    KyoEconomyState ecoState = KyoEconomyState.getServerState(server);
    Map<UUID, Long> balances = ecoState.balances;

    // Nếu server chưa có ai có tiền thì bỏ qua
    if (balances.isEmpty()) return;

    // 1. Sắp xếp danh sách tài khoản theo thứ tự giảm dần và lấy hẳn TOP 5
    List<Map.Entry<UUID, Long>> topPlayers = balances.entrySet().stream()
        .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
        .limit(5)
        .toList();

    Scoreboard scoreboard = server.getScoreboard();

    // 2. Thiết kế lại Tiền tố danh hiệu với Ký tự đặc biệt siêu nổi bật cho 5 bậc Giai Cấp
    setupTeam(scoreboard, "kyo_rank_1", "✦ 👑 [ĐẠI PHÚ HỘ] ✦ ", ChatFormatting.GOLD);
    setupTeam(scoreboard, "kyo_rank_2", "✧ 🌟 [CỰ PHÚ] ✧ ", ChatFormatting.AQUA);
    setupTeam(scoreboard, "kyo_rank_3", "🔸 💎 [ĐẠI THƯƠNG] 🔸 ", ChatFormatting.LIGHT_PURPLE);
    setupTeam(scoreboard, "kyo_rank_4", "🔹 ⚜ [TÀI PHIỆT] 🔹 ", ChatFormatting.GREEN);
    setupTeam(scoreboard, "kyo_rank_5", "▪ 🪙 [PHÚ HÀO] ▪ ", ChatFormatting.GRAY);

    // 3. Quét dọn dẹp và phân bổ người chơi lọt Top vào Đội tương ứng
    for (int i = 0; i < 5; i++) {
      PlayerTeam team = scoreboard.getPlayerTeam("kyo_rank_" + (i + 1));
      if (team != null) {
        // Đá tất cả thành viên cũ ra khỏi danh hiệu này để nhường chỗ cho thế hệ đại gia mới
        Collection<String> oldMembers = new ArrayList<>(team.getPlayers());
        for (String member : oldMembers) {
          scoreboard.removePlayerFromTeam(member, team);
        }

        // Nếu vị trí Top này có người nắm giữ
        if (i < topPlayers.size()) {
          UUID topUuid = topPlayers.get(i).getKey();

          // Chỉ những đại gia đang Online mới được khoác áo vinh quang (Tiết kiệm CPU/RAM)
          ServerPlayer p = server.getPlayerList().getPlayer(topUuid);
          if (p != null) {
            scoreboard.addPlayerToTeam(p.getScoreboardName(), team);
          }
        }
      }
    }
  }

  private static void setupTeam(Scoreboard scoreboard, String teamName, String prefix, ChatFormatting color) {
    PlayerTeam team = scoreboard.getPlayerTeam(teamName);
    if (team == null) {
      team = scoreboard.addPlayerTeam(teamName);
    }
    // Ép chữ đậm (BOLD) và phủ màu rực rỡ lên toàn bộ tag ký tự đặc biệt
    team.setPlayerPrefix(Component.literal(prefix).withStyle(color, ChatFormatting.BOLD));
  }
}