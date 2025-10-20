package com.petrus.friend.managers;

import java.util.UUID;

import com.petrus.friend.FriendMod;
import com.petrus.friend.entity.FriendBot;
import com.petrus.friend.registry.EntitiesRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gerencia spawn/despawn/respawn seguro do FriendBot.
 *
 * Regras:
 * - spawn(...) cria a entidade via
 * EntitiesRegistry.FRIEND_BOT.get().create(level)
 * - ao morrer, salva o antigo id do owner e agenda respawn após
 * respawnDelayTicks
 * - quando spawnar novamente, restaura o owner antigo salvo (caso ocontrario,
 * usa o default)
 * - se o mundo unloadar antes do respawn, cancela o respawn
 * - evita double-spawn e NPEs
 */
@Mod.EventBusSubscriber(modid = FriendMod.MODID)
public class BotManager {

  private static FriendBot botInstance = null;

  // respawn scheduling
  private static int respawnTicksRemaining = 0;
  private static ServerLevel respawnLevel = null;
  private static BlockPos respawnPos = null;
  private static final int DEFAULT_RESPAWN_DELAY_TICKS = 20 * 10; // 10s padrão

  // controla mensagem de "entrou no mundo" por sessão
  private static boolean hasAnnouncedThisSession = false;

  // default owner (aplicado a bots novos e existentes se configurado)
  private static UUID defaultOwner = null;
  private static UUID respawnOwner = null;

  // -------------------
  // API pública simples
  // -------------------
  public static synchronized void setDefaultOwner(UUID u) {
    defaultOwner = u;
    // aplica imediatamente no bot atual se houver
    if (botInstance != null && defaultOwner != null) {
      try {
        botInstance.setOwnerID(defaultOwner);
        System.out.println("[FriendMod] Default owner aplicado ao bot existente: " + defaultOwner);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static synchronized UUID getDefaultOwner() {
    return defaultOwner;
  }

  /**
   * Spawn na posição indicada. Se já existir, retorna a instância atual.
   *
   * @return instância do FriendBot (ou null se falhar)
   */
  public static synchronized FriendBot spawn(ServerLevel level, BlockPos pos) {
    try {
      if (botInstance != null && !botInstance.isRemoved())
        return botInstance;

      EntityType<FriendBot> type = EntitiesRegistry.FRIEND_BOT.get();
      if (type == null)
        return null;

      FriendBot bot = type.create(level);
      if (bot == null)
        return null; // pode ocorrer em ambientes estranhos

      // colocar items
      bot.initInventory();

      // aplicando default owner (se houver)
      if (respawnOwner != null) {
        bot.setOwnerID(respawnOwner);
        respawnOwner = null;
      } else if (bot.getOwnerID() == null && defaultOwner != null) {
        bot.setOwnerID(defaultOwner);
      }

      // position + persistência
      bot.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
      bot.setPersistenceRequired(); // evita despawn natural

      // adiciona ao mundo (server-side)
      level.addFreshEntity(bot);
      botInstance = bot;

      // Announce apenas se ainda não foi anunciado nessa sessão
      MinecraftServer server = level.getServer();
      if (server != null && !hasAnnouncedThisSession) {
        PlayerList list = server.getPlayerList();
        list.broadcastSystemMessage(Component.literal("§a" + bot.getName().getString() + " entrou no mundo!"), false);
        hasAnnouncedThisSession = true;
      }

      // Cancela qualquer respawn agendado anterior
      cancelScheduledRespawn();

      return botInstance;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Remove o bot do mundo e cancela respawn.
   */
  public static synchronized void despawn() {
    try {
      if (botInstance != null && !botInstance.isRemoved()) {
        Level botLevel = botInstance.level();
        botInstance.setRemoved(RemovalReason.DISCARDED);
        if (botLevel instanceof ServerLevel) {
          botInstance.discard();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      botInstance = null;
      cancelScheduledRespawn();
    }
  }

  /**
   * Força respawn imediato.
   */
  public static synchronized void forceRespawnNow(ServerLevel level, BlockPos pos) {
    cancelScheduledRespawn();
    spawn(level, pos);
  }

  // -------------------
  // Agendamento de respawn (interno)
  // -------------------

  private static synchronized void scheduleRespawn(ServerLevel level, BlockPos pos, int delayTicks) {
    respawnLevel = level;
    respawnPos = pos;
    respawnTicksRemaining = Math.max(1, delayTicks);
  }

  private static synchronized void cancelScheduledRespawn() {
    respawnTicksRemaining = 0;
    respawnLevel = null;
    respawnPos = null;
  }

  // -------------------
  // Eventos — subscribe
  // -------------------

  @SubscribeEvent
  public static void onLivingDeath(LivingDeathEvent event) {
    // server-side only
    if (event.getEntity() == null || event.getEntity().level().isClientSide)
      return;

    if (event.getEntity() instanceof FriendBot deadBot) {
      ServerLevel level = (ServerLevel) deadBot.level();
      BlockPos pos = deadBot.blockPosition();
      // Salvar owner antes de descartar
      UUID previousOwner = deadBot.getOwnerID();

      // marca para respawn depois de X ticks
      scheduleRespawn(level, pos, DEFAULT_RESPAWN_DELAY_TICKS);

      // permitir mostrar "entrou novamente" no respawn:
      hasAnnouncedThisSession = false;

      // remover referência imediatamente pra evitar double-use
      botInstance = null;

      // anunciar morte (opcional)
      MinecraftServer server = level.getServer();
      if (server != null) {
        server.getPlayerList().broadcastSystemMessage(
            Component.literal("§c" + deadBot.getName().getString() + " foi morto! Reiniciando..."), false);
      }
      // armazenar owner temporariamente
      respawnOwner = previousOwner;
    }
  }

  @SubscribeEvent
  public static void onServerTick(TickEvent.ServerTickEvent event) {
    // apenas no END e START? usamos END para reduzir impacto
    if (event.phase != TickEvent.Phase.END)
      return;

    // handle respawn countdown
    if (respawnTicksRemaining > 0) {
      respawnTicksRemaining--;
      if (respawnTicksRemaining <= 0) {
        // tempo de respawn
        try {
          if (respawnLevel != null && !respawnLevel.isClientSide) {
            // se o chunk estiver carregado ou o level ativo, spawn imediatamente
            if (respawnPos == null)
              respawnPos = respawnLevel.getSharedSpawnPos(); // fallback
            spawn(respawnLevel, respawnPos);
          }
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          // limpar agendamento
          cancelScheduledRespawn();
        }
      }
    } else {
      // sanity check: se botInstance foi removido sem passar por LivingDeathEvent,
      // mantenha a referência consistente e schedule respawn curto.
      if (botInstance != null && botInstance.isRemoved()) {
        // captura world e pos se possível
        try {
          ServerLevel lvl = (ServerLevel) botInstance.level();
          BlockPos pos = botInstance.blockPosition();
          botInstance = null;
          scheduleRespawn(lvl, pos, DEFAULT_RESPAWN_DELAY_TICKS);
          hasAnnouncedThisSession = false;
        } catch (Exception ignore) {
          botInstance = null;
        }
      }
    }
  }

  @SubscribeEvent
  public static void onLevelUnload(LevelEvent.Unload event) {
    // Se o mundo que será descarregado tem respawn agendado, cancela (evita tentar
    // spawnar em world descarregado)
    if (respawnLevel != null && event.getLevel() == respawnLevel) {
      cancelScheduledRespawn();
    }

    // Se o botInstance pertence a esse level, limpa referência (evita NPE ao
    // acessar level posteriormente)
    if (botInstance != null && botInstance.level() == event.getLevel()) {
      botInstance = null;
    }
  }

  // -------------------
  // Utilitários / inspeção
  // -------------------

  public static synchronized boolean isSpawned() {
    return botInstance != null && !botInstance.isRemoved();
  }

  public static synchronized FriendBot getBotInstance() {
    return botInstance;
  }

  public static synchronized int getRespawnTicksRemaining() {
    return respawnTicksRemaining;
  }
}
