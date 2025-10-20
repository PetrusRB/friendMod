package com.petrus.friend;

import com.mojang.logging.LogUtils;
import com.petrus.friend.registry.AmiItems;
import com.petrus.friend.registry.EntitiesRegistry;
import com.petrus.friend.registry.SoundRegistry;
import com.petrus.friend.tabs.FriendTab;
import com.petrus.friend.commands.impl.BotCommands;
import com.petrus.friend.entity.renderer.*;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;

@Mod(FriendMod.MODID)
public class FriendMod {
  public static final String MODID = "friend";
  public static final ResourceLocation FRIEND_SKIN = new ResourceLocation(FriendMod.MODID,
      "textures/entity/bot.png");

  private static final Logger LOGGER = LogUtils.getLogger();

  public FriendMod(FMLJavaModLoadingContext context) {
    IEventBus modEventBus = context.getModEventBus();
    EntitiesRegistry.register(modEventBus);

    FriendTab.register(modEventBus);
    SoundRegistry.register(modEventBus);
    AmiItems.register(modEventBus);
    BotCommands.registerCommands();

    modEventBus.addListener(this::commonSetup);
    MinecraftForge.EVENT_BUS.register(this);
    context.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "friend-common.toml");
  }

  private void commonSetup(final FMLCommonSetupEvent event) {
    LOGGER.info("Iniciando o amiguinho");
  }

  @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
  public static class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
      EntityRenderers.register(EntitiesRegistry.BULLET.get(), BulletEntityRenderer::new);
      EntityRenderers.register(EntitiesRegistry.FRIEND_BOT.get(), FriendBotRenderer::new);
    }
  }
}
