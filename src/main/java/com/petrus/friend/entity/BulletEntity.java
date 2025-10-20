package com.petrus.friend.entity;

import com.petrus.friend.registry.EntitiesRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class BulletEntity extends AbstractArrow {
  public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
    super(type, level);
  }

  public BulletEntity(Level level, LivingEntity shooter, float damage) {
    this(EntitiesRegistry.BULLET.get(), level);
    this.setOwner(shooter);

    // Posição inicial e disparo
    this.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
    this.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, 3.0F, 1.0F);

    this.setCritArrow(false); // evita erros de crit
  }

  @Override
  protected void defineSynchedData() {
    super.defineSynchedData();
    // Nada é preciso, pois AbstractArrow cuida do crit internamente
  }

  @Override
  protected void onHitEntity(EntityHitResult result) {
    super.onHitEntity(result);

    if (!this.level().isClientSide) {
      Entity target = result.getEntity();

      // mata qualquer entidade
      target.discard(); // usado internamente pelo jogo
      // target.kill(); // força state DEAD

      this.discard(); // remove o projétil
    }
  }

  @Override
  protected void onHitBlock(BlockHitResult result) {
    super.onHitBlock(result);
    if (!this.level().isClientSide)
      this.discard();
  }

  @Override
  public void tick() {
    super.tick();
    if (this.level().isClientSide) {
      this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
    }
  }

  @Override
  protected ItemStack getPickupItem() {
    return ItemStack.EMPTY; // Não pode ser pego
  }
}
