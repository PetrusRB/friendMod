package com.petrus.friend.registry;

import com.petrus.friend.FriendMod;
import com.petrus.friend.items.Pistol;
import com.petrus.friend.items.SuperAxe;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class AmiItems {
  public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FriendMod.MODID);

  public static final RegistryObject<Item> AXE = ITEMS.register("amiaxe",
      () -> new SuperAxe());

  public static final RegistryObject<Item> PISTOL = ITEMS.register("amipistol",
      () -> new Pistol());

  public static void register(IEventBus eventBus) {
    ITEMS.register(eventBus);
  }
}
