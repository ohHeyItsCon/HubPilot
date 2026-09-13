package dev.hubpilot.hub.bridge;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.config.Destination;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class HubPilotPromptManager implements Listener {
   private final HubPilotHubPlugin plugin;
   private final Map<UUID, HubPilotPromptManager.Session> sessions = new ConcurrentHashMap<>();

   public HubPilotPromptManager(HubPilotHubPlugin var1) {
      this.plugin = var1;
   }

   public void begin(Player var1, String var2, boolean var3, SettingKey var4) {
      this.sessions.put(var1.getUniqueId(), new HubPilotPromptManager.Session(var2, var3, var4));
      var1.closeInventory();
      var1.sendMessage("§6HubPilot §8» §eEnter a value for §f" + var4.label() + "§e.");
      var1.sendMessage(hint(var4, var3));
      var1.sendMessage("§7Type §fcancel §7to return without changing it.");
   }

   @EventHandler
   public void onChat(AsyncPlayerChatEvent var1) {
      HubPilotPromptManager.Session var2 = this.sessions.get(var1.getPlayer().getUniqueId());
      if (var2 != null) {
         var1.setCancelled(true);
         String var3 = var1.getMessage().trim();
         Bukkit.getScheduler().runTask(this.plugin, () -> this.handle(var1.getPlayer(), var2, var3));
      }
   }

   private void handle(Player var1, HubPilotPromptManager.Session var2, String var3) {
      if (this.sessions.get(var1.getUniqueId()) == var2) {
         if (var3.equalsIgnoreCase("cancel")) {
            this.sessions.remove(var1.getUniqueId());
            this.reopen(var1, var2);
         } else {
            try {
               if (var2.global && var3.equalsIgnoreCase("inherit")) {
                  throw new IllegalArgumentException("Global settings cannot inherit another value.");
               }

               String var4 = validate(var2.key, var3);
               if (!var2.global && var4.equalsIgnoreCase("inherit")) {
                  this.plugin.getHubPilotStore().clearOverride(var2.destinationId, var2.key);
                  var1.sendMessage("§aRestored the global value for " + var2.key.label() + ".");
               } else if (var2.global) {
                  this.plugin.getHubPilotStore().setGlobal(var2.key, var4);
                  var1.sendMessage("§aUpdated global " + var2.key.label() + ".");
               } else {
                  this.plugin.getHubPilotStore().setOverride(var2.destinationId, var2.key, var4);
                  var1.sendMessage("§aUpdated " + var2.key.label() + " for this server.");
               }

               this.sessions.remove(var1.getUniqueId());
               this.reopen(var1, var2);
            } catch (IllegalArgumentException var5) {
               var1.sendMessage("§c" + var5.getMessage());
               var1.sendMessage(hint(var2.key, var2.global));
            } catch (IOException var6) {
               this.sessions.remove(var1.getUniqueId());
               var1.sendMessage("§cCould not save HubPilot settings: " + var6.getMessage());
            }
         }
      }
   }

   private void reopen(Player var1, HubPilotPromptManager.Session var2) {
      if (var2.global) {
         var1.openInventory(HubPilotMenuBuilder.buildGlobal(this.plugin));
      } else {
         Destination var3 = this.plugin.getDestinationStore().find(var2.destinationId);
         if (var3 != null) {
            var1.openInventory(HubPilotMenuBuilder.buildServer(this.plugin, var3));
         }
      }
   }

   private static String validate(SettingKey var0, String var1) {
      String var2 = var1.trim();
      if (var2.equalsIgnoreCase("inherit")) {
         return "inherit";
      } else {
         return switch (var0) {
            case COUNTDOWN_SECONDS -> integer(var2, 0, 300, "Countdown duration");
            case SOUND_VOLUME -> decimal(var2, 0.0, 2.0, "Sound volume");
            case STARTUP_TIMEOUT_SECONDS -> integer(var2, 10, 900, "Startup timeout");
            case RETRY_COUNT -> integer(var2, 0, 10, "Retry count");
            case RETRY_DELAY_SECONDS -> integer(var2, 1, 120, "Retry delay");
            case IDLE_SHUTDOWN_MINUTES -> integer(var2, 0, 1440, "Idle shutdown");
            case STOP_AFTER_FAILURE, AUTOSTART_ENABLED -> bool(var2);
            case PITCH_STYLE -> pitch(var2);
            case REQUIRED_VERSION -> var2.isBlank() ? "any" : var2;
            case COUNTDOWN_SOUND -> {
               if (var2.isBlank()) {
                  throw new IllegalArgumentException("Sound cannot be blank. Use 'none' for silence.");
               }

               yield var2.toLowerCase(Locale.ENGLISH);
            }
            case COUNTDOWN_MESSAGE -> {
               if (var2.isBlank()) {
                  throw new IllegalArgumentException("Countdown message cannot be blank.");
               }

               if (var2.length() > 200) {
                  throw new IllegalArgumentException("Countdown message must be 200 characters or shorter.");
               }

               yield var2;
            }
            default -> throw new MatchException(null, null);
         };
      }
   }

   private static String integer(String var0, int var1, int var2, String var3) {
      try {
         int var4 = Integer.parseInt(var0);
         if (var4 >= var1 && var4 <= var2) {
            return Integer.toString(var4);
         } else {
            throw new NumberFormatException();
         }
      } catch (NumberFormatException var5) {
         throw new IllegalArgumentException(var3 + " must be between " + var1 + " and " + var2 + ".");
      }
   }

   private static String decimal(String var0, double var1, double var3, String var5) {
      try {
         double var6 = Double.parseDouble(var0);
         if (Double.isFinite(var6) && !(var6 < var1) && !(var6 > var3)) {
            return Double.toString(var6);
         } else {
            throw new NumberFormatException();
         }
      } catch (NumberFormatException var8) {
         throw new IllegalArgumentException(var5 + " must be between " + var1 + " and " + var3 + ".");
      }
   }

   private static String bool(String var0) {
      if (var0.equalsIgnoreCase("true") || var0.equalsIgnoreCase("yes") || var0.equalsIgnoreCase("on")) {
         return "true";
      } else if (!var0.equalsIgnoreCase("false") && !var0.equalsIgnoreCase("no") && !var0.equalsIgnoreCase("off")) {
         throw new IllegalArgumentException("Enter true/false, yes/no, or on/off.");
      } else {
         return "false";
      }
   }

   private static String pitch(String var0) {
      String var1 = var0.toUpperCase(Locale.ENGLISH);
      if (!var1.equals("RISING") && !var1.equals("FLAT") && !var1.equals("FALLING") && !var1.equals("NONE")) {
         throw new IllegalArgumentException("Pitch style must be RISING, FLAT, FALLING, or NONE.");
      } else {
         return var1;
      }
   }

   private static String hint(SettingKey var0, boolean var1) {
      String var2 = switch (var0) {
         case COUNTDOWN_SECONDS -> "§7Example: §f5 §8(0-300 seconds)";
         case SOUND_VOLUME -> "§7Example: §f1.0 §8(0.0-2.0)";
         case STARTUP_TIMEOUT_SECONDS -> "§7Example: §f90 §8(10-900 seconds)";
         case RETRY_COUNT -> "§7Example: §f3 §8(0-10)";
         case RETRY_DELAY_SECONDS -> "§7Example: §f15 §8(1-120 seconds)";
         case IDLE_SHUTDOWN_MINUTES -> "§7Example: §f30 §8(0 disables it)";
         case STOP_AFTER_FAILURE, AUTOSTART_ENABLED -> "§7Use §ftrue §7or §ffalse";
         case PITCH_STYLE -> "§7Use §fRISING, FLAT, FALLING, §7or §fNONE";
         case REQUIRED_VERSION -> "§7Example: §f1.21.8 §8or §fany";
         case COUNTDOWN_SOUND -> "§7Example: §fminecraft:block.note_block.pling §8or §fnone";
         case COUNTDOWN_MESSAGE -> "§7Use placeholders §f{server} §7and §f{seconds}";
         default -> throw new MatchException(null, null);
      };
      return var1 ? var2 : var2 + " §8| §f'inherit' §7uses global";
   }

   private record Session(String destinationId, boolean global, SettingKey key) {
   }
}
