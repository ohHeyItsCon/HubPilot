package dev.hubpilot.hub.listener;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.adminui.AdminHomeBuilder;
import dev.hubpilot.hub.gui.DestinationMenuBuilder;
import dev.hubpilot.hub.publicapi.PublicHubBootstrap;
import dev.hubpilot.hub.util.MenuItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class ItemInteractListener implements Listener {
   private final HubPilotHubPlugin plugin;

   public ItemInteractListener(HubPilotHubPlugin var1) {
      this.plugin = var1;
   }

   @EventHandler
   public void onInteract(PlayerInteractEvent var1) {
      if (var1.getHand() == EquipmentSlot.HAND) {
         Action var2 = var1.getAction();
         if (var2 == Action.RIGHT_CLICK_AIR || var2 == Action.RIGHT_CLICK_BLOCK) {
            ItemStack var3 = var1.getItem();
            if (MenuItems.isMenuOpener(this.plugin, var3)) {
               var1.setCancelled(true);
               var1.getPlayer().openInventory(DestinationMenuBuilder.build(this.plugin, 0));
            } else {
               if (MenuItems.isAdminOpener(this.plugin, var3)) {
                  var1.setCancelled(true);
                  Player var4 = var1.getPlayer();
                  if (!PublicHubBootstrap.canAdmin(var4)) {
                     var4.sendMessage("§cYou do not have permission to use the HubPilot editor.");
                     return;
                  }

                  var4.openInventory(AdminHomeBuilder.build(this.plugin));
               }
            }
         }
      }
   }
}
