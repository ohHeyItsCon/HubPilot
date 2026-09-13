package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.gui.AdminMenuBuilder;
import dev.hubpilot.hub.publicapi.PublicHubBootstrap;
import dev.hubpilot.hub.util.MenuItems;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class AdminNavigation {
   private static volatile HubPilotHubPlugin plugin;
   private static final Map<UUID, String> STAFF_PARENT = new ConcurrentHashMap<>();

   private AdminNavigation() {
   }

   public static void bind(HubPilotHubPlugin var0) {
      plugin = var0;
   }

   public static void markStaffAdmin(Player var0) {
      if (var0 != null) {
         STAFF_PARENT.put(var0.getUniqueId(), "admin");
      }
   }

   public static void openStaffFromSetup(PublicHubBootstrap var0, Player var1) {
      PublicHubBootstrap.openStaff(var1);
      if (var1 != null) {
         STAFF_PARENT.put(var1.getUniqueId(), "setup");
      }
   }

   public static void backFromStaff(PublicHubBootstrap var0, Player var1) {
      String var2 = var1 == null ? null : STAFF_PARENT.remove(var1.getUniqueId());
      if ("setup".equals(var2)) {
         PublicHubBootstrap.openSetup(var1);
      } else {
         HubPilotHubPlugin var3 = plugin;
         if (var3 != null && var1 != null) {
            var1.openInventory(AdminHomeBuilder.build(var3));
         }
      }
   }

   public static Inventory globalBack(HubPilotHubPlugin var0, int var1) {
      return AdminHomeBuilder.build(var0);
   }

   public static Inventory serverEditorPrevious(HubPilotHubPlugin var0, int var1) {
      return var1 < 0 ? AdminHomeBuilder.build(var0) : AdminMenuBuilder.build(var0, var1);
   }

   public static ItemStack serverEditorBackOrPrevious(int var0) {
      return var0 <= 0
         ? MenuItems.named(Material.ARROW, "&e&lBack", List.of("&7Return to HubPilot Admin"))
         : MenuItems.named(Material.ARROW, "&e&lPrevious Page", List.of("&7Go back one server page"));
   }
}
