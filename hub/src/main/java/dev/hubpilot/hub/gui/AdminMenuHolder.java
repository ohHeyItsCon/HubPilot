package dev.hubpilot.hub.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class AdminMenuHolder implements InventoryHolder {
   private final int page;
   private Inventory inventory;

   public AdminMenuHolder(int var1) {
      this.page = var1;
   }

   public int page() {
      return this.page;
   }

   public void setInventory(Inventory var1) {
      this.inventory = var1;
   }

   public Inventory getInventory() {
      return this.inventory;
   }
}
