package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.publicapi.SetupItemManager;
import dev.hubpilot.hub.util.MenuItems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class NavigatorManager {
   private final HubPilotHubPlugin plugin;
   private final AdminSettingsStore settings;

   NavigatorManager(HubPilotHubPlugin var1, AdminSettingsStore var2) {
      this.plugin = var1;
      this.settings = var2;
   }

   public boolean shouldEnforce(Player var1) {
      if (!this.settings.getBoolean("navigator.force-on-join", false)) {
         return false;
      } else {
         return this.settings.getBoolean("navigator.require-permission", false)
               && !var1.hasPermission(this.settings.get("navigator.permission", "hubpilot.navigator"))
            ? false
            : !this.settings.getBoolean("navigator.hub-world-only", false)
               || var1.getWorld().getName().equals(this.settings.get("navigator.hub-world", "world"));
      }
   }

   public boolean isNavigator(ItemStack var1) {
      return MenuItems.isMenuOpener(this.plugin, var1);
   }

   public boolean hasNavigator(Player var1) {
      PlayerInventory var2 = var1.getInventory();

      for (int var3 = 0; var3 < Math.min(36, var2.getSize()); var3++) {
         if (this.isNavigator(var2.getItem(var3))) {
            return true;
         }
      }

      return this.isNavigator(var2.getItemInOffHand());
   }

   public void ensureReady(Player var1, boolean var2) {
      if (var2 || this.shouldEnforce(var1)) {
         PlayerInventory var3 = var1.getInventory();
         int var4 = Math.max(0, Math.min(8, this.settings.getInt("navigator.hotbar-slot", 1) - 1));
         int var5 = this.chooseKeeper(var3, var4);
         if (var5 >= 0) {
            ItemStack var7 = var3.getItem(var5);
            this.normalize(var7);
            this.removeOtherNavigators(var3, var5);
            this.clearOffhandDuplicate(var3, var7);
            if (this.settings.getBoolean("navigator.lock-to-slot", true) && var5 != var4) {
               var3.setItem(var5, null);
               this.moveIntoTarget(var1, var3, var4, var7);
            }
         } else if (this.isNavigator(var3.getItemInOffHand())) {
            ItemStack var6 = var3.getItemInOffHand();
            this.normalize(var6);
            var3.setItemInOffHand(null);
            this.moveIntoTarget(var1, var3, var4, var6);
         } else {
            this.moveIntoTarget(var1, var3, var4, MenuItems.createMenuOpener(this.plugin, this.plugin.getMenuConfig()));
         }
      }
   }

   public void dedupe(Player var1) {
      PlayerInventory var2 = var1.getInventory();
      int var3 = Math.max(0, Math.min(8, this.settings.getInt("navigator.hotbar-slot", 1) - 1));
      int var4 = this.chooseKeeper(var2, var3);
      if (var4 >= 0) {
         ItemStack var6 = var2.getItem(var4);
         this.normalize(var6);
         this.removeOtherNavigators(var2, var4);
         this.clearOffhandDuplicate(var2, var6);
      } else {
         ItemStack var5 = var2.getItemInOffHand();
         if (this.isNavigator(var5)) {
            this.normalize(var5);
         }
      }
   }

   public void removeNavigator(Player var1) {
      PlayerInventory var2 = var1.getInventory();

      for (int var3 = 0; var3 < var2.getSize(); var3++) {
         if (this.isNavigator(var2.getItem(var3))) {
            var2.setItem(var3, null);
         }
      }

      if (this.isNavigator(var2.getItemInOffHand())) {
         var2.setItemInOffHand(null);
      }
   }

   private int chooseKeeper(PlayerInventory var1, int var2) {
      if (this.isNavigator(var1.getItem(var2))) {
         return var2;
      } else {
         for (int var3 = 0; var3 < Math.min(36, var1.getSize()); var3++) {
            if (this.isNavigator(var1.getItem(var3))) {
               return var3;
            }
         }

         return -1;
      }
   }

   private void removeOtherNavigators(PlayerInventory var1, int var2) {
      for (int var3 = 0; var3 < Math.min(36, var1.getSize()); var3++) {
         if (var3 != var2 && this.isNavigator(var1.getItem(var3))) {
            var1.setItem(var3, null);
         }
      }
   }

   private void clearOffhandDuplicate(PlayerInventory var1, ItemStack var2) {
      if (this.isNavigator(var1.getItemInOffHand()) && var1.getItemInOffHand() != var2) {
         var1.setItemInOffHand(null);
      }
   }

   private void normalize(ItemStack var1) {
      if (var1 != null && var1.getAmount() != 1) {
         var1.setAmount(1);
      }
   }

   private void moveIntoTarget(Player var1, PlayerInventory var2, int var3, ItemStack var4) {
      this.normalize(var4);
      ItemStack var5 = var2.getItem(var3);
      if (var5 != null && !var5.getType().isAir() && !this.isNavigator(var5)) {
         int var6 = var2.firstEmpty();
         if (var6 < 0) {
            var1.sendMessage("§eHubPilot could not place the navigator because your inventory is full.");
            return;
         }

         var2.setItem(var6, var5);
      }

      var2.setItem(var3, var4);
   }

   public void ensure(Player var1, boolean var2) {
      SetupItemManager.guardNavigator(this, var1, var2);
   }
}
