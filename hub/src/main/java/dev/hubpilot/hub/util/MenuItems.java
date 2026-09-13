package dev.hubpilot.hub.util;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.config.MenuConfig;
import dev.hubpilot.hub.publicapi.SetupItemManager;
import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class MenuItems {
   private static final String MENU_KEY = "menu_opener";
   private static final String ADMIN_KEY = "admin_opener";

   private MenuItems() {
   }

   public static ItemStack createMenuOpener(HubPilotHubPlugin var0, MenuConfig var1) {
      ItemStack var2 = new ItemStack(Material.COMPASS);
      applyMeta(var2, var1.compassName(), var1.compassLore());
      ItemMeta var3 = var2.getItemMeta();
      if (var1.compassCustomModelData() != null) {
         var3.setCustomModelData(var1.compassCustomModelData());
      }

      var3.getPersistentDataContainer().set(new NamespacedKey(var0, "menu_opener"), PersistentDataType.BYTE, (byte)1);
      var2.setItemMeta(var3);
      return var2;
   }

   public static ItemStack createAdminOpener(HubPilotHubPlugin var0, MenuConfig var1) {
      ItemStack var2 = new ItemStack(Material.NETHER_STAR);
      applyMeta(var2, var1.adminItemName(), var1.adminItemLore());
      ItemMeta var3 = var2.getItemMeta();
      var3.getPersistentDataContainer().set(new NamespacedKey(var0, "admin_opener"), PersistentDataType.BYTE, (byte)1);
      var2.setItemMeta(var3);
      return var2;
   }

   public static boolean isMenuOpener(HubPilotHubPlugin var0, ItemStack var1) {
      return hasTag(var0, var1, "menu_opener", Material.COMPASS);
   }

   public static boolean isAdminOpener(HubPilotHubPlugin var0, ItemStack var1) {
      return hasTag(var0, var1, "admin_opener", Material.NETHER_STAR);
   }

   public static boolean isProtectedItem(HubPilotHubPlugin var0, ItemStack var1) {
      return SetupItemManager.isProtected(var0, var1);
   }

   private static boolean hasTag(HubPilotHubPlugin var0, ItemStack var1, String var2, Material var3) {
      if (var1 != null && var1.getType() == var3 && var1.hasItemMeta()) {
         Byte var4 = (Byte)var1.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(var0, var2), PersistentDataType.BYTE);
         return var4 != null && var4 == 1;
      } else {
         return false;
      }
   }

   private static void applyMeta(ItemStack var0, String var1, List<String> var2) {
      ItemMeta var3 = var0.getItemMeta();
      var3.setDisplayName(colorize(var1));
      ArrayList var4 = new ArrayList();

      for (String var6 : var2) {
         var4.add(colorize(var6));
      }

      var3.setLore(var4);
      var0.setItemMeta(var3);
   }

   public static String colorize(String var0) {
      return ChatColor.translateAlternateColorCodes('&', var0 == null ? "" : var0);
   }

   public static ItemStack named(Material var0, String var1, List<String> var2) {
      ItemStack var3 = new ItemStack(var0);
      ItemMeta var4 = var3.getItemMeta();
      var4.setDisplayName(colorize(var1));
      ArrayList var5 = new ArrayList();

      for (String var7 : var2) {
         var5.add(colorize(var7));
      }

      var4.setLore(var5);
      var3.setItemMeta(var4);
      return var3;
   }
}
