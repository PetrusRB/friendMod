package com.petrus.friend.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * CommandManager: armazena builders e registra tudo no
 * RegisterCommandsEvent.
 *
 * Uso:
 * CommandManager.register(rootBuilder);
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandHandler {
  private static final List<LiteralArgumentBuilder<CommandSourceStack>> ROOTS = Collections
      .synchronizedList(new ArrayList<>());

  /**
   * Registra um root command (p.ex. Commands.literal("friend")...).
   * Pode ser chamado a qualquer momento antes do RegisterCommandsEvent.
   */
  public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
    if (root == null)
      return;
    ROOTS.add(root);
  }

  /**
   * Evento do Forge que registra todos os builders no dispatcher.
   * Executado uma vez por servidor/client onde apropriado.
   */
  @SubscribeEvent
  public static void onRegisterCommands(RegisterCommandsEvent event) {
    synchronized (ROOTS) {
      for (LiteralArgumentBuilder<CommandSourceStack> root : ROOTS) {
        try {
          event.getDispatcher().register(root);
        } catch (Exception e) {
          // Falha para um comando não impede os demais
          e.printStackTrace();
        }
      }
    }
  }
}
