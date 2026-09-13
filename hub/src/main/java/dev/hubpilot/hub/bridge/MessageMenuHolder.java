package dev.hubpilot.hub.bridge;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class MessageMenuHolder implements InventoryHolder {
   private final String destinationId;
   private final int page;
   private Inventory inventory;

   MessageMenuHolder(String var1, int var2) {
      this.destinationId = var1;
      this.page = var2;
   }

   public String destinationId() {
      return this.destinationId;
   }

   public boolean global() {
      return this.destinationId == null;
   }

   public int page() {
      return this.page;
   }

   void setInventory(Inventory var1) {
      this.inventory = var1;
   }

   public Inventory getInventory() {
      return this.inventory;
   }
}
