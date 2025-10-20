package com.petrus.friend.items;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public class SuperAxe extends AxeItem {
  // Definindo um Tier personalizado (material do machado)
  public static final Tier FRIEND_TIER = new Tier() {
    @Override
    public int getUses() {
      return 5000;
    }

    @Override
    public float getSpeed() {
      return 12.0F;
    }

    @Override
    public float getAttackDamageBonus() {
      return 20.0F;
    }

    @Override
    public int getLevel() {
      return 4;
    }

    @Override
    public int getEnchantmentValue() {
      return 30;
    }

    @Override
    public Ingredient getRepairIngredient() {
      return Ingredient.of(Items.IRON_INGOT, Items.STICK);
    }
  };

  public SuperAxe() {
    super(FRIEND_TIER, 20.0F, -2.8F,
        new Properties().stacksTo(1).durability(500));
  }
}
