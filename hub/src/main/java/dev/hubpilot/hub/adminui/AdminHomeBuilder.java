package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.gui.GuiCommon;
import dev.hubpilot.hub.util.MenuItems;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

public final class AdminHomeBuilder {
   private AdminHomeBuilder() {
   }

   public static Inventory build(HubPilotHubPlugin var0) {
      AdminHomeHolder var1 = new AdminHomeHolder();
      Inventory var2 = Bukkit.createInventory(var1, 54, MenuItems.colorize("&8&lHubPilot Admin"));
      var1.setInventory(var2);
      GuiCommon.fill(var2, Material.BLACK_STAINED_GLASS_PANE);
      Map var3 = AdminHomeLayout.assignments(var0);

      for (String var5 : AdminHomeLayout.ids()) {
         Integer var6 = (Integer)var3.get(var5);
         if (var6 != null) {
            var2.setItem(var6, AdminHomeLayout.item(var5, false));
         }
      }

      var2.setItem(34, MenuItems.named(Material.NETHER_STAR, "&b&lAbout HubPilot", List.of("&7HubPilot Hub 1.0.2", "&7Velocity lifecycle core is separate")));
      var2.setItem(
         45,
         MenuItems.named(Material.HOPPER, "&e&lEdit Admin Layout", List.of("&7Move submenu buttons to any available slot", "&8This control cannot be moved"))
      );
      var2.setItem(49, MenuItems.named(Material.BARRIER, "&cClose", List.of("&7Close this menu")));
      return var2;
   }
}
