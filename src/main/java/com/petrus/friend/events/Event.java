package com.petrus.friend.events;

import org.joml.Vector3f;

import com.petrus.friend.FriendMod;
import com.petrus.friend.items.SuperAxe;
import com.petrus.friend.registry.SoundRegistry;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FriendMod.MODID)
public class Event {
  @SubscribeEvent
  public static void onEntityKilled(LivingDeathEvent event) {
    if (!(event.getSource().getEntity() instanceof Player player))
      return;

    if (!(player.getMainHandItem().getItem() instanceof SuperAxe))
      return; // Checa se está usando o machado

    Level level = player.level();
    Entity target = event.getEntity();

    // tocar somzin
    level.playSound(null, player.blockPosition(), SoundRegistry.AXE_KILL_SOUND.get(),
        SoundSource.PLAYERS, 1.0F, 1.0F);

    // efeito de sangue (20 partículas vermelhas)
    for (int i = 0; i < 20; i++) {
      double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
      double offsetY = level.random.nextDouble() * 1.0;
      double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

      level.addParticle(
          new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), 1.0F), // Cor vermelha e tamanho 1.0
          target.getX() + offsetX,
          target.getY() + 1.0 + offsetY,
          target.getZ() + offsetZ,
          0.0, 0.1, 0.0 // movimento
      );
    }
    // dar speed ao jogador
    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1)); // 10s speed 2
  }
}
