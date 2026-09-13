package dev.hubpilot.hub.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class IconPickerHolder implements InventoryHolder {
   private final String destinationId;
   private final int page;
   private Inventory inventory;

   public IconPickerHolder(String var1, int var2) {
      this.destinationId = var1;
      this.page = var2;
   }

   public String destinationId() {
      return this.destinationId;
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
