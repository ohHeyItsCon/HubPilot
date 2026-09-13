package dev.hubpilot.hub.adminui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class AdminHomeHolder implements InventoryHolder {
   private Inventory inventory;

   public void setInventory(Inventory var1) {
      this.inventory = var1;
   }

   public Inventory getInventory() {
      return this.inventory;
   }
}
