package dev.hubpilot.hub.bridge;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.config.Destination;
import dev.hubpilot.hub.publicapi.PublicHubBootstrap;
import dev.hubpilot.hub.util.MenuItems;
import java.io.IOException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class MessageMenuListener implements Listener {
   private final HubPilotHubPlugin plugin;
   private final MessagePromptManager prompts;

   public MessageMenuListener(HubPilotHubPlugin var1, MessagePromptManager var2) {
      this.plugin = var1;
      this.prompts = var2;
   }

   @EventHandler
   public void onClick(InventoryClickEvent var1) {
      if (var1.getInventory().getHolder() instanceof MessageMenuHolder var2) {
         var1.setCancelled(true);
         if (var1.getWhoClicked() instanceof Player var8 && PublicHubBootstrap.canAdmin(var8)) {
            int var9 = var1.getRawSlot();
            if (var9 == 45) {
               Destination var10 = var2.global() ? null : this.plugin.getDestinationStore().find(var2.destinationId());
               if (var2.global()) {
                  var8.openInventory(HubPilotMenuBuilder.buildGlobal(this.plugin));
               } else if (var10 != null) {
                  var8.openInventory(HubPilotMenuBuilder.buildServer(this.plugin, var10));
               }
            } else if (var9 == 48) {
               var8.openInventory(MessageMenuBuilder.build(this.plugin, var2.destinationId(), var2.page() - 1));
            } else if (var9 == 50) {
               var8.openInventory(MessageMenuBuilder.build(this.plugin, var2.destinationId(), var2.page() + 1));
            } else {
               MessageEvent var5 = MessageMenuBuilder.eventAt(var2.page(), var9);
               if (var5 != null) {
                  try {
                     if (var1.isShiftClick() && var1.isRightClick()) {
                        this.plugin.getHubPilotStore().clearMessageOverride(var2.destinationId(), var5);
                        var8.sendMessage(var2.global() ? "§aRestored the built-in default." : "§aRestored the global message.");
                     } else {
                        if (var1.isRightClick()) {
                           this.prompts.begin(var8, var2.destinationId(), var2.page(), var5);
                           return;
                        }

                        if (var1.getClick() == ClickType.MIDDLE) {
                           String var11 = this.plugin
                              .getHubPilotStore()
                              .messageText(var2.destinationId(), var5)
                              .replace("{server}", "&f&lExample Server&r")
                              .replace("{server_plain}", "Example Server")
                              .replace("{position}", "2")
                              .replace("{queue_size}", "5")
                              .replace("{seconds}", "5")
                              .replace("{delay}", "15")
                              .replace("{attempt}", "1")
                              .replace("{max}", "3")
                              .replace("{required}", "1.21.10")
                              .replace("{current}", "1.21.10")
                              .replace("{error}", "Example error");
                           var8.sendMessage(MenuItems.colorize(var11));
                           return;
                        }

                        boolean var6 = !this.plugin.getHubPilotStore().messageEnabled(var2.destinationId(), var5);
                        this.plugin.getHubPilotStore().setMessageEnabled(var2.destinationId(), var5, var6);
                        var8.sendMessage(var6 ? "§aMessage shown." : "§eMessage hidden.");
                     }

                     var8.openInventory(MessageMenuBuilder.build(this.plugin, var2.destinationId(), var2.page()));
                  } catch (IOException var7) {
                     var8.sendMessage("§cCould not save the message setting: " + var7.getMessage());
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onDrag(InventoryDragEvent var1) {
      if (var1.getInventory().getHolder() instanceof MessageMenuHolder) {
         var1.setCancelled(true);
      }
   }
}
