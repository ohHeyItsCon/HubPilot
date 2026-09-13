package dev.hubpilot.hub.adminui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class DiagnosticsMenuHolder implements InventoryHolder {
   private Inventory i;

   public void setInventory(Inventory var1) {
      this.i = var1;
   }

   public Inventory getInventory() {
      return this.i;
   }
}
