package dev.hubpilot.hub.gui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class NavigatorLiveRefresh {
   private NavigatorLiveRefresh() {
   }

   public static void refreshOpen(HubPilotHubPlugin var0) {
      for (Player var2 : Bukkit.getOnlinePlayers()) {
         Inventory var3 = var2.getOpenInventory().getTopInventory();
         if (var3.getHolder() instanceof DestinationMenuHolder var4) {
            Inventory var7 = DestinationMenuBuilder.build(var0, var4.page());
            if (var7.getSize() == var3.getSize()) {
               for (int var6 = 0; var6 < var3.getSize(); var6++) {
                  var3.setItem(var6, var7.getItem(var6));
               }

               var2.updateInventory();
            }
         }
      }
   }
}
