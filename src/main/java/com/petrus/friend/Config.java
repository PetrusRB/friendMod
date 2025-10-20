package com.petrus.friend;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = FriendMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
  private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

  public static final ForgeConfigSpec SPEC = BUILDER.build();
  public static final ForgeConfigSpec CONFIG;
  public static final Common COMMON;

  private static UUID ownerUUID = UUID.fromString("dab21994-ec10-4c31-ae46-72124dd07a43");

  static {
    COMMON = new Common(BUILDER);
    CONFIG = BUILDER.build();
  }

  public static class Common {
    public final ForgeConfigSpec.ConfigValue<String> ownerUUIDString;

    public Common(ForgeConfigSpec.Builder builder) {
      builder.push("General");

      ownerUUIDString = builder
          .comment("UUID do dono do bot")
          .define("ownerUUID", ownerUUID.toString());

      builder.pop();
    }

    // Modifica o UUID do dono do bot (na config)
    public void setOwnerUUID(UUID uuid) {
      ownerUUIDString.set(uuid.toString());
    }

    // Pega o UUID do dono do bot (na config)
    public UUID getOwnerUUID() {
      try {
        return UUID.fromString(ownerUUIDString.get());
      } catch (IllegalArgumentException e) {
        return null;
      }
    }

  }
}
