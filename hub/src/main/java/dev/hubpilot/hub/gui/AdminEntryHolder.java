package dev.hubpilot.hub.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class AdminEntryHolder implements InventoryHolder {
   private final String destinationId;
   private Inventory inventory;

   public AdminEntryHolder(String var1) {
      this.destinationId = var1;
   }

   public String destinationId() {
      return this.destinationId;
   }

   public void setInventory(Inventory var1) {
      this.inventory = var1;
   }

   public Inventory getInventory() {
      return this.inventory;
   }
}
