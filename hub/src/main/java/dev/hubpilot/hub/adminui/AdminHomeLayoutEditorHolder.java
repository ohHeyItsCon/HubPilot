package dev.hubpilot.hub.adminui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class AdminHomeLayoutEditorHolder implements InventoryHolder {
   private final String selectedId;
   private Inventory inventory;

   public AdminHomeLayoutEditorHolder(String var1) {
      this.selectedId = var1;
   }

   public String selectedId() {
      return this.selectedId;
   }

   public void setInventory(Inventory var1) {
      this.inventory = var1;
   }

   public Inventory getInventory() {
      return this.inventory;
   }
}
