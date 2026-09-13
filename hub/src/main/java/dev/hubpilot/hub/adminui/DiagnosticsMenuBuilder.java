package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.gui.GuiCommon;
import dev.hubpilot.hub.util.MenuItems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

public final class DiagnosticsMenuBuilder {
   private DiagnosticsMenuBuilder() {
   }

   public static Inventory build(HubPilotHubPlugin var0) {
      DiagnosticsMenuHolder var1 = new DiagnosticsMenuHolder();
      Inventory var2 = Bukkit.createInventory(var1, 45, MenuItems.colorize("&8HubPilot Diagnostics"));
      var1.setInventory(var2);
      GuiCommon.fill(var2, Material.BLACK_STAINED_GLASS_PANE);
      Path var3 = var0.getDestinationStore().file();
      Path var4 = var0.getHubPilotStore().file();
      Path var5 = HubPilotAdminRuntime.settings(var0).file();
      status(var2, 10, "Destination Storage", Files.isWritable(var3.getParent()), var3.toString());
      status(var2, 11, "Automation Settings", Files.isWritable(var4.getParent()), var4.toString());
      status(var2, 12, "Admin UI Settings", Files.isWritable(var5.getParent()), var5.toString());
      int var6 = var0.getDestinationStore().all().size();
      int var7 = var0.getDestinationStore().enabled().size();
      var2.setItem(14, MenuItems.named(Material.CHEST, "&e&lDestinations", List.of("&7Configured: &f" + var6, "&7Visible: &f" + var7)));
      boolean var8 = !var3.toString().contains("plugins/HubPilot/shared") && !var3.toString().contains("plugins\\HubPilot\\shared");
      status(var2, 16, "Shared Directory", var8, var3.getParent().toString());
      var2.setItem(
         29,
         MenuItems.named(
            Material.REDSTONE_TORCH,
            "&e&lCore Bridge",
            List.of("&7Requests use channel: &fhubpilot:control", "&7Use &f/hp doctor &7on the proxy for Core diagnostics")
         )
      );
      var2.setItem(31, MenuItems.named(Material.EMERALD, "&a&lReload Paper Hub", List.of("&eClick to reload stores and config")));
      var2.setItem(36, MenuItems.named(Material.ARROW, "&e&lBack", List.of()));
      return var2;
   }

   private static void status(Inventory var0, int var1, String var2, boolean var3, String var4) {
      var0.setItem(var1, MenuItems.named(var3 ? Material.LIME_DYE : Material.RED_DYE, "&e&l" + var2, List.of(var3 ? "&aOK" : "&cNeeds attention", "&7" + var4)));
   }
}
