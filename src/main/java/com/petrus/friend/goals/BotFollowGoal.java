package com.petrus.friend.goals;

import com.petrus.friend.entity.FriendBot;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * Uma goal customizada para seguir o dono do bot
 * Parametros:
 * - bot:
 * - speed: velocidade de movimento
 * - minDist: distancia minima para seguir
 * - maxDist: distancia maxima para seguir
 */
public class BotFollowGoal extends Goal {
  private final FriendBot bot;
  private final double speed;
  private final float minDist;
  private final float maxDist;
  private Player owner;

  public BotFollowGoal(FriendBot bot, double speed, float minDist, float maxDist) {
    this.bot = bot;
    this.speed = speed;
    this.minDist = minDist;
    this.maxDist = maxDist;
    this.setFlags(java.util.EnumSet.of(net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE,
        net.minecraft.world.entity.ai.goal.Goal.Flag.LOOK));
  }

  @Override
  public boolean canUse() {
    owner = bot.getOwnerPlayer();
    if (owner == null)
      return false;
    return bot.distanceTo(owner) > 3.0F;
  }

  @Override
  public boolean canContinueToUse() {
    if (owner == null || owner.isRemoved())
      return false;
    double d = bot.distanceToSqr(owner);
    return d > (double) (minDist * minDist);
  }

  @Override
  public void start() {
    // nothing
  }

  @Override
  public void stop() {
    owner = null;
    bot.getNavigation().stop();
  }

  @Override
  public void tick() {
    if (owner == null)
      return;
    bot.getLookControl().setLookAt(owner, 30.0F, 30.0F);
    if (!bot.getNavigation().isDone())
      return;
    double d = bot.distanceToSqr(owner);
    if (d > (double) (maxDist * maxDist)) {
      bot.getNavigation().moveTo(owner, speed);
    } else if (d > (double) (minDist * minDist)) {
      bot.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), speed);
    } else {
      bot.getNavigation().stop();
    }

    // fallback teleport if very far
    if (d > 2500.0) {
      bot.teleportTo(owner.getX(), owner.getY(), owner.getZ());
    }
  }
}
