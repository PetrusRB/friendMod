package com.petrus.friend.registry;

import com.petrus.friend.FriendMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundRegistry {
  public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
      FriendMod.MODID);

  public static final RegistryObject<SoundEvent> AXE_KILL_SOUND = SOUNDS.register("axe_kill",
      () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(FriendMod.MODID, "axe_kill")));

  public static final RegistryObject<SoundEvent> PISTOL_SHOT_SOUND = SOUNDS.register("gun_shot",
      () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(FriendMod.MODID, "gun_shot")));

  public static void register(IEventBus eventBus) {
    SOUNDS.register(eventBus);
  }
}
