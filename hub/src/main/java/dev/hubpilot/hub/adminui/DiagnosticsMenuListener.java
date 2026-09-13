package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.publicapi.PublicHubBootstrap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class DiagnosticsMenuListener implements Listener {
   private final HubPilotHubPlugin p;

   public DiagnosticsMenuListener(HubPilotHubPlugin var1) {
      this.p = var1;
   }

   @EventHandler
   public void c(InventoryClickEvent var1) {
      if (var1.getInventory().getHolder() instanceof DiagnosticsMenuHolder) {
         var1.setCancelled(true);
         if (var1.getWhoClicked() instanceof Player var2 && PublicHubBootstrap.canAdmin(var2)) {
            Inventory var10000 = var1.getClickedInventory();
            var1.getInventory();
            if (var10000 == var10000) {
               if (var1.getRawSlot() == 31) {
                  this.p.reloadEverything();
                  var2.sendMessage("§aHubPilot Hub reloaded.");
                  var2.openInventory(DiagnosticsMenuBuilder.build(this.p));
               } else if (var1.getRawSlot() == 36) {
                  var2.openInventory(AdminHomeBuilder.build(this.p));
               }
            }
         }
      }
   }
}
