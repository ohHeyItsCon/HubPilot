package dev.hubpilot.hub.gui;

import dev.hubpilot.hub.util.MenuItems;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GuiCommon {
   private GuiCommon() {
   }

   public static void fill(Inventory var0, Material var1) {
      ItemStack var2 = new ItemStack(var1);
      ItemMeta var3 = var2.getItemMeta();
      var3.setDisplayName(" ");
      var2.setItemMeta(var3);

      for (int var4 = 0; var4 < var0.getSize(); var4++) {
         var0.setItem(var4, var2);
      }
   }

   public static ItemStack previous() {
      return MenuItems.named(Material.ARROW, "&e&lPrevious Page", List.of("&7Go back one page"));
   }

   public static ItemStack next() {
      return MenuItems.named(Material.ARROW, "&e&lNext Page", List.of("&7Go forward one page"));
   }
}
