package com.petrus.friend.events;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import com.petrus.friend.entity.FriendBot;
import com.petrus.friend.registry.EntitiesRegistry;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class BotAttributeEvent {
  // Chamar quando um atributo em uma entidade for criada
  @SubscribeEvent
  public static void registerAttributes(EntityAttributeCreationEvent event) {
    event.put(EntitiesRegistry.FRIEND_BOT.get(), FriendBot.createAttributes().build());
  }
}
