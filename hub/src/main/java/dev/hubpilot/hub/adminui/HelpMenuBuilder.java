package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.gui.GuiCommon;
import dev.hubpilot.hub.util.MenuItems;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

public final class HelpMenuBuilder {
   private HelpMenuBuilder() {
   }

   public static Inventory build() {
      HelpMenuHolder var0 = new HelpMenuHolder();
      Inventory var1 = Bukkit.createInventory(var0, 45, MenuItems.colorize("&8HubPilot Help"));
      var0.setInventory(var1);
      GuiCommon.fill(var1, Material.BLACK_STAINED_GLASS_PANE);
      var1.setItem(
         10,
         MenuItems.named(
            Material.COMPASS, "&bPlayer Commands", List.of("&f/hp gui open &7- server selector", "&f/hub &7or &f/lobby &7- return to hub (Velocity)")
         )
      );
      var1.setItem(
         12,
         MenuItems.named(
            Material.COMMAND_BLOCK,
            "&6Admin Commands",
            List.of(
               "&f/hp gui admin &7- admin dashboard",
               "&f/hp gui editor [player] &7- editor item",
               "&f/hp gui navigator &7- navigator settings",
               "&f/hp gui diagnostics &7- diagnostics",
               "&f/hp gui reload &7- reload Paper Hub",
               "&f/hp help &7- Velocity Core commands"
            )
         )
      );
      var1.setItem(
         14,
         MenuItems.named(
            Material.TARGET,
            "&eGUI Controls",
            List.of("&7Left click: next/toggle", "&7Right click: previous where supported", "&7Shift-click: custom/reset/confirm where shown")
         )
      );
      var1.setItem(
         16,
         MenuItems.named(
            Material.BOOK,
            "&fConfig Philosophy",
            List.of("&7Common settings live in the GUI.", "&7Secrets, database credentials and advanced", "&7provider security remain file/console-only.")
         )
      );
      var1.setItem(36, MenuItems.named(Material.ARROW, "&e&lBack", List.of()));
      return var1;
   }
}
