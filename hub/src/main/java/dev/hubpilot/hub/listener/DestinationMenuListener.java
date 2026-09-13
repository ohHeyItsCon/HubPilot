package dev.hubpilot.hub.listener;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.bridge.MessageEvent;
import dev.hubpilot.hub.config.Destination;
import dev.hubpilot.hub.gui.DestinationMenuHolder;
import dev.hubpilot.hub.layout.NavigatorLayoutStore;
import dev.hubpilot.hub.util.MenuItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class DestinationMenuListener implements Listener {
   private final HubPilotHubPlugin plugin;

   public DestinationMenuListener(HubPilotHubPlugin var1) {
      this.plugin = var1;
   }

   @EventHandler
   public void onClick(InventoryClickEvent var1) {
      if (var1.getInventory().getHolder() instanceof DestinationMenuHolder) {
         var1.setCancelled(true);
         if (var1.getWhoClicked() instanceof Player var2) {
            if (var1.getClickedInventory() != null) {
               Inventory var4 = var1.getClickedInventory();
               var1.getInventory();
               if (var4 == var4) {
                  int var6 = var1.getRawSlot();
                  Destination var7 = NavigatorLayoutStore.destinationAt(this.plugin, var6, this.plugin.getDestinationStore().enabled());
                  if (var7 == null) {
                     return;
                  }

                  var2.closeInventory();
                  if (this.plugin.getHubPilotStore().messageEnabled(var7.id(), MessageEvent.CONNECTING)) {
                     String var5 = this.plugin
                        .getHubPilotStore()
                        .messageText(var7.id(), MessageEvent.CONNECTING)
                        .replace("{server}", "&f&l" + var7.label() + "&r")
                        .replace("{server_plain}", var7.label())
                        .replace("{id}", var7.id());
                     var2.sendMessage(MenuItems.colorize(var5));
                  }

                  this.plugin.getRequestSender().requestJoin(var2, var7);
                  return;
               }
            }
         }
      }
   }

   @EventHandler
   public void onDrag(InventoryDragEvent var1) {
      if (var1.getInventory().getHolder() instanceof DestinationMenuHolder) {
         var1.setCancelled(true);
      }
   }
}
