package dev.hubpilot.hub.bridge;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class HubPilotMenuHolder implements InventoryHolder {
   private final String destinationId;
   private final boolean global;
   private Inventory inventory;

   public HubPilotMenuHolder(String var1, boolean var2) {
      this.destinationId = var1;
      this.global = var2;
   }

   public String destinationId() {
      return this.destinationId;
   }

   public boolean global() {
      return this.global;
   }

   public void setInventory(Inventory var1) {
      this.inventory = var1;
   }

   public Inventory getInventory() {
      return this.inventory;
   }
}
