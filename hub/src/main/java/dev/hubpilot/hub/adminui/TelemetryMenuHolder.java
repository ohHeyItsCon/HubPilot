package dev.hubpilot.hub.adminui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class TelemetryMenuHolder implements InventoryHolder {
   private final String serverId;
   private final boolean global;
   private Inventory inventory;

   public TelemetryMenuHolder(String var1, boolean var2) {
      this.serverId = var1;
      this.global = var2;
   }

   public String serverId() {
      return this.serverId;
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
