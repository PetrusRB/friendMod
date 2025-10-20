package com.petrus.friend.entity.renderer;

import com.petrus.friend.FriendMod;
import com.petrus.friend.entity.BulletEntity;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class BulletEntityRenderer extends ArrowRenderer<BulletEntity> {
  public BulletEntityRenderer(EntityRendererProvider.Context context) {
    super(context);
  }

  @Override
  public ResourceLocation getTextureLocation(BulletEntity entity) {
    return new ResourceLocation(FriendMod.MODID, "textures/entity/bullet.png");
  }
}
