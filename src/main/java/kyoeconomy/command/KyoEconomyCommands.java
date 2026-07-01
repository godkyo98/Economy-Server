package kyoeconomy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType; // Sử dụng đối số thập phân
import kyoeconomy.config.KyoEconomyConfig;
import kyoeconomy.config.ShopConfigManager;
import kyoeconomy.data.KyoEconomyState;
import kyoeconomy.data.KyoMarketState;
import kyoeconomy.data.MarketItem;
import kyoeconomy.gui.KyoMarketGui;
import kyoeconomy.gui.KyoShopGui;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.UUID;

public class KyoEconomyCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerBal(dispatcher);
            registerPay(dispatcher);
            registerAdmin(dispatcher);
            registerShop(dispatcher);
            registerAh(dispatcher);
        });
    }

    // Hàm tiện ích hỗ trợ định dạng tiền tệ đẹp mắt trong khung Chat cho người chơi
    private static String formatMoney(long totalHao) {
        long xu = totalHao / 1000;
        long hao = totalHao % 1000;
        if (xu > 0 && hao > 0) return String.format("%,d Xu %,d Hào", xu, hao);
        if (xu > 0) return String.format("%,d Xu", xu);
        return String.format("%,d Hào", hao);
    }

    // 1. LỆNH XEM TIỀN (/bal)
    private static void registerBal(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(KyoEconomyConfig.CMD_BAL)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    KyoEconomyState state = KyoEconomyState.getServerState(context.getSource().getServer());
                    long totalHao = state.getBalance(player.getUUID());

                    player.sendSystemMessage(Component.literal("Số dư của bạn: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(formatMoney(totalHao)).withStyle(ChatFormatting.GOLD)));
                    return 1;
                })
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer adminOrPlayer = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            KyoEconomyState state = KyoEconomyState.getServerState(context.getSource().getServer());
                            long totalHao = state.getBalance(target.getUUID());

                            adminOrPlayer.sendSystemMessage(Component.literal("Số dư của " + target.getScoreboardName() + ": ").withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal(formatMoney(totalHao)).withStyle(ChatFormatting.GOLD)));
                            return 1;
                        })
                )
        );
    }

    // 2. LỆNH CHUYỂN TIỀN (/pay <player> <số_xu_thập_phân>)
    private static void registerPay(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(KyoEconomyConfig.CMD_PAY)
                .then(Commands.argument("player", EntityArgument.player())
                        // Cho phép chuyển tối thiểu 0.001 Xu (= 1 Hào)
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.001))
                                .executes(context -> {
                                    ServerPlayer source = context.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");

                                    double amountInXu = DoubleArgumentType.getDouble(context, "amount");
                                    long amountInHao = (long) (amountInXu * 1000); // Quy đổi ra đơn vị gốc Hào

                                    if (source.getUUID().equals(target.getUUID())) {
                                        source.sendSystemMessage(Component.literal("Bạn không thể tự chuyển tiền cho chính mình!").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }

                                    KyoEconomyState state = KyoEconomyState.getServerState(context.getSource().getServer());
                                    if (state.getBalance(source.getUUID()) >= amountInHao) {
                                        state.removeBalance(source.getUUID(), amountInHao);
                                        state.addBalance(target.getUUID(), amountInHao);

                                        source.sendSystemMessage(Component.literal("Đã chuyển " + formatMoney(amountInHao) + " cho " + target.getScoreboardName()).withStyle(ChatFormatting.GREEN));
                                        target.sendSystemMessage(Component.literal("Bạn đã nhận được " + formatMoney(amountInHao) + " từ " + source.getScoreboardName()).withStyle(ChatFormatting.GREEN));
                                        return 1;
                                    } else {
                                        source.sendSystemMessage(Component.literal("Bạn không có đủ số dư để thực hiện giao dịch này!").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                }))
                )
        );
    }

    // 3. LỆNH ADMIN QUẢN LÝ TIỀN (/eco) - Hỗ trợ nhập Xu thập phân, lưu trữ bằng Hào nguyên chẵn
    private static void registerAdmin(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(KyoEconomyConfig.CMD_ECO)
                .requires(source -> {
                    if (!source.isPlayer()) return true; // Console luôn chạy được
                    ServerPlayer p = source.getPlayer();
                    return p != null && source.getServer().getPlayerList().isOp(p.nameAndId());
                })
                .then(Commands.literal("reload")
                        .executes(context -> {
                            KyoEconomyConfig.loadConfig();
                            ShopConfigManager.loadShop();
                            context.getSource().sendSystemMessage(Component.literal("✔ Đã tải lại thành công Cấu hình và Giá Shop KyoEconomy!").withStyle(ChatFormatting.GREEN));
                            return 1;
                        })
                )
                .then(Commands.literal("add")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.001))
                                        .executes(context -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "players");
                                            double amountInXu = DoubleArgumentType.getDouble(context, "amount");
                                            long amountInHao = (long) (amountInXu * 1000);

                                            KyoEconomyState state = KyoEconomyState.getServerState(context.getSource().getServer());
                                            for (ServerPlayer target : targets) {
                                                state.addBalance(target.getUUID(), amountInHao);
                                                context.getSource().sendSystemMessage(Component.literal("Đã cộng " + formatMoney(amountInHao) + " cho " + target.getScoreboardName()).withStyle(ChatFormatting.GREEN));
                                            }
                                            return targets.size();
                                        }))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.001))
                                        .executes(context -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "players");
                                            double amountInXu = DoubleArgumentType.getDouble(context, "amount");
                                            long amountInHao = (long) (amountInXu * 1000);

                                            KyoEconomyState state = KyoEconomyState.getServerState(context.getSource().getServer());
                                            for (ServerPlayer target : targets) {
                                                state.removeBalance(target.getUUID(), amountInHao);
                                                context.getSource().sendSystemMessage(Component.literal("Đã trừ " + formatMoney(amountInHao) + " của " + target.getScoreboardName()).withStyle(ChatFormatting.RED));
                                            }
                                            return targets.size();
                                        }))))
                .then(Commands.literal("set")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                        .executes(context -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "players");
                                            double amountInXu = DoubleArgumentType.getDouble(context, "amount");
                                            long amountInHao = (long) (amountInXu * 1000);

                                            KyoEconomyState state = KyoEconomyState.getServerState(context.getSource().getServer());
                                            for (ServerPlayer target : targets) {
                                                state.setBalance(target.getUUID(), amountInHao);
                                                context.getSource().sendSystemMessage(Component.literal("Đã đặt số dư của " + target.getScoreboardName() + " thành " + formatMoney(amountInHao)).withStyle(ChatFormatting.GREEN));
                                            }
                                            return targets.size();
                                        }))))
        );
    }

  // Lệnh Shop (Mở Admin Shop hoặc Admin thêm đồ)
  private static void registerShop(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(Commands.literal(KyoEconomyConfig.CMD_SHOP)

        // Chức năng 1: Người chơi mở Shop bình thường
        .executes(context -> {
          ServerPlayer player = context.getSource().getPlayerOrException();
          new KyoShopGui(player).open();
          return 1;
        })

        // Chức năng 2: Admin thêm đồ vào Shop với giá CỐ ĐỊNH (/shop additem <Giá_Xu>)
        .then(Commands.literal("additem")
            // FIX: Kiểm tra quyền OP an toàn theo kiến trúc 26.2
            .requires(source -> {
              try {
                if (source.getEntity() instanceof ServerPlayer player) {
                  return source.getServer().getPlayerList().isOp(player.nameAndId());
                }
                return true; // Cho phép Console Server chạy lệnh
              } catch (Exception e) {
                return false;
              }
            })
            .then(Commands.argument("price", DoubleArgumentType.doubleArg(0.01))
                .executes(context -> {
                  ServerPlayer admin = context.getSource().getPlayerOrException();
                  ItemStack itemInHand = admin.getMainHandItem();

                  if (itemInHand.isEmpty()) {
                    admin.sendSystemMessage(Component.literal("Bạn phải cầm một vật phẩm trên tay để thêm vào Shop!").withStyle(ChatFormatting.RED));
                    return 0;
                  }

                  double priceXu = DoubleArgumentType.getDouble(context, "price");
                  String itemName = itemInHand.getHoverName().getString(); // Tự động lấy tên Item (Kể cả tên Custom)

                  boolean success = ShopConfigManager.addAndSaveItem(itemInHand.getItem(), itemName, 0, 0, priceXu, -1);

                  if (success) {
                    admin.sendSystemMessage(Component.literal("Đã thêm [" + itemName + "] vào Shop với giá " + priceXu + " Xu!").withStyle(ChatFormatting.GREEN));
                  } else {
                    admin.sendSystemMessage(Component.literal("Lỗi khi ghi vào file shop.json!").withStyle(ChatFormatting.RED));
                  }
                  return 1;
                })
            )
        )

        // Chức năng 3: Admin thêm đồ vào Shop với giá DAO ĐỘNG (/shop adddynamic <Min> <Max>)
        .then(Commands.literal("adddynamic")
            // FIX: Kiểm tra quyền OP an toàn theo kiến trúc 26.2
            .requires(source -> {
              try {
                if (source.getEntity() instanceof ServerPlayer player) {
                  return source.getServer().getPlayerList().isOp(player.nameAndId());
                }
                return true;
              } catch (Exception e) {
                return false;
              }
            })
            .then(Commands.argument("minPrice", DoubleArgumentType.doubleArg(0.001))
                .then(Commands.argument("maxPrice", DoubleArgumentType.doubleArg(0.001))
                    .executes(context -> {
                      ServerPlayer admin = context.getSource().getPlayerOrException();
                      ItemStack itemInHand = admin.getMainHandItem();

                      if (itemInHand.isEmpty()) {
                        admin.sendSystemMessage(Component.literal("Bạn phải cầm một vật phẩm trên tay để thêm vào Shop!").withStyle(ChatFormatting.RED));
                        return 0;
                      }

                      double minXu = DoubleArgumentType.getDouble(context, "minPrice");
                      double maxXu = DoubleArgumentType.getDouble(context, "maxPrice");

                      if (minXu > maxXu) {
                        admin.sendSystemMessage(Component.literal("Giá Min không thể lớn hơn giá Max!").withStyle(ChatFormatting.RED));
                        return 0;
                      }

                      String itemName = itemInHand.getHoverName().getString();

                      boolean success = ShopConfigManager.addAndSaveItem(itemInHand.getItem(), itemName, minXu, maxXu, 0, -1);

                      if (success) {
                        admin.sendSystemMessage(Component.literal("Đã thêm [" + itemName + "] vào Shop (Giá dao động: " + minXu + " - " + maxXu + " Xu)!").withStyle(ChatFormatting.GREEN));
                      } else {
                        admin.sendSystemMessage(Component.literal("Lỗi khi ghi vào file shop.json!").withStyle(ChatFormatting.RED));
                      }
                      return 1;
                    })
                )
            )
        )
    );
  }

    // 5. LỆNH MỞ CHỢ ĐEN NGƯỜI CHƠI (/ah) - HỖ TRỢ ĐĂNG BÁN THẬP PHÂN XU THÂN THIỆN (Ví dụ: /ah sell 1.5)
    private static void registerAh(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(KyoEconomyConfig.CMD_AH)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    new KyoMarketGui(player).open();

                    player.sendSystemMessage(Component.literal("[Mẹo] ").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                            .append(Component.literal("Cầm vật phẩm trên tay và gõ ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("/" + KyoEconomyConfig.CMD_AH + " sell <giá_xu> ").withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal("để treo chợ (Hỗ trợ số lẻ ví dụ: 2.5 Xu)!").withStyle(ChatFormatting.GRAY)));
                    return 1;
                })
                .then(Commands.literal("help")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            player.sendSystemMessage(Component.literal("=== HƯỚNG DẪN CHỢ ĐEN NGƯỜI CHƠI ===").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                            player.sendSystemMessage(Component.literal("• /" + KyoEconomyConfig.CMD_AH + " : ").withStyle(ChatFormatting.AQUA)
                                    .append(Component.literal("Mở giao diện chợ để mua đồ của người khác.").withStyle(ChatFormatting.WHITE)));
                            player.sendSystemMessage(Component.literal("• /" + KyoEconomyConfig.CMD_AH + " sell <giá_xu> : ").withStyle(ChatFormatting.AQUA)
                                    .append(Component.literal("Cầm đồ trên tay, treo bán theo giá Xu (Ví dụ: 10 hoặc 1.5).").withStyle(ChatFormatting.WHITE)));
                            player.sendSystemMessage(Component.literal("• /" + KyoEconomyConfig.CMD_AH + " help : ").withStyle(ChatFormatting.AQUA)
                                    .append(Component.literal("Xem menu hướng dẫn này.").withStyle(ChatFormatting.WHITE)));
                            player.sendSystemMessage(Component.literal("====================================").withStyle(ChatFormatting.DARK_PURPLE));
                            return 1;
                        })
                )
                .then(Commands.literal("sell")
                        // Chuyển sang DoubleArgumentType để người chơi gõ số thập phân thoải mái
                        .then(Commands.argument("price", DoubleArgumentType.doubleArg(0.001))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();

                                    double priceInXu = DoubleArgumentType.getDouble(context, "price");
                                    long priceInHao = (long) (priceInXu * 1000); // Ép kiểu quy đổi ngược về Hào nguyên chẵn

                                    ItemStack itemInHand = player.getMainHandItem();

                                    if (itemInHand.isEmpty()) {
                                        player.sendSystemMessage(Component.literal("Bạn phải cầm một vật phẩm trên tay mới có thể đăng bán!").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }

                                    // Đưa sản phẩm lên chợ với đơn vị lưu trữ Hào chuẩn backend
                                    MarketItem listing = new MarketItem(
                                            UUID.randomUUID(),
                                            player.getUUID(),
                                            player.getScoreboardName(),
                                            itemInHand.copy(),
                                            priceInHao
                                    );

                                    KyoMarketState marketState = KyoMarketState.getServerState(player.level().getServer());
                                    marketState.addListing(listing);

                                    itemInHand.shrink(itemInHand.getCount());

                                    player.sendSystemMessage(Component.literal("Đã treo chợ thành công với giá " + priceInXu + " Xu!").withStyle(ChatFormatting.GREEN));

                                    player.level().getServer().getPlayerList().broadcastSystemMessage(
                                            Component.literal("").append(Component.literal("[Chợ Đen] ").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD))
                                                    .append(Component.literal(player.getScoreboardName() + " vừa treo bán một mặt hàng mới giá " + priceInXu + " Xu! Gõ ").withStyle(ChatFormatting.LIGHT_PURPLE))
                                                    .append(Component.literal("/" + KyoEconomyConfig.CMD_AH).withStyle(ChatFormatting.YELLOW))
                                                    .append(Component.literal(" để vào xem ngay.").withStyle(ChatFormatting.LIGHT_PURPLE)),
                                            false
                                    );

                                    return 1;
                                })
                        )
                )
        );
    }
}