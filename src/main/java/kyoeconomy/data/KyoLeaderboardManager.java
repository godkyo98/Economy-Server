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
  private static final long COOLDOWN_MS = 5 * 60 * 1000; // Bộ đếm: 5 phút cập nhật 1 lần

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

    // 1. Sắp xếp danh sách tài khoản theo thứ tự giảm dần và lấy Top 3
    List<Map.Entry<UUID, Long>> topPlayers = balances.entrySet().stream()
        .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
        .limit(3)
        .toList();

    Scoreboard scoreboard = server.getScoreboard();

    // 2. Cấu hình 3 Đội (Team) với Tiền tố tương ứng
    setupTeam(scoreboard, "kyo_rank_1", "[Phú Hộ] ", ChatFormatting.GOLD);
    setupTeam(scoreboard, "kyo_rank_2", "[Cự Phú] ", ChatFormatting.GRAY); // Bạc
    setupTeam(scoreboard, "kyo_rank_3", "[Thương Gia] ", ChatFormatting.RED); // Đồng

    // 3. Phân bổ người chơi vào Đội
    for (int i = 0; i < 3; i++) {
      PlayerTeam team = scoreboard.getPlayerTeam("kyo_rank_" + (i + 1));
      if (team != null) {
        // Đá tất cả thành viên cũ ra khỏi Đội này để nhường chỗ cho Top mới
        Collection<String> oldMembers = new ArrayList<>(team.getPlayers());
        for (String member : oldMembers) {
          scoreboard.removePlayerFromTeam(member, team);
        }

        if (i < topPlayers.size()) {
          UUID topUuid = topPlayers.get(i).getKey();

          // FIX: Lấy tên người chơi thông qua danh sách Online thay vì Profile Cache cũ
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

    // Chỉ dùng setPlayerPrefix để gán tag màu (VD: [Phú Hộ] lấp lánh Vàng)
    // Bỏ qua hàm team.setColor() để tránh lỗi Optional<TeamColor> gắt gao của bản 26.2
    team.setPlayerPrefix(Component.literal(prefix).withStyle(color, ChatFormatting.BOLD));
  }
}