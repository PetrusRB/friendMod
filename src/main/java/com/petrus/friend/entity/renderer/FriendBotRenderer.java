package com.petrus.friend.entity.renderer;

import com.petrus.friend.FriendMod;
import com.petrus.friend.entity.FriendBot;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer para FriendBot. Internamente delega para PlayerRenderer para
 * aproveitar o model padrão.
 */
public class FriendBotRenderer extends HumanoidMobRenderer<FriendBot, HumanoidModel<FriendBot>> {

  public FriendBotRenderer(EntityRendererProvider.Context ctx) {
    super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
  }

  @Override
  public ResourceLocation getTextureLocation(FriendBot entity) {
    return FriendMod.FRIEND_SKIN; // PNG normal ou HD
  }
}
