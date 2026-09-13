package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.config.Destination;
import dev.hubpilot.hub.gui.GuiCommon;
import dev.hubpilot.hub.util.MenuItems;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

public final class TelemetryMenuBuilder {
   private static final String[] FIELDS = new String[]{
      "description",
      "software",
      "type",
      "status",
      "players",
      "time",
      "action",
      "backend_ping",
      "startup_progress",
      "queue",
      "avg_startup",
      "tps",
      "mspt",
      "ram",
      "cpu",
      "chunks",
      "entities",
      "link",
      "history",
      "errors"
   };
   private static final String[] LABELS = new String[]{
      "Description",
      "Software / Version",
      "Destination Type",
      "Status",
      "Player Count",
      "Uptime / Last Started",
      "Join Action",
      "Backend Ping",
      "Startup Progress",
      "Queue Size",
      "Average Startup",
      "TPS",
      "MSPT",
      "Memory",
      "CPU",
      "Loaded Chunks",
      "Entities",
      "HubPilot Link",
      "Request History",
      "Last Error"
   };
   private static final Material[] ICONS = new Material[]{
      Material.WRITABLE_BOOK,
      Material.ANVIL,
      Material.ENDER_EYE,
      Material.BEACON,
      Material.PLAYER_HEAD,
      Material.CLOCK,
      Material.LIME_DYE,
      Material.COMPASS,
      Material.RECOVERY_COMPASS,
      Material.HOPPER,
      Material.CLOCK,
      Material.REDSTONE_TORCH,
      Material.REPEATER,
      Material.CHEST,
      Material.COMPARATOR,
      Material.MAP,
      Material.ZOMBIE_HEAD,
      Material.CHAIN,
      Material.BOOK,
      Material.BARRIER
   };
   private static final int[] SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33};

   private TelemetryMenuBuilder() {
   }

   public static Inventory buildGlobal(HubPilotHubPlugin var0) {
      return build(var0, null, true);
   }

   public static Inventory buildServer(HubPilotHubPlugin var0, Destination var1) {
      return build(var0, var1.id(), false);
   }

   private static Inventory build(HubPilotHubPlugin var0, String var1, boolean var2) {
      TelemetryMenuHolder var3 = new TelemetryMenuHolder(var1, var2);
      Inventory var4 = Bukkit.createInventory(var3, 54, MenuItems.colorize(var2 ? "&8Server Card Defaults" : "&8Server Card: &f" + var1));
      var3.setInventory(var4);
      GuiCommon.fill(var4, Material.BLACK_STAINED_GLASS_PANE);
      AdminSettingsStore var5 = HubPilotAdminRuntime.settings(var0);

      for (int var6 = 0; var6 < FIELDS.length; var6++) {
         String var7 = FIELDS[var6];
         boolean var8 = var2 ? var5.globalTelemetry(var7) : var5.telemetry(var1, var7);
         String var9 = var2 ? (var8 ? "ON" : "OFF") : var5.telemetrySource(var1, var7);
         var4.setItem(
            SLOTS[var6],
            MenuItems.named(
               var8 ? ICONS[var6] : Material.GRAY_DYE,
               "&e&l" + LABELS[var6],
               List.of(
                  "&7Effective: " + (var8 ? "&aSHOWN" : "&cHIDDEN"),
                  var2 ? "&7Global default" : "&7Override: &f" + var9,
                  var2 ? "&eClick to toggle" : "&eClick: inherit → on → off"
               )
            )
         );
      }

      var4.setItem(
         45,
         MenuItems.named(
            Material.SPYGLASS,
            "&b&lTelemetry Sources",
            List.of(
               "&7Core always supplies status, backend ping, queue,",
               "&7startup progress and request history.",
               "&7HubPilot Link adds TPS, MSPT, RAM, CPU,",
               "&7loaded chunks and entity counts.",
               "&8A missing Link never breaks server navigation."
            )
         )
      );
      if (!var2) {
         var4.setItem(49, MenuItems.named(Material.MILK_BUCKET, "&d&lClear Card Overrides", List.of("&eShift-click to inherit all global defaults")));
      }

      var4.setItem(53, MenuItems.named(Material.ARROW, "&e&lBack", List.of()));
      return var4;
   }

   public static String fieldForSlot(int var0) {
      for (int var1 = 0; var1 < SLOTS.length; var1++) {
         if (SLOTS[var1] == var0) {
            return FIELDS[var1];
         }
      }

      return null;
   }
}
