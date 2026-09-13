package dev.hubpilot.hub.gui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.config.Destination;
import dev.hubpilot.hub.layout.NavigatorLayoutStore;
import dev.hubpilot.hub.util.MenuItems;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class DestinationMenuBuilder {
   private DestinationMenuBuilder() {
   }

   public static Inventory build(HubPilotHubPlugin var0, int var1) {
      List<Destination> var2 = var0.getDestinationStore().enabled();
      int var3 = NavigatorLayoutStore.inventorySize(var0);
      DestinationMenuHolder var4 = new DestinationMenuHolder(0);
      Inventory var5 = Bukkit.createInventory(var4, var3, MenuItems.colorize(var0.getMenuConfig().guiTitle()));
      var4.setInventory(var5);
      GuiCommon.fill(var5, var0.getMenuConfig().fillerMaterial());
      Map var6 = NavigatorLayoutStore.assignments(var0, var0.getDestinationStore().all());

      for (Destination var8 : var2) {
         Integer var9 = (Integer)var6.get(var8.id().toLowerCase(Locale.ROOT));
         if (var9 != null && var9 >= 0 && var9 < var3) {
            var5.setItem(var9, entryItem(var0, var8));
         }
      }

      return var5;
   }

   public static ItemStack entryItem(HubPilotHubPlugin var0, Destination var1) {
      return LegacyDestinationMenuBuilder.entryItem(var0, var1);
   }
}
