package dev.hubpilot.hub.gui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.adminui.AdminSettingsStore;
import dev.hubpilot.hub.adminui.HubPilotAdminRuntime;
import dev.hubpilot.hub.config.Destination;
import dev.hubpilot.hub.status.ServerStatus;
import dev.hubpilot.hub.status.TimeFormat;
import dev.hubpilot.hub.telemetry.RichTelemetryStore;
import dev.hubpilot.hub.util.IconMaterials;
import dev.hubpilot.hub.util.MenuItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class LegacyDestinationMenuBuilder {
   private LegacyDestinationMenuBuilder() {
   }

   public static Inventory build(HubPilotHubPlugin var0, int var1) {
      List var2 = var0.getDestinationStore().enabled();
      int var3 = Math.max(1, (var2.size() + MenuSlots.ENTRY_SLOTS.length - 1) / MenuSlots.ENTRY_SLOTS.length);
      int var4 = Math.max(0, Math.min(var1, var3 - 1));
      DestinationMenuHolder var5 = new DestinationMenuHolder(var4);
      Inventory var6 = Bukkit.createInventory(var5, 54, MenuItems.colorize(var0.getMenuConfig().guiTitle() + " &7(" + (var4 + 1) + "/" + var3 + ")"));
      var5.setInventory(var6);
      GuiCommon.fill(var6, var0.getMenuConfig().fillerMaterial());
      int var7 = var4 * MenuSlots.ENTRY_SLOTS.length;

      for (int var8 = 0; var8 < MenuSlots.ENTRY_SLOTS.length; var8++) {
         int var9 = var7 + var8;
         if (var9 >= var2.size()) {
            break;
         }

         var6.setItem(MenuSlots.ENTRY_SLOTS[var8], entryItem(var0, (Destination)var2.get(var9)));
      }

      if (var4 > 0) {
         var6.setItem(45, GuiCommon.previous());
      }

      var6.setItem(49, MenuItems.named(Material.CLOCK, "&b&lRefresh Status", List.of("&7Reload live server status")));
      if (var4 + 1 < var3) {
         var6.setItem(53, GuiCommon.next());
      }

      return var6;
   }

   public static ItemStack entryItem(HubPilotHubPlugin var0, Destination var1) {
      ServerStatus var2 = var0.getStatusStore().get(var1.statusTarget());
      RichTelemetryStore.Snapshot var3 = RichTelemetryStore.of(var0).get(var1.id());
      AdminSettingsStore var4 = HubPilotAdminRuntime.settings(var0);
      ArrayList var5 = new ArrayList();
      if (var4.telemetry(var1.id(), "description") && !var1.description().isBlank()) {
         var5.add("&7" + var1.description());
      }

      if (var4.telemetry(var1.id(), "description") && !var1.description().isBlank()) {
         var5.add("");
      }

      if (var4.telemetry(var1.id(), "software")) {
         var5.add("&7Software: &f" + var1.software());
      }

      if (var4.telemetry(var1.id(), "type")) {
         var5.add("&7Type: &f" + var1.targetType().name());
      }

      if ((var4.telemetry(var1.id(), "software") || var4.telemetry(var1.id(), "type")) && var4.telemetry(var1.id(), "status")) {
         var5.add("");
      }

      if (var4.telemetry(var1.id(), "status")) {
         switch (DestinationMenuBuilder$1.$SwitchMap$dev$hubpilot$hub$status$ServerStatus$State[var2.state().ordinal()]) {
            case 1:
               var5.add("&a● &lONLINE");
               break;
            case 2:
               var5.add("&e● &lSTARTING");
               break;
            case 3:
               var5.add("&c● &lOFFLINE");
               break;
            default:
               var5.add("&8● &lSTATUS UNKNOWN");
         }
      }

      if (var4.telemetry(var1.id(), "players") && var2.state() == ServerStatus.State.ONLINE && var2.playersOnline() >= 0) {
         var5.add("&7Players: &f" + var2.playersOnline() + (var2.playersMax() >= 0 ? "/" + var2.playersMax() : ""));
      }

      if (var4.telemetry(var1.id(), "backend_ping") && var3.backendPingMs() >= 0L) {
         var5.add("&7Backend ping: &f" + var3.backendPingMs() + "ms");
      }

      if (var4.telemetry(var1.id(), "startup_progress") && var2.state() == ServerStatus.State.STARTING) {
         var5.add("&7Startup: &e" + var3.startupProgress() + "%");
      }

      if (var4.telemetry(var1.id(), "queue") && var3.queueSize() > 0) {
         var5.add("&7Queue: &f" + var3.queueSize());
      }

      if (var4.telemetry(var1.id(), "avg_startup") && var3.averageStartupSeconds() > 0L) {
         var5.add("&7Average startup: &f" + var3.averageStartupSeconds() + "s");
      }

      if (var3.linkFresh()) {
         if (var4.telemetry(var1.id(), "tps") && var3.tps() >= 0.0) {
            var5.add("&7TPS: &f" + one(var3.tps()));
         }

         if (var4.telemetry(var1.id(), "mspt") && var3.mspt() >= 0.0) {
            var5.add("&7MSPT: &f" + one(var3.mspt()));
         }

         if (var4.telemetry(var1.id(), "ram") && var3.memoryUsedMb() >= 0L) {
            var5.add("&7RAM: &f" + var3.memoryUsedMb() + " / " + var3.memoryMaxMb() + " MB");
         }

         if (var4.telemetry(var1.id(), "cpu") && var3.cpuPercent() >= 0.0) {
            var5.add("&7CPU: &f" + one(var3.cpuPercent()) + "%");
         }

         if (var4.telemetry(var1.id(), "chunks") && var3.loadedChunks() >= 0) {
            var5.add("&7Loaded chunks: &f" + var3.loadedChunks());
         }

         if (var4.telemetry(var1.id(), "entities") && var3.entities() >= 0) {
            var5.add("&7Entities: &f" + var3.entities());
         }
      }

      if (var4.telemetry(var1.id(), "link")) {
         if (var3.linkFresh()) {
            var5.add("&7Link: &aCONNECTED &8(" + var3.platform() + ")");
         } else {
            var5.add("&7Link: &8NOT AVAILABLE");
         }
      }

      if (var4.telemetry(var1.id(), "history") && var3.receivedAt() > 0L) {
         var5.add("&7Requests/joins/fails: &f" + var3.totalRequests() + "/" + var3.successfulJoins() + "/" + var3.failedJoins());
      }

      if (var4.telemetry(var1.id(), "errors") && var3.lastError() != null && !var3.lastError().isBlank()) {
         var5.add("&cLast error: &7" + trim(var3.lastError(), 48));
      }

      if (var4.telemetry(var1.id(), "time")) {
         if (var2.state() == ServerStatus.State.ONLINE) {
            var5.add("&7Uptime: &f" + TimeFormat.durationSince(var2.onlineSince()));
         } else if (var2.state() == ServerStatus.State.STARTING) {
            var5.add("&7Started: &f" + TimeFormat.relative(var2.lastStarted()));
         } else if (var2.state() == ServerStatus.State.OFFLINE) {
            var5.add("&7Last started: &f" + TimeFormat.relative(var2.lastStarted()));
         }
      }

      if (var4.telemetry(var1.id(), "action")) {
         var5.add("");
         var5.add("&eClick to start or join");
      }

      return MenuItems.named(IconMaterials.resolve(var1.iconMaterial()), "&f&l" + var1.label(), var5);
   }

   private static String one(double var0) {
      return String.format(Locale.ROOT, "%.1f", var0);
   }

   private static String trim(String var0, int var1) {
      return var0.length() <= var1 ? var0 : var0.substring(0, var1 - 1) + "…";
   }
}
