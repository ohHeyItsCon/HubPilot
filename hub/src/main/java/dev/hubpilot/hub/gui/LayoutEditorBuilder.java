package dev.hubpilot.hub.gui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.config.Destination;
import dev.hubpilot.hub.layout.NavigatorLayoutStore;
import dev.hubpilot.hub.util.MenuItems;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

public final class LayoutEditorBuilder {
   private LayoutEditorBuilder() {
   }

   public static Inventory build(HubPilotHubPlugin var0, Destination var1) {
      int var2 = NavigatorLayoutStore.inventorySize(var0);
      LayoutEditorHolder var3 = new LayoutEditorHolder(var1.id());
      Inventory var4 = Bukkit.createInventory(var3, var2, MenuItems.colorize("&8Navigator Slot: &f" + var1.label()));
      var3.setInventory(var4);
      Map var5 = NavigatorLayoutStore.assignments(var0, var0.getDestinationStore().all());
      HashMap var6 = new HashMap();

      for (Destination var8 : var0.getDestinationStore().all()) {
         Integer var9 = (Integer)var5.get(var8.id().toLowerCase(Locale.ROOT));
         if (var9 != null && var9 >= 0 && var9 < var2) {
            var6.put(var9, var8);
         }
      }

      for (int var10 = 0; var10 < var2; var10++) {
         Destination var11 = (Destination)var6.get(var10);
         if (var11 == null) {
            var4.setItem(
               var10,
               MenuItems.named(
                  Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&7Slot " + var10, List.of("&aClick to place &f" + var1.label(), "&8Config slot: " + var10)
               )
            );
         } else {
            var4.setItem(
               var10,
               MenuItems.named(
                  var11.id().equalsIgnoreCase(var1.id()) ? Material.LIME_DYE : Material.CHEST,
                  "&fSlot " + var10 + " &8- &b" + var11.label(),
                  List.of(var11.id().equalsIgnoreCase(var1.id()) ? "&aCurrent slot" : "&eClick to move " + var1.label() + " here", "&8Config slot: " + var10)
               )
            );
         }
      }

      return var4;
   }
}
