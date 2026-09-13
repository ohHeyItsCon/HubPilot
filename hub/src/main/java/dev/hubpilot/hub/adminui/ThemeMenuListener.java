package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.publicapi.PublicHubBootstrap;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class ThemeMenuListener implements Listener {
   private final HubPilotHubPlugin plugin;
   private static final List<Material> FILLERS = List.of(
      Material.GRAY_STAINED_GLASS_PANE,
      Material.BLACK_STAINED_GLASS_PANE,
      Material.BLUE_STAINED_GLASS_PANE,
      Material.CYAN_STAINED_GLASS_PANE,
      Material.PURPLE_STAINED_GLASS_PANE,
      Material.WHITE_STAINED_GLASS_PANE
   );

   public ThemeMenuListener(HubPilotHubPlugin var1) {
      this.plugin = var1;
   }

   @EventHandler
   public void click(InventoryClickEvent var1) {
      if (var1.getInventory().getHolder() instanceof ThemeMenuHolder) {
         var1.setCancelled(true);
         if (var1.getWhoClicked() instanceof Player var2 && PublicHubBootstrap.canAdmin(var2)) {
            Inventory var10000 = var1.getClickedInventory();
            var1.getInventory();
            if (var10000 == var10000) {
               switch (var1.getRawSlot()) {
                  case 10:
                     HubPilotAdminRuntime.prompts(this.plugin).begin(var2, UiPromptManager.Type.GUI_TITLE);
                     break;
                  case 11:
                     Material var8 = this.plugin.getMenuConfig().fillerMaterial();
                     int var4 = Math.max(0, FILLERS.indexOf(var8));
                     Material var5 = FILLERS.get(Math.floorMod(var4 + (var1.isRightClick() ? -1 : 1), FILLERS.size()));
                     this.plugin.getConfig().set("filler-material", var5.name());
                     this.plugin.saveConfig();
                     this.plugin.reloadEverything();
                     var2.openInventory(ThemeMenuBuilder.build(this.plugin));
                     break;
                  case 12:
                     HubPilotAdminRuntime.prompts(this.plugin).begin(var2, UiPromptManager.Type.NAVIGATOR_NAME);
                     break;
                  case 13:
                     int var6 = this.plugin.getConfig().getInt("compass.custom-model-data", 0);
                     if (var1.isShiftClick()) {
                        var6 = 0;
                     } else {
                        var6 = Math.max(0, var6 + (var1.isRightClick() ? -1 : 1));
                     }

                     this.plugin.getConfig().set("compass.custom-model-data", var6);
                     this.plugin.saveConfig();
                     this.plugin.reloadEverything();
                     var2.openInventory(ThemeMenuBuilder.build(this.plugin));
                     break;
                  case 14:
                     HubPilotAdminRuntime.prompts(this.plugin).begin(var2, UiPromptManager.Type.EDITOR_NAME);
                  case 15:
                  case 16:
                  case 17:
                  case 18:
                  case 19:
                  case 20:
                  case 21:
                  case 23:
                  case 24:
                  case 25:
                  case 26:
                  default:
                     break;
                  case 22:
                     this.plugin.reloadEverything();
                     var2.sendMessage("§aHubPilot Hub settings reloaded.");
                     var2.openInventory(ThemeMenuBuilder.build(this.plugin));
                     break;
                  case 27:
                     var2.openInventory(AdminHomeBuilder.build(this.plugin));
               }
            }
         }
      }
   }
}
