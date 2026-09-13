package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.config.Destination;
import dev.hubpilot.hub.gui.AdminEntryBuilder;
import dev.hubpilot.hub.gui.AdminEntryHolder;
import dev.hubpilot.hub.gui.LayoutEditorBuilder;
import dev.hubpilot.hub.gui.LayoutEditorHolder;
import dev.hubpilot.hub.layout.NavigatorLayoutStore;
import dev.hubpilot.hub.publicapi.PublicHubBootstrap;
import java.io.IOException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class TelemetryMenuListener implements Listener {
   private final HubPilotHubPlugin plugin;

   public TelemetryMenuListener(HubPilotHubPlugin var1) {
      this.plugin = var1;
   }

   @EventHandler
   public void click(InventoryClickEvent var1) {
      InventoryHolder var2 = var1.getInventory().getHolder();
      if (var2 instanceof LayoutEditorHolder var10) {
         this.handleLayout(var1, var10);
      } else if (var2 instanceof AdminEntryHolder var3 && var1.getRawSlot() == 23) {
         var1.setCancelled(true);
         if (var1.getWhoClicked() instanceof Player var11 && PublicHubBootstrap.canAdmin(var11)) {
            if (var1.getClickedInventory() != null) {
               Inventory var15 = var1.getClickedInventory();
               var1.getInventory();
               if (var15 == var15) {
                  Destination var14 = this.plugin.getDestinationStore().find(var3.destinationId());
                  if (var14 != null) {
                     var11.openInventory(LayoutEditorBuilder.build(this.plugin, var14));
                  }

                  return;
               }
            }
         }
      } else if (var2 instanceof TelemetryMenuHolder var9) {
         var1.setCancelled(true);
         if (var1.getWhoClicked() instanceof Player var4 && PublicHubBootstrap.canAdmin(var4)) {
            Inventory var10000 = var1.getClickedInventory();
            var1.getInventory();
            if (var10000 == var10000) {
               AdminSettingsStore var12 = HubPilotAdminRuntime.settings(this.plugin);
               String var6 = TelemetryMenuBuilder.fieldForSlot(var1.getRawSlot());

               try {
                  if (var6 != null) {
                     if (var9.global()) {
                        var12.setGlobalTelemetry(var6, !var12.globalTelemetry(var6));
                     } else {
                        var12.cycleTelemetry(var9.serverId(), var6);
                     }

                     this.reopen(var4, var9);
                     return;
                  }

                  if (var1.getRawSlot() == 49 && !var9.global()) {
                     if (!var1.isShiftClick()) {
                        var4.sendMessage("§eShift-click to clear all card overrides.");
                        return;
                     }

                     var12.clearTelemetryOverrides(var9.serverId());
                     this.reopen(var4, var9);
                     return;
                  }

                  if (var1.getRawSlot() == 53) {
                     if (var9.global()) {
                        var4.openInventory(AdminHomeBuilder.build(this.plugin));
                     } else {
                        Destination var7 = this.plugin.getDestinationStore().find(var9.serverId());
                        if (var7 != null) {
                           var4.openInventory(AdminEntryBuilder.build(var7));
                        }
                     }
                  }
               } catch (IOException var8) {
                  var4.sendMessage("§cCould not save telemetry settings: " + var8.getMessage());
               }
            }
         }
      }
   }

   private void handleLayout(InventoryClickEvent var1, LayoutEditorHolder var2) {
      var1.setCancelled(true);
      if (var1.getWhoClicked() instanceof Player var3 && PublicHubBootstrap.canAdmin(var3)) {
         if (var1.getClickedInventory() != null) {
            Inventory var10000 = var1.getClickedInventory();
            var1.getInventory();
            if (var10000 == var10000) {
               Destination var7 = this.plugin.getDestinationStore().find(var2.destinationId());
               if (var7 == null) {
                  var3.sendMessage("§cThat destination no longer exists.");
                  return;
               }

               try {
                  NavigatorLayoutStore.AssignmentResult var5 = NavigatorLayoutStore.assign(this.plugin, var7.id(), var1.getRawSlot());
                  var3.sendMessage((var5.success() ? "§a" : "§c") + var5.message());
                  if (var5.success()) {
                     var3.openInventory(AdminEntryBuilder.build(var7));
                  }
               } catch (IOException var6) {
                  var3.sendMessage("§cCould not save Navigator layout: " + var6.getMessage());
               }

               return;
            }
         }
      }
   }

   private void reopen(Player var1, TelemetryMenuHolder var2) {
      if (var2.global()) {
         var1.openInventory(TelemetryMenuBuilder.buildGlobal(this.plugin));
      } else {
         Destination var3 = this.plugin.getDestinationStore().find(var2.serverId());
         if (var3 != null) {
            var1.openInventory(TelemetryMenuBuilder.buildServer(this.plugin, var3));
         }
      }
   }

   @EventHandler
   public void drag(InventoryDragEvent var1) {
      if (var1.getInventory().getHolder() instanceof LayoutEditorHolder) {
         var1.setCancelled(true);
      }
   }
}
