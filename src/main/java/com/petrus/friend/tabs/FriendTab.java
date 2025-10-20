package com.petrus.friend.tabs;

import com.petrus.friend.FriendMod;
import com.petrus.friend.registry.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class FriendTab {
  public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
      .create(Registries.CREATIVE_MODE_TAB, FriendMod.MODID);

  public static final RegistryObject<CreativeModeTab> FRIEND_TAB = CREATIVE_MODE_TABS.register("friend_tab",
      () -> CreativeModeTab.builder().icon(() -> new ItemStack(AmiItems.AXE.get()))
          .title(Component.literal("Amiguinho").withStyle(style -> style.withColor(0xFF0000)))
          .displayItems((pParameters, pOutput) -> {
            pOutput.accept(AmiItems.AXE.get());
            pOutput.accept(AmiItems.PISTOL.get());

          })
          .build());

  public static void register(IEventBus eventBus) {
    CREATIVE_MODE_TABS.register(eventBus);
  }
}
