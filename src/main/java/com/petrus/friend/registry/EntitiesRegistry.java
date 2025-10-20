package com.petrus.friend.registry;

import com.petrus.friend.FriendMod;
import com.petrus.friend.entity.BulletEntity;
import com.petrus.friend.entity.FriendBot;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class EntitiesRegistry {
  public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE,
      FriendMod.MODID);

  public static final RegistryObject<EntityType<FriendBot>> FRIEND_BOT = ENTITIES.register("friend_bot",
      () -> EntityType.Builder
          .<FriendBot>of(FriendBot::new, MobCategory.MISC)
          .sized(0.6F, 1.8F)
          .clientTrackingRange(8)
          .build(new ResourceLocation(FriendMod.MODID, "friend_bot").toString()));

  public static final RegistryObject<EntityType<BulletEntity>> BULLET = ENTITIES.register("bullet",
      () -> EntityType.Builder.<BulletEntity>of(BulletEntity::new, MobCategory.MISC)
          .sized(0.25f, 0.25f)
          .clientTrackingRange(4)
          .updateInterval(1)
          .build(new ResourceLocation(FriendMod.MODID, "bullet").toString()));

  public static void register(IEventBus eventBus) {
    ENTITIES.register(eventBus);
  }
}
