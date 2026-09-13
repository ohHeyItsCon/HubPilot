package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.publicapi.PublicHubBootstrap;
import java.io.IOException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class NavigatorMenuListener implements Listener {
   private final HubPilotHubPlugin plugin;

   public NavigatorMenuListener(HubPilotHubPlugin var1) {
      this.plugin = var1;
   }

   @EventHandler
   public void click(InventoryClickEvent var1) {
      if (var1.getInventory().getHolder() instanceof NavigatorMenuHolder) {
         var1.setCancelled(true);
         if (var1.getWhoClicked() instanceof Player var2 && PublicHubBootstrap.canAdmin(var2)) {
            Inventory var10000 = var1.getClickedInventory();
            var1.getInventory();
            if (var10000 == var10000) {
               AdminSettingsStore var6 = HubPilotAdminRuntime.settings(this.plugin);

               try {
                  switch (var1.getRawSlot()) {
                     case 10:
                        this.toggle(var6, "navigator.force-on-join", false);
                        break;
                     case 11:
                        int var4 = var6.getInt("navigator.hotbar-slot", 1);
                        var4 = Math.floorMod(var4 - 1 + (var1.isRightClick() ? -1 : 1), 9) + 1;
                        var6.setInt("navigator.hotbar-slot", var4);
                        break;
                     case 12:
                        this.toggle(var6, "navigator.lock-to-slot", true);
                        break;
                     case 13:
                        this.toggle(var6, "navigator.prevent-drop", true);
                        break;
                     case 14:
                        this.toggle(var6, "navigator.prevent-containers", true);
                        break;
                     case 15:
                        this.toggle(var6, "navigator.prevent-moving", true);
                        break;
                     case 16:
                     case 17:
                     case 18:
                     case 24:
                     case 25:
                     case 26:
                     case 27:
                     case 28:
                     case 29:
                     case 30:
                     case 32:
                     case 33:
                     case 34:
                     case 35:
                     case 36:
                     case 37:
                     case 38:
                     case 39:
                     case 40:
                     case 41:
                     case 42:
                     case 43:
                     case 44:
                     default:
                        return;
                     case 19:
                        this.toggle(var6, "navigator.restore-after-death", true);
                        break;
                     case 20:
                        this.toggle(var6, "navigator.restore-if-missing", true);
                        break;
                     case 21:
                        this.toggle(var6, "navigator.hub-world-only", false);
                        break;
                     case 22:
                        var6.set("navigator.hub-world", var2.getWorld().getName());
                        break;
                     case 23:
                        this.toggle(var6, "navigator.require-permission", false);
                        break;
                     case 31:
                        HubPilotAdminRuntime.navigator(this.plugin).ensure(var2, true);
                        var2.sendMessage("§aNavigator checked using current settings.");
                        break;
                     case 45:
                        var2.openInventory(AdminHomeBuilder.build(this.plugin));
                        return;
                  }

                  var2.openInventory(NavigatorMenuBuilder.build(this.plugin));
               } catch (IOException var5) {
                  var2.sendMessage("§cCould not save setting: " + var5.getMessage());
               }
            }
         }
      }
   }

   private void toggle(AdminSettingsStore var1, String var2, boolean var3) throws IOException {
      var1.setBoolean(var2, !var1.getBoolean(var2, var3));
   }
}
