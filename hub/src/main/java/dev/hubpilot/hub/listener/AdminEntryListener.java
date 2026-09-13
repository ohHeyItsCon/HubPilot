package dev.hubpilot.hub.listener;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.admin.PromptManager;
import dev.hubpilot.hub.adminui.TelemetryMenuBuilder;
import dev.hubpilot.hub.bridge.HubPilotMenuBuilder;
import dev.hubpilot.hub.config.Destination;
import dev.hubpilot.hub.gui.AdminEntryBuilder;
import dev.hubpilot.hub.gui.AdminEntryHolder;
import dev.hubpilot.hub.gui.AdminMenuBuilder;
import dev.hubpilot.hub.gui.IconPickerBuilder;
import dev.hubpilot.hub.gui.IconPickerHolder;
import dev.hubpilot.hub.publicapi.PublicHubBootstrap;
import dev.hubpilot.hub.util.IconMaterials;
import java.io.IOException;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class AdminEntryListener implements Listener {
   private final HubPilotHubPlugin plugin;

   public AdminEntryListener(HubPilotHubPlugin var1) {
      this.plugin = var1;
   }

   @EventHandler
   public void onClick(InventoryClickEvent var1) {
      Inventory var2 = var1.getInventory();
      if (var2.getHolder() instanceof IconPickerHolder var9) {
         this.handleIconPicker(var1, var9);
      } else if (var2.getHolder() instanceof AdminEntryHolder var3) {
         var1.setCancelled(true);
         if (var1.getWhoClicked() instanceof Player var5 && PublicHubBootstrap.canAdmin(var5)) {
            if (var1.getClickedInventory() != null) {
               Inventory var10000 = var1.getClickedInventory();
               if (var10000 == var10000) {
                  Destination var6 = this.plugin.getDestinationStore().find(var3.destinationId());
                  if (var6 == null) {
                     var5.sendMessage("§cThat destination no longer exists.");
                     var5.openInventory(AdminMenuBuilder.build(this.plugin, 0));
                     return;
                  }

                  try {
                     switch (var1.getRawSlot()) {
                        case 10:
                           this.plugin.getPromptManager().startFieldEdit(var5, var6, PromptManager.Field.LABEL);
                           break;
                        case 11:
                           this.plugin.getPromptManager().startFieldEdit(var5, var6, PromptManager.Field.DESCRIPTION);
                           break;
                        case 12:
                           this.plugin.getPromptManager().startFieldEdit(var5, var6, PromptManager.Field.SOFTWARE);
                           break;
                        case 13:
                           Destination.TargetType var12 = var6.targetType() == Destination.TargetType.SERVER
                              ? Destination.TargetType.WORLD
                              : Destination.TargetType.SERVER;
                           this.saveAndReopen(var5, var6.withTargetType(var12));
                           break;
                        case 14:
                           this.plugin.getPromptManager().startFieldEdit(var5, var6, PromptManager.Field.TARGET);
                           break;
                        case 15:
                           this.plugin.getPromptManager().startFieldEdit(var5, var6, PromptManager.Field.STATUS_TARGET);
                           break;
                        case 16:
                           this.saveAndReopen(var5, var6.withEnabled(!var6.enabled()));
                           break;
                        case 17:
                           if (var1.isRightClick()) {
                              ItemStack var7 = var5.getInventory().getItemInOffHand();
                              if (var7 == null || var7.getType().isAir()) {
                                 var5.sendMessage("§ePut the icon item in your off-hand first.");
                                 return;
                              }

                              this.saveAndReopen(var5, var6.withIconMaterial(IconMaterials.canonical(var7.getType().name())));
                           } else if (var1.isShiftClick()) {
                              this.plugin.getPromptManager().startFieldEdit(var5, var6, PromptManager.Field.ICON);
                           } else {
                              var5.openInventory(IconPickerBuilder.build(var6, 0));
                           }
                        case 18:
                        case 19:
                        case 23:
                        case 24:
                        case 25:
                        case 26:
                        case 28:
                        case 29:
                        case 30:
                        default:
                           break;
                        case 20:
                           var5.openInventory(TelemetryMenuBuilder.buildServer(this.plugin, var6));
                           break;
                        case 21:
                           var5.openInventory(HubPilotMenuBuilder.buildServer(this.plugin, var6));
                           break;
                        case 22:
                           this.cloneEntry(var5, var6);
                           break;
                        case 27:
                           var5.openInventory(AdminMenuBuilder.build(this.plugin, 0));
                           break;
                        case 31:
                           if (!var1.isShiftClick()) {
                              var5.sendMessage("§eShift-click the TNT to confirm deletion.");
                              return;
                           }

                           this.plugin.getDestinationStore().delete(var6.id());
                           var5.sendMessage("§cDeleted " + var6.label() + ".");
                           var5.openInventory(AdminMenuBuilder.build(this.plugin, 0));
                     }
                  } catch (IOException var8) {
                     var5.sendMessage("§cCould not save: " + var8.getMessage());
                     this.plugin.getLogger().warning("Could not update destination: " + var8.getMessage());
                  }

                  return;
               }
            }
         }
      }
   }

   private void handleIconPicker(InventoryClickEvent var1, IconPickerHolder var2) {
      var1.setCancelled(true);
      if (var1.getWhoClicked() instanceof Player var4 && PublicHubBootstrap.canAdmin(var4)) {
         if (var1.getClickedInventory() != null) {
            Inventory var10000 = var1.getClickedInventory();
            var1.getInventory();
            if (var10000 == var10000) {
               Destination var5 = this.plugin.getDestinationStore().find(var2.destinationId());
               if (var5 == null) {
                  var4.openInventory(AdminMenuBuilder.build(this.plugin, 0));
                  return;
               }

               int var6 = var1.getRawSlot();
               if (var6 == 45 && var2.page() > 0) {
                  var4.openInventory(IconPickerBuilder.build(var5, var2.page() - 1));
                  return;
               }

               if (var6 == 53) {
                  var4.openInventory(IconPickerBuilder.build(var5, var2.page() + 1));
                  return;
               }

               if (var6 == 49) {
                  var4.openInventory(AdminEntryBuilder.build(var5));
                  return;
               }

               if (var6 == 47) {
                  try {
                     this.saveAndReopen(var4, var5.withIconMaterial("GRASS_BLOCK"));
                  } catch (IOException var9) {
                     var4.sendMessage("§cCould not save icon: " + var9.getMessage());
                  }

                  return;
               }

               if (var6 == 51) {
                  this.plugin.getPromptManager().startFieldEdit(var4, var5, PromptManager.Field.ICON);
                  return;
               }

               if (var6 >= 0 && var6 < 45) {
                  ItemStack var7 = var1.getCurrentItem();
                  if (var7 != null && !var7.getType().isAir() && var7.getType() != Material.BLACK_STAINED_GLASS_PANE) {
                     try {
                        Destination var8 = var5.withIconMaterial(IconMaterials.canonical(var7.getType().name()));
                        this.plugin.getDestinationStore().replace(var8);
                        var4.sendMessage("§aIcon set to §f" + var8.iconMaterial() + "§a.");
                        var4.openInventory(AdminEntryBuilder.build(var8));
                     } catch (IOException var10) {
                        var4.sendMessage("§cCould not save icon: " + var10.getMessage());
                     }

                     return;
                  }

                  return;
               }

               return;
            }
         }
      }
   }

   private void saveAndReopen(Player var1, Destination var2) throws IOException {
      this.plugin.getDestinationStore().replace(var2);
      var1.openInventory(AdminEntryBuilder.build(var2));
   }

   private void cloneEntry(Player var1, Destination var2) throws IOException {
      String var3 = var2.id() + "-copy";
      String var4 = var3;
      int var5 = 2;

      while (this.plugin.getDestinationStore().find(var4) != null) {
         var4 = var3 + var5++;
      }

      Destination var6 = new Destination(
         var4, var2.label() + " Copy", var2.description(), var2.software(), var2.iconMaterial(), var2.targetType(), var2.target(), var2.statusTarget(), false
      );
      this.plugin.getDestinationStore().add(var6);
      this.plugin.getHubPilotStore().copyOverrides(var2.id(), var6.id());
      var1.sendMessage("§aCloned as §f" + var6.id() + "§a. It starts hidden.");
      var1.openInventory(AdminEntryBuilder.build(var6));
   }

   @EventHandler
   public void onDrag(InventoryDragEvent var1) {
      if (var1.getInventory().getHolder() instanceof AdminEntryHolder || var1.getInventory().getHolder() instanceof IconPickerHolder) {
         var1.setCancelled(true);
      }
   }
}
