package com.petrus.friend.items;

import com.petrus.friend.entity.BulletEntity;
import com.petrus.friend.registry.SoundRegistry;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

public class Pistol extends Item {
  public Pistol() {
    super(new Item.Properties().stacksTo(1).durability(500));
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    // só dispara no servidor
    if (!level.isClientSide) {
      float damage = 120f; // daninho
      BulletEntity bullet = new BulletEntity(level, player, damage);
      bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.5F /* velocidade */, 1.0F /*
                                                                                                              * inaccuracy
                                                                                                              */);
      level.addFreshEntity(bullet);

      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundRegistry.PISTOL_SHOT_SOUND.get(),
          SoundSource.PLAYERS, 1.0F, 1.0F);
      player.getCooldowns().addCooldown(this, 8); // taxa de tiro
      stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
    }

    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
  }

  @Override
  public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    // sangue
    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0)); // efeito

    // Som de corte mais brutal
    attacker.level().playSound(null, attacker.blockPosition(),
        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.8F);

    return super.hurtEnemy(stack, target, attacker);
  }
}
