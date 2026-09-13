package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class HelpMenuListener implements Listener {
   private final HubPilotHubPlugin p;

   public HelpMenuListener(HubPilotHubPlugin var1) {
      this.p = var1;
   }

   @EventHandler
   public void c(InventoryClickEvent var1) {
      if (var1.getInventory().getHolder() instanceof HelpMenuHolder) {
         var1.setCancelled(true);
         if (var1.getWhoClicked() instanceof Player var2 && var1.getRawSlot() == 36) {
            var2.openInventory(AdminHomeBuilder.build(this.p));
         }
      }
   }
}
