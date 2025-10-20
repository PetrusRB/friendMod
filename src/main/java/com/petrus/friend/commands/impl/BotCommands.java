package com.petrus.friend.commands.impl;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import java.util.UUID;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.petrus.friend.Config;
import com.petrus.friend.commands.CommandHandler;
import com.petrus.friend.entity.FriendBot;
import com.petrus.friend.managers.BotManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.Commands;

/**
 * Comandos /friend:
 * - /friend spawn [x y z]
 * - /friend despawn
 * - /friend status
 * - /friend reload <- recarrega configurações de forma segura
 * - /friend owner <- mostra dono do bot (ou default)
 * - /friend setowner <player>
 * - /friend respawnnow
 *
 * Registre chamando: FriendCommands.registerCommands();
 * (o CommandManager fará o resto)
 */
public final class BotCommands {

  private BotCommands() {
  }

  public static void registerCommands() {
    CommandHandler.register(
        literal("friend")
            .requires(src -> src.hasPermission(0)) // qualquer um pode ver, subcomandos checam permissões
            .then(literal("spawn")
                .requires(src -> src.hasPermission(2)) // op required
                .executes(ctx -> spawnAtInvoker(ctx.getSource()))
                .then(argument("x", DoubleArgumentType.doubleArg())
                    .then(argument("y", DoubleArgumentType.doubleArg())
                        .then(argument("z", DoubleArgumentType.doubleArg())
                            .executes(ctx -> {
                              double x = DoubleArgumentType.getDouble(ctx, "x");
                              double y = DoubleArgumentType.getDouble(ctx, "y");
                              double z = DoubleArgumentType.getDouble(ctx, "z");
                              return spawnAtCoords(ctx.getSource(), x, y, z);
                            })))))
            .then(literal("despawn")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                  BotManager.despawn();
                  ctx.getSource().sendSuccess(() -> Component.literal("Friend removido."), true);
                  return 1;
                }))
            .then(literal("status")
                .executes(ctx -> {
                  boolean s = BotManager.isSpawned();
                  ctx.getSource().sendSuccess(() -> Component.literal("Friend spawnado: " + s), false);
                  return s ? 1 : 0;
                }))
            .then(literal("refresh")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                  try {
                    Config.CONFIG.afterReload();
                    return 1;
                  } catch (Exception e) {
                    return 0;
                  }
                }))
            .then(literal("reload")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                  CommandSourceStack src = ctx.getSource();
                  try {
                    // Tenta chamar hook de reload do config (se existir)
                    try {
                      if (Config.CONFIG != null)
                        Config.CONFIG.afterReload();
                    } catch (Throwable ignore) {
                      // some mappings/environments may not expose afterReload; ignore
                    }

                    // lê owner default do config
                    UUID defaultOwner = null;
                    try {
                      defaultOwner = Config.COMMON.getOwnerUUID();
                    } catch (Exception ignored) {
                    }

                    // aplica no BotManager (que por sua vez aplica ao bot vivo)
                    BotManager.setDefaultOwner(defaultOwner);

                    src.sendSuccess(() -> Component.literal("Config recarregada com segurança."), true);
                    return 1;
                  } catch (Exception e) {
                    e.printStackTrace();
                    src.sendFailure(Component.literal("Falha ao recarregar config (ver console)."));
                    return 0;
                  }
                }))
            .then(literal("owner")
                .executes(ctx -> {
                  CommandSourceStack src = ctx.getSource();
                  try {
                    FriendBot bot = BotManager.getBotInstance();

                    // Coleta informações (mutáveis durante a lógica)
                    UUID ownerUuid = null;
                    String ownerName = null;
                    boolean botExists = bot != null;

                    if (botExists) {
                      ownerUuid = bot.getOwnerID();
                      if (ownerUuid != null && bot.level() instanceof ServerLevel sl) {
                        Player p = sl.getPlayerByUUID(ownerUuid);
                        if (p != null)
                          ownerName = p.getName().getString();
                      }
                    }

                    // Se não encontrou owner na instância, tenta default do BotManager
                    if (ownerUuid == null) {
                      ownerUuid = BotManager.getDefaultOwner();
                      if (ownerUuid != null && src.getServer() != null) {
                        ServerPlayer sp = src.getServer().getPlayerList().getPlayer(ownerUuid);
                        if (sp != null)
                          ownerName = sp.getName().getString();
                      }
                    }

                    // transfere para finais para uso nas lambdas
                    final boolean finalBotExists = botExists;
                    final UUID finalOwnerUuid = ownerUuid;
                    final String finalOwnerName = ownerName;

                    if (!finalBotExists) {
                      if (finalOwnerUuid != null) {
                        String shortUuid = finalOwnerUuid.toString().substring(0, 8);
                        src.sendSuccess(() -> Component.literal("Nenhum bot spawnado. Owner default: "
                            + (finalOwnerName != null ? finalOwnerName : shortUuid)), false);
                        return 1;
                      } else {
                        src.sendFailure(Component.literal("Nenhum bot spawnado e sem owner default configurado."));
                        return 0;
                      }
                    } else {
                      if (finalOwnerUuid == null) {
                        src.sendFailure(Component.literal("Bot spawnado, mas sem owner configurado."));
                        return 0;
                      } else {
                        String who = finalOwnerName != null ? finalOwnerName
                            : finalOwnerUuid.toString().substring(0, 8);
                        src.sendSuccess(() -> Component.literal("Owner do bot: " + who), false);
                        return 1;
                      }
                    }
                  } catch (Exception e) {
                    e.printStackTrace();
                    src.sendFailure(Component.literal("Erro ao obter owner (ver console)."));
                    return 0;
                  }
                }))

            .then(literal("setowner")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> {
                      try {
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        definirDono(ctx.getSource(), target);
                        return 1;
                      } catch (CommandSyntaxException e) {
                        ctx.getSource().sendFailure(Component.literal("Jogador não encontrado"));
                        e.printStackTrace();
                        return 0;
                      }
                    })))
            .then(literal("respawnnow")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                  // respawn no mundo do invoker (ou overworld se console)
                  CommandSourceStack src = ctx.getSource();
                  ServerLevel lvl = toServerLevelOrOverworld(src);
                  if (lvl == null) {
                    src.sendFailure(Component.literal("Erro: mundo não disponível."));
                    return 0;
                  }
                  BlockPos pos = src.getEntity() instanceof Player p ? p.blockPosition().above(1)
                      : lvl.getSharedSpawnPos();
                  BotManager.forceRespawnNow(lvl, pos);
                  src.sendSuccess(() -> Component.literal("Forçando respawn do Friend..."), true);
                  // note: last line used src.getSource() to avoid ambiguity in some mappings; if
                  // compiler complains, use src.sendSuccess(...)
                  return 1;
                })));
  }

  // helper: definir o dono do bot (de uma forma segura)
  private static int definirDono(CommandSourceStack src, ServerPlayer target) {
    try {
      FriendBot bot = null;
      if (src.getEntity() instanceof Player p) {
        // usar AABB gerado a partir do bounding box do player
        bot = src.getLevel().getNearestEntity(
            FriendBot.class,
            TargetingConditions.forNonCombat(),
            p,
            p.getX(), p.getY(), p.getZ(),
            p.getBoundingBox().inflate(32.0) // <-- Lembrar que é AABB, não int
        );
      } else {
        bot = BotManager.getBotInstance();
      }

      if (bot == null) {
        src.sendFailure(Component.literal("O bot não está spawnado! Use /friend spawn primeiro."));
        return 0;
      }

      boolean ok = bot.setOwnerID(target.getUUID());
      src.sendSuccess(() -> Component.literal(ok ? "Owner atualizado." : "Nada alterado."), true);
      return ok ? 1 : 0;
    } catch (Exception e) {
      e.printStackTrace();
      src.sendFailure(Component.literal("ERRO ao executar setOwner (ver console)."));
      return 0;
    }
  }

  // helper: spawn simpels (invoker position or console -> overworld spawn)
  private static int spawnAtInvoker(CommandSourceStack src) {
    try {
      ServerLevel lvl = toServerLevelOrOverworld(src);
      if (lvl == null) {
        src.sendFailure(Component.literal("Mundo indisponível."));
        return 0;
      }
      BlockPos pos;
      if (src.getEntity() instanceof Player p) {
        pos = p.blockPosition().above(1);
      } else {
        pos = lvl.getSharedSpawnPos();
      }
      BotManager.spawn(lvl, pos);
      src.sendSuccess(() -> Component.literal("Friend spawnado em " + pos.toShortString()), true);
      return 1;
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }

  private static int spawnAtCoords(CommandSourceStack src, double x, double y, double z) {
    ServerLevel lvl = toServerLevelOrOverworld(src);
    if (lvl == null) {
      src.sendFailure(Component.literal("Mundo indisponível."));
      return 0;
    }
    BlockPos pos = new BlockPos((int) x, (int) y, (int) z);
    BotManager.spawn(lvl, pos);
    src.sendSuccess(() -> Component.literal("Friend spawnado em " + pos.toShortString()), true);
    return 1;
  }

  private static ServerLevel toServerLevelOrOverworld(CommandSourceStack src) {
    Level level = src.getLevel();
    if (!(level instanceof ServerLevel serverLevel)) {
      src.sendFailure(Component.literal("Erro: Este comando só funciona no servidor."));
      return null;
    }

    try {
      if (level instanceof ServerLevel sl)
        return sl;
      if (src.getServer() != null) {
        return src.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
      }
    } catch (Throwable ignored) {
    }
    return null;
  }
}
