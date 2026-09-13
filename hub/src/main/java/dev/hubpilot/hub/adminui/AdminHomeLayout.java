package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.util.MenuItems;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class AdminHomeLayout {
   public static final String SERVERS = "servers";
   public static final String NAVIGATOR = "navigator";
   public static final String AUTOMATION = "automation";
   public static final String THEME = "theme";
   public static final String TELEMETRY = "telemetry";
   public static final String DIAGNOSTICS = "diagnostics";
   public static final String HELP = "help";
   public static final String STAFF = "staff";
   public static final String SETUP = "setup";
   public static final int ABOUT_SLOT = 34;
   public static final int EDIT_LAYOUT_SLOT = 45;
   public static final int CLOSE_SLOT = 49;
   private static final List<String> IDS = List.of("servers", "navigator", "automation", "theme", "telemetry", "diagnostics", "help", "staff", "setup");
   private static final Map<String, Integer> DEFAULTS = Map.ofEntries(
      Map.entry("servers", 10),
      Map.entry("navigator", 12),
      Map.entry("automation", 14),
      Map.entry("theme", 16),
      Map.entry("telemetry", 28),
      Map.entry("diagnostics", 30),
      Map.entry("help", 32),
      Map.entry("staff", 36),
      Map.entry("setup", 38)
   );

   private AdminHomeLayout() {
   }

   public static List<String> ids() {
      return IDS;
   }

   public static String label(String var0) {
      return switch (var0) {
         case "servers" -> "Servers";
         case "navigator" -> "Navigator Item";
         case "automation" -> "Global Automation Defaults";
         case "theme" -> "Menu & Theme";
         case "telemetry" -> "Server Card Defaults";
         case "diagnostics" -> "Diagnostics";
         case "help" -> "Help";
         case "staff" -> "Staff Management";
         case "setup" -> "Setup & Providers";
         default -> var0;
      };
   }

   public static int slot(HubPilotHubPlugin var0, String var1) {
      Integer var2 = assignments(var0).get(var1);
      return var2 == null ? -1 : var2;
   }

   public static String idAtSlot(HubPilotHubPlugin var0, int var1) {
      for (Entry var3 : assignments(var0).entrySet()) {
         if ((Integer)var3.getValue() == var1) {
            return (String)var3.getKey();
         }
      }

      return null;
   }

   public static Map<String, Integer> assignments(HubPilotHubPlugin var0) {
      AdminSettingsStore var1 = HubPilotAdminRuntime.settings(var0);
      LinkedHashMap var2 = new LinkedHashMap();
      HashSet var3 = new HashSet();
      var3.add(34);
      var3.add(45);
      var3.add(49);

      for (String var5 : IDS) {
         int var6 = DEFAULTS.get(var5);
         int var7 = var1.getInt(key(var5), var6);
         int var8 = var7;
         if (!allowed(var7) || var3.contains(var7)) {
            var8 = var6;
            if (!allowed(var6) || var3.contains(var6)) {
               var8 = firstFree(var3);
            }
         }

         var2.put(var5, var8);
         var3.add(var8);
      }

      return var2;
   }

   public static AdminHomeLayout.AssignmentResult assign(HubPilotHubPlugin var0, String var1, int var2) throws IOException {
      if (!IDS.contains(var1)) {
         return new AdminHomeLayout.AssignmentResult(false, "Unknown Admin Home item.");
      } else if (!allowed(var2)) {
         return new AdminHomeLayout.AssignmentResult(false, "That slot is reserved for a fixed Admin Home control.");
      } else {
         Map<String, Integer> var3 = assignments(var0);
         int var4 = (Integer)var3.get(var1);
         if (var4 == var2) {
            return new AdminHomeLayout.AssignmentResult(true, label(var1) + " is already in that slot.");
         } else {
            String var5 = null;

            for (Entry var7 : var3.entrySet()) {
               if (!((String)var7.getKey()).equals(var1) && (Integer)var7.getValue() == var2) {
                  var5 = (String)var7.getKey();
                  break;
               }
            }

            AdminSettingsStore var11 = HubPilotAdminRuntime.settings(var0);

            try {
               var11.setInt(key(var1), var2);
               if (var5 != null) {
                  var11.setInt(key(var5), var4);
               }
            } catch (IOException var10) {
               try {
                  var11.setInt(key(var1), var4);
                  if (var5 != null) {
                     var11.setInt(key(var5), var2);
                  }
               } catch (IOException var9) {
               }

               throw var10;
            }

            return var5 == null
               ? new AdminHomeLayout.AssignmentResult(true, "Moved " + label(var1) + " to GUI slot " + var2 + ".")
               : new AdminHomeLayout.AssignmentResult(true, "Moved " + label(var1) + " to GUI slot " + var2 + " and swapped with " + label(var5) + ".");
         }
      }
   }

   public static ItemStack item(String var0, boolean var1) {
      Material var2;
      String var3;
      String var4;
      switch (var0) {
         case "servers":
            var2 = Material.CHEST;
            var3 = "&b&lServers";
            var4 = "&7Add, remove and edit destinations";
            break;
         case "navigator":
            var2 = Material.COMPASS;
            var3 = "&b&lNavigator Item";
            var4 = "&7Forced compass, hotbar slot and protection";
            break;
         case "automation":
            var2 = Material.COMMAND_BLOCK;
            var3 = "&d&lGlobal Automation Defaults";
            var4 = "&7Countdown, retries and shutdown defaults";
            break;
         case "theme":
            var2 = Material.ITEM_FRAME;
            var3 = "&6&lMenu & Theme";
            var4 = "&7GUI title, filler and item names";
            break;
         case "telemetry":
            var2 = Material.SPYGLASS;
            var3 = "&a&lServer Card Defaults";
            var4 = "&7Choose which live stats appear";
            break;
         case "diagnostics":
            var2 = Material.COMPARATOR;
            var3 = "&e&lDiagnostics";
            var4 = "&7Storage, bridge and destination checks";
            break;
         case "help":
            var2 = Material.WRITABLE_BOOK;
            var3 = "&f&lHelp";
            var4 = "&7Commands and click controls";
            break;
         case "staff":
            var2 = Material.PLAYER_HEAD;
            var3 = "&b&lStaff Management";
            var4 = "&7Owners, admins, moderators and helpers";
            break;
         case "setup":
            var2 = Material.COMMAND_BLOCK;
            var3 = "&a&lSetup & Providers";
            var4 = "&7Ownership, provider connection and setup state";
            break;
         default:
            var2 = Material.BARRIER;
            var3 = "&cUnknown";
            var4 = "&7Unknown Admin Home item";
      }

      return var1
         ? MenuItems.named(var2, var3, List.of(var4, "", "&a&lSELECTED", "&eClick any movable slot to place/swap"))
         : MenuItems.named(var2, var3, List.of(var4, "&eClick to open"));
   }

   public static boolean allowed(int var0) {
      return var0 >= 0 && var0 < 54 && var0 != 34 && var0 != 45 && var0 != 49;
   }

   private static int firstFree(Set<Integer> var0) {
      for (int var1 = 0; var1 < 54; var1++) {
         if (allowed(var1) && !var0.contains(var1)) {
            return var1;
         }
      }

      return 0;
   }

   private static String key(String var0) {
      return "admin-home.slot." + var0;
   }

   public record AssignmentResult(boolean success, String message) {
   }
}
