package dev.hubpilot.hub.gui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.adminui.AdminNavigation;
import dev.hubpilot.hub.config.Destination;
import dev.hubpilot.hub.util.IconMaterials;
import dev.hubpilot.hub.util.MenuItems;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class AdminMenuBuilder {
   private AdminMenuBuilder() {
   }

   public static Inventory build(HubPilotHubPlugin var0, int var1) {
      List var2 = var0.getDestinationStore().all();
      int var3 = Math.max(1, (var2.size() + MenuSlots.ENTRY_SLOTS.length - 1) / MenuSlots.ENTRY_SLOTS.length);
      int var4 = Math.max(0, Math.min(var1, var3 - 1));
      AdminMenuHolder var5 = new AdminMenuHolder(var4);
      Inventory var6 = Bukkit.createInventory(var5, 54, MenuItems.colorize("&4&lHubPilot Admin &7(" + (var4 + 1) + "/" + var3 + ")"));
      var5.setInventory(var6);
      GuiCommon.fill(var6, Material.RED_STAINED_GLASS_PANE);
      int var7 = var4 * MenuSlots.ENTRY_SLOTS.length;

      for (int var8 = 0; var8 < MenuSlots.ENTRY_SLOTS.length; var8++) {
         int var9 = var7 + var8;
         if (var9 >= var2.size()) {
            break;
         }

         var6.setItem(MenuSlots.ENTRY_SLOTS[var8], entryItem((Destination)var2.get(var9), var9));
      }

      var6.setItem(45, AdminNavigation.serverEditorBackOrPrevious(var4));
      var6.setItem(47, MenuItems.named(Material.LIME_DYE, "&a&lAdd Destination", List.of("&7Starts an in-game setup wizard")));
      var6.setItem(49, MenuItems.named(Material.COMPASS, "&b&lOpen Player Menu", List.of("&7Preview the current server menu")));
      var6.setItem(
         51,
         MenuItems.named(
            Material.BOOK,
            "&e&lShared Data",
            List.of("&7File: &f" + var0.getDestinationStore().file().getFileName(), "&7Changes are picked up automatically", "&7by both Velocity menus")
         )
      );
      if (var4 + 1 < var3) {
         var6.setItem(53, GuiCommon.next());
      }

      return var6;
   }

   private static ItemStack entryItem(Destination var0, int var1) {
      ArrayList var2 = new ArrayList();
      var2.add("&7" + var0.description());
      var2.add("");
      var2.add("&7Software: &f" + var0.software());
      var2.add("&7Join icon: &f" + var0.iconMaterial());
      var2.add("&7Type: &f" + var0.targetType());
      var2.add("&7Target: &f" + var0.target());
      var2.add("&7Status target: &f" + var0.statusTarget());
      var2.add("&7Visible: " + (var0.enabled() ? "&aYes" : "&cNo"));
      var2.add("");
      var2.add("&eLeft-click to edit");
      var2.add("&bShift-left: move earlier");
      var2.add("&bShift-right: move later");
      return MenuItems.named(
         var0.enabled() ? IconMaterials.resolve(var0.iconMaterial()) : Material.BARRIER, (var0.enabled() ? "&a&l" : "&c&l") + var0.label(), var2
      );
   }
}
