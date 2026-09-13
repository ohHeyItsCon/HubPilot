package dev.hubpilot.hub.listener;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.adminui.AdminNavigation;
import dev.hubpilot.hub.config.Destination;
import dev.hubpilot.hub.gui.AdminEntryBuilder;
import dev.hubpilot.hub.gui.AdminMenuBuilder;
import dev.hubpilot.hub.gui.AdminMenuHolder;
import dev.hubpilot.hub.gui.DestinationMenuBuilder;
import dev.hubpilot.hub.gui.MenuSlots;
import dev.hubpilot.hub.publicapi.PublicHubBootstrap;
import java.io.IOException;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class AdminMenuListener implements Listener {
   private final HubPilotHubPlugin plugin;

   public AdminMenuListener(HubPilotHubPlugin var1) {
      this.plugin = var1;
   }

   @EventHandler
   public void onClick(InventoryClickEvent var1) {
      if (var1.getInventory().getHolder() instanceof AdminMenuHolder var2) {
         var1.setCancelled(true);
         if (var1.getWhoClicked() instanceof Player var11) {
            if (PublicHubBootstrap.canAdmin(var11)) {
               if (var1.getClickedInventory() != null) {
                  Inventory var10000 = var1.getClickedInventory();
                  var1.getInventory();
                  if (var10000 == var10000) {
                     int var12 = var1.getRawSlot();
                     if (var12 == 45) {
                        var11.openInventory(AdminNavigation.serverEditorPrevious(this.plugin, var2.page() - 1));
                        return;
                     }

                     if (var12 == 53) {
                        var11.openInventory(AdminMenuBuilder.build(this.plugin, var2.page() + 1));
                        return;
                     }

                     if (var12 == 47) {
                        this.plugin.getPromptManager().startAdd(var11);
                        return;
                     }

                     if (var12 == 49) {
                        var11.openInventory(DestinationMenuBuilder.build(this.plugin, 0));
                        return;
                     }

                     int var5 = -1;

                     for (int var6 = 0; var6 < MenuSlots.ENTRY_SLOTS.length; var6++) {
                        if (MenuSlots.ENTRY_SLOTS[var6] == var12) {
                           var5 = var6;
                        }
                     }

                     if (var5 < 0) {
                        return;
                     }

                     List var13 = this.plugin.getDestinationStore().all();
                     int var7 = var2.page() * MenuSlots.ENTRY_SLOTS.length + var5;
                     if (var7 >= var13.size()) {
                        return;
                     }

                     Destination var8 = (Destination)var13.get(var7);

                     try {
                        if (var1.isShiftClick() && var1.isLeftClick()) {
                           this.plugin.getDestinationStore().move(var8.id(), -1);
                           var11.openInventory(AdminMenuBuilder.build(this.plugin, var2.page()));
                        } else if (var1.isShiftClick() && var1.isRightClick()) {
                           this.plugin.getDestinationStore().move(var8.id(), 1);
                           var11.openInventory(AdminMenuBuilder.build(this.plugin, var2.page()));
                        } else {
                           var11.openInventory(AdminEntryBuilder.build(var8));
                        }
                     } catch (IOException var10) {
                        var11.sendMessage(ChatColor.RED + "Could not save the shared destination file.");
                        this.plugin.getLogger().warning("Could not reorder destinations: " + var10.getMessage());
                     }

                     return;
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onDrag(InventoryDragEvent var1) {
      if (var1.getInventory().getHolder() instanceof AdminMenuHolder) {
         var1.setCancelled(true);
      }
   }
}
