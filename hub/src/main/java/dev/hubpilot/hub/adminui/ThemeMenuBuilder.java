package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.gui.GuiCommon;
import dev.hubpilot.hub.util.MenuItems;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

public final class ThemeMenuBuilder {
   private ThemeMenuBuilder() {
   }

   public static Inventory build(HubPilotHubPlugin var0) {
      ThemeMenuHolder var1 = new ThemeMenuHolder();
      Inventory var2 = Bukkit.createInventory(var1, 36, MenuItems.colorize("&8Menu & Theme"));
      var1.setInventory(var2);
      GuiCommon.fill(var2, Material.BLACK_STAINED_GLASS_PANE);
      var2.setItem(
         10,
         MenuItems.named(
            Material.NAME_TAG,
            "&e&lGUI Title",
            List.of("&7Current: &f" + var0.getConfig().getString("gui-title", "&8&lSelect a Destination"), "&eClick to type")
         )
      );
      var2.setItem(
         11,
         MenuItems.named(
            var0.getMenuConfig().fillerMaterial(),
            "&e&lFiller Material",
            List.of("&7Current: &f" + var0.getMenuConfig().fillerMaterial().name(), "&eLeft/right-click to cycle")
         )
      );
      var2.setItem(
         12,
         MenuItems.named(
            Material.COMPASS, "&e&lNavigator Name", List.of("&7Current: &f" + var0.getConfig().getString("compass.name", "&b&lServer Menu"), "&eClick to type")
         )
      );
      var2.setItem(
         13,
         MenuItems.named(
            Material.REDSTONE_TORCH,
            "&e&lCustom Model Data",
            List.of("&7Current: &f" + var0.getConfig().getInt("compass.custom-model-data", 0), "&eLeft/right-click to change by 1", "&bShift-click resets to 0")
         )
      );
      var2.setItem(
         14,
         MenuItems.named(
            Material.BLAZE_ROD,
            "&e&lEditor Item Name",
            List.of("&7Current: &f" + var0.getConfig().getString("admin-item.name", "&c&lHubPilot Editor"), "&eClick to type")
         )
      );
      var2.setItem(22, MenuItems.named(Material.EMERALD, "&a&lReload & Apply", List.of("&7Reload Paper Hub configuration", "&eClick")));
      var2.setItem(27, MenuItems.named(Material.ARROW, "&e&lBack", List.of()));
      return var2;
   }
}
