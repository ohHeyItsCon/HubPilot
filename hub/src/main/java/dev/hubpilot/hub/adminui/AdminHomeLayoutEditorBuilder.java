package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.util.MenuItems;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

public final class AdminHomeLayoutEditorBuilder {
   private AdminHomeLayoutEditorBuilder() {
   }

   public static Inventory build(HubPilotHubPlugin var0, String var1) {
      AdminHomeLayoutEditorHolder var2 = new AdminHomeLayoutEditorHolder(var1);
      Inventory var3 = Bukkit.createInventory(var2, 54, MenuItems.colorize("&8&lAdmin Home Layout"));
      var2.setInventory(var3);

      for (int var4 = 0; var4 < 54; var4++) {
         var3.setItem(
            var4,
            MenuItems.named(
               Material.LIGHT_GRAY_STAINED_GLASS_PANE,
               "&7Empty Slot",
               List.of("&8GUI index: " + var4, var1 == null ? "&7Select a submenu item first" : "&eClick to place selected item")
            )
         );
      }

      Map var8 = AdminHomeLayout.assignments(var0);

      for (String var6 : AdminHomeLayout.ids()) {
         Integer var7 = (Integer)var8.get(var6);
         if (var7 != null) {
            var3.setItem(var7, AdminHomeLayout.item(var6, var6.equals(var1)));
         }
      }

      var3.setItem(
         34, MenuItems.named(Material.NETHER_STAR, "&c&lReserved: About HubPilot", List.of("&7Fixed Admin Home slot", "&8This slot cannot be assigned"))
      );
      var3.setItem(
         45,
         MenuItems.named(
            Material.BOOK,
            "&e&lLayout Editor",
            List.of(
               var1 == null ? "&7Click a submenu item to select it" : "&aSelected: &f" + AdminHomeLayout.label(var1),
               "&7Then click another slot to move or swap it",
               "&8Only submenu-launch items are movable"
            )
         )
      );
      var3.setItem(49, MenuItems.named(Material.ARROW, "&e&lBack", List.of("&7Return to HubPilot Admin")));
      return var3;
   }
}
