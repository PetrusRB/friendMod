package com.petrus.friend.events;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.UUID;

import com.petrus.friend.Config;
import com.petrus.friend.FriendMod;
import com.petrus.friend.managers.BotManager;

@Mod.EventBusSubscriber(modid = FriendMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ConfigEvents {
  @SubscribeEvent
  public static void onConfigReload(ModConfigEvent.Reloading event) {
    if (event.getConfig().getSpec() != Config.SPEC)
      return;

    UUID defaultOwner = null;
    try {
      defaultOwner = Config.COMMON.getOwnerUUID();
    } catch (Exception e) {
      // ignorar (caso a config não estiver pronta)
    }

    // atualiza o BotManager (que aplica ao bot vivo também)
    BotManager.setDefaultOwner(defaultOwner);
  }
}
