package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.gui.GuiCommon;
import dev.hubpilot.hub.util.MenuItems;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

public final class NavigatorMenuBuilder {
   private NavigatorMenuBuilder() {
   }

   public static Inventory build(HubPilotHubPlugin var0) {
      AdminSettingsStore var1 = HubPilotAdminRuntime.settings(var0);
      NavigatorMenuHolder var2 = new NavigatorMenuHolder();
      Inventory var3 = Bukkit.createInventory(var2, 54, MenuItems.colorize("&8Navigator Item"));
      var2.setInventory(var3);
      GuiCommon.fill(var3, Material.BLACK_STAINED_GLASS_PANE);
      bool(
         var3,
         10,
         Material.COMPASS,
         "Force Compass On Join",
         var1.getBoolean("navigator.force-on-join", false),
         "Automatically place the navigator in the configured hotbar slot"
      );
      var3.setItem(
         11,
         MenuItems.named(
            Material.HOPPER, "&e&lHotbar Slot", List.of("&7Current: &f" + var1.getInt("navigator.hotbar-slot", 1), "&eLeft/right-click to cycle 1-9")
         )
      );
      bool(var3, 12, Material.IRON_BARS, "Lock To Slot", var1.getBoolean("navigator.lock-to-slot", true), "Prevents moving the compass out of its slot");
      bool(var3, 13, Material.FEATHER, "Prevent Dropping", var1.getBoolean("navigator.prevent-drop", true), "Blocks Q/drop for the navigator");
      bool(var3, 14, Material.CHEST, "Prevent Containers", var1.getBoolean("navigator.prevent-containers", true), "Blocks storing the navigator in containers");
      bool(var3, 15, Material.PISTON, "Prevent Moving", var1.getBoolean("navigator.prevent-moving", true), "Blocks inventory rearranging of the navigator");
      bool(
         var3,
         19,
         Material.TOTEM_OF_UNDYING,
         "Restore After Death",
         var1.getBoolean("navigator.restore-after-death", true),
         "Restores the navigator after respawn"
      );
      bool(
         var3,
         20,
         Material.RECOVERY_COMPASS,
         "Restore If Missing",
         var1.getBoolean("navigator.restore-if-missing", true),
         "Periodically repairs a missing navigator"
      );
      bool(
         var3,
         21,
         Material.GRASS_BLOCK,
         "Hub World Only",
         var1.getBoolean("navigator.hub-world-only", false),
         "Only enforce the navigator in the configured hub world"
      );
      var3.setItem(
         22,
         MenuItems.named(
            Material.MAP, "&e&lHub World", List.of("&7Current: &f" + var1.get("navigator.hub-world", "world"), "&eClick to use your current world")
         )
      );
      bool(
         var3,
         23,
         Material.TRIPWIRE_HOOK,
         "Require Permission",
         var1.getBoolean("navigator.require-permission", false),
         "Uses permission: " + var1.get("navigator.permission", "hubpilot.navigator")
      );
      var3.setItem(31, MenuItems.named(Material.LIME_DYE, "&a&lGive / Repair Now", List.of("&7Place the navigator using current settings", "&eClick")));
      var3.setItem(45, MenuItems.named(Material.ARROW, "&e&lBack", List.of()));
      var3.setItem(
         49,
         MenuItems.named(
            Material.BOOK,
            "&bHow it works",
            List.of(
               "&7Occupied target slots are moved to a free slot",
               "&7HubPilot will never intentionally delete that item",
               "&7If inventory is full, enforcement is skipped"
            )
         )
      );
      return var3;
   }

   private static void bool(Inventory var0, int var1, Material var2, String var3, boolean var4, String var5) {
      var0.setItem(
         var1,
         MenuItems.named(
            var4 ? Material.LIME_DYE : Material.GRAY_DYE, "&e&l" + var3, List.of("&7Current: " + (var4 ? "&aON" : "&cOFF"), "&7" + var5, "&eClick to toggle")
         )
      );
   }
}
