package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.bridge.HubPilotMenuBuilder;
import dev.hubpilot.hub.gui.AdminMenuBuilder;
import dev.hubpilot.hub.publicapi.PublicHubBootstrap;
import java.io.IOException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public final class AdminHomeListener implements Listener {
   private final HubPilotHubPlugin plugin;

   public AdminHomeListener(HubPilotHubPlugin var1) {
      this.plugin = var1;
      AdminNavigation.bind(var1);
   }

   @EventHandler
   public void onClick(InventoryClickEvent var1) {
      InventoryHolder var2 = var1.getInventory().getHolder();
      if (var2 instanceof AdminHomeLayoutEditorHolder var9) {
         this.handleLayout(var1, var9);
      } else if (var2 instanceof AdminHomeHolder) {
         var1.setCancelled(true);
         if (var1.getWhoClicked() instanceof Player var4 && PublicHubBootstrap.canAdmin(var4)) {
            int var5 = var1.getRawSlot();
            if (var5 >= 0 && var5 < var1.getInventory().getSize()) {
               if (var5 == 45) {
                  var4.openInventory(AdminHomeLayoutEditorBuilder.build(this.plugin, null));
               } else if (var5 == 49) {
                  var4.closeInventory();
               } else {
                  String var6 = AdminHomeLayout.idAtSlot(this.plugin, var5);
                  if (var6 != null) {
                     switch (var6) {
                        case "servers":
                           var4.openInventory(AdminMenuBuilder.build(this.plugin, 0));
                           break;
                        case "navigator":
                           var4.openInventory(NavigatorMenuBuilder.build(this.plugin));
                           break;
                        case "automation":
                           var4.openInventory(HubPilotMenuBuilder.buildGlobal(this.plugin));
                           break;
                        case "theme":
                           var4.openInventory(ThemeMenuBuilder.build(this.plugin));
                           break;
                        case "telemetry":
                           var4.openInventory(TelemetryMenuBuilder.buildGlobal(this.plugin));
                           break;
                        case "diagnostics":
                           var4.openInventory(DiagnosticsMenuBuilder.build(this.plugin));
                           break;
                        case "help":
                           var4.openInventory(HelpMenuBuilder.build());
                           break;
                        case "staff":
                           PublicHubBootstrap.openStaff(var4);
                           break;
                        case "setup":
                           PublicHubBootstrap.openSetup(var4);
                     }
                  }
               }
            }
         }
      }
   }

   private void handleLayout(InventoryClickEvent var1, AdminHomeLayoutEditorHolder var2) {
      var1.setCancelled(true);
      if (var1.getWhoClicked() instanceof Player var4 && PublicHubBootstrap.canAdmin(var4)) {
         int var5 = var1.getRawSlot();
         if (var5 >= 0 && var5 < var1.getInventory().getSize()) {
            if (var5 == 49) {
               var4.openInventory(AdminHomeBuilder.build(this.plugin));
            } else if (!AdminHomeLayout.allowed(var5)) {
               if (var5 != 45) {
                  var4.sendMessage("§cThat Admin Home slot is fixed and cannot be moved.");
               }
            } else {
               String var6 = var2.selectedId();
               String var7 = AdminHomeLayout.idAtSlot(this.plugin, var5);
               if (var6 == null) {
                  if (var7 == null) {
                     var4.sendMessage("§eSelect a submenu item first, then click the slot where you want it.");
                  } else {
                     var4.openInventory(AdminHomeLayoutEditorBuilder.build(this.plugin, var7));
                  }
               } else if (var6.equals(var7)) {
                  var4.openInventory(AdminHomeLayoutEditorBuilder.build(this.plugin, null));
               } else {
                  try {
                     AdminHomeLayout.AssignmentResult var8 = AdminHomeLayout.assign(this.plugin, var6, var5);
                     var4.sendMessage((var8.success() ? "§a" : "§c") + var8.message());
                     var4.openInventory(AdminHomeLayoutEditorBuilder.build(this.plugin, null));
                  } catch (IOException var9) {
                     var4.sendMessage("§cCould not save the Admin Home layout: " + var9.getMessage());
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onDrag(InventoryDragEvent var1) {
      InventoryHolder var2 = var1.getInventory().getHolder();
      if (var2 instanceof AdminHomeHolder || var2 instanceof AdminHomeLayoutEditorHolder) {
         var1.setCancelled(true);
      }
   }
}
