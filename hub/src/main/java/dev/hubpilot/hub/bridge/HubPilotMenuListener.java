package dev.hubpilot.hub.bridge;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.adminui.AdminNavigation;
import dev.hubpilot.hub.config.Destination;
import dev.hubpilot.hub.gui.AdminEntryBuilder;
import dev.hubpilot.hub.publicapi.PublicHubBootstrap;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

public final class HubPilotMenuListener implements Listener {
   private final HubPilotHubPlugin plugin;

   public HubPilotMenuListener(HubPilotHubPlugin var1) {
      this.plugin = var1;
   }

   @EventHandler
   public void onOpen(InventoryOpenEvent var1) {
   }

   @EventHandler
   public void onClick(InventoryClickEvent var1) {
      if (var1.getInventory().getHolder() instanceof HubPilotMenuHolder var3) {
         var1.setCancelled(true);
         HumanEntity var7 = var1.getWhoClicked();
         Player var2;
         if (var7 instanceof Player && PublicHubBootstrap.canAdmin(var2 = (Player)var7)) {
            if (var1.getClickedInventory() != null) {
               Inventory var5 = var1.getClickedInventory();
               var1.getInventory();
               if (var5 == var5) {
                  SettingKey var8 = this.keyForSlot(var1.getRawSlot());
                  if (var8 != null) {
                     if (var8 != SettingKey.COUNTDOWN_MESSAGE && var8 != SettingKey.REQUIRED_VERSION && !var1.isShiftClick()) {
                        if (var8 != SettingKey.STOP_AFTER_FAILURE && var8 != SettingKey.AUTOSTART_ENABLED && var8 != SettingKey.ALWAYS_ON_SERVER) {
                           String var10 = this.cycleValue(var8, this.value(var3, var8), var1.isRightClick() ? -1 : 1);
                           this.set(var2, var3, var8, var10);
                           return;
                        }

                        this.set(var2, var3, var8, Boolean.toString(!Boolean.parseBoolean(this.value(var3, var8))));
                        return;
                     }

                     this.plugin.getHubPilotPromptManager().begin(var2, var3.destinationId(), var3.global(), var8);
                     return;
                  }

                  switch (var1.getRawSlot()) {
                     case 45:
                        this.back(var2, var3);
                        break;
                     case 46:
                        var2.openInventory(MessageMenuBuilder.build(this.plugin, var3.destinationId(), 0));
                     case 47:
                     case 51:
                     default:
                        break;
                     case 48:
                        if (var3.global()) {
                           var2.sendMessage("§eGlobal defaults cannot be cleared.");
                           return;
                        }

                        if (!var1.isShiftClick()) {
                           var2.sendMessage("§eShift-click to clear every override.");
                           return;
                        }

                        try {
                           this.plugin.getHubPilotStore().clearAllOverrides(var3.destinationId());
                           var2.sendMessage("§aAll overrides cleared; this server now inherits global defaults.");
                           this.reopen(var2, var3);
                        } catch (IOException var6) {
                           var2.sendMessage("§cCould not clear overrides: " + var6.getMessage());
                        }
                        break;
                     case 49:
                        this.preview(var2, var3.destinationId());
                        break;
                     case 50:
                        Destination var9;
                        if (!var3.global() && (var9 = this.plugin.getDestinationStore().find(var3.destinationId())) != null) {
                           this.plugin.getHubPilotRequestSender().testStart(var2, var9);
                        }
                        break;
                     case 52:
                        var2.openInventory(HubPilotMenuBuilder.buildGlobal(this.plugin));
                  }

                  return;
               }
            }
         }
      }
   }

   private SettingKey keyForSlot(int var1) {
      return switch (var1) {
         case 10 -> SettingKey.COUNTDOWN_SECONDS;
         case 11 -> SettingKey.COUNTDOWN_SOUND;
         case 12 -> SettingKey.SOUND_VOLUME;
         case 13 -> SettingKey.PITCH_STYLE;
         case 14 -> SettingKey.COUNTDOWN_MESSAGE;
         case 15 -> SettingKey.REQUIRED_VERSION;
         default -> null;
         case 19 -> SettingKey.STARTUP_TIMEOUT_SECONDS;
         case 20 -> SettingKey.RETRY_COUNT;
         case 21 -> SettingKey.RETRY_DELAY_SECONDS;
         case 22 -> SettingKey.STOP_AFTER_FAILURE;
         case 23 -> SettingKey.IDLE_SHUTDOWN_MINUTES;
         case 24 -> SettingKey.AUTOSTART_ENABLED;
         case 25 -> SettingKey.ALWAYS_ON_SERVER;
      };
   }

   private String cycleValue(SettingKey var1, String var2, int var3) {
      List var4 = switch (var1) {
         case COUNTDOWN_SECONDS -> List.of("0", "3", "5", "10", "15", "30", "60");
         case COUNTDOWN_SOUND -> List.of(
            "none",
            "minecraft:block.note_block.pling",
            "minecraft:entity.experience_orb.pickup",
            "minecraft:block.amethyst_block.chime",
            "minecraft:block.beacon.activate",
            "minecraft:block.portal.trigger"
         );
         case SOUND_VOLUME -> List.of("0.0", "0.25", "0.5", "1.0", "2.0");
         case PITCH_STYLE -> List.of("RISING", "FLAT", "FALLING", "NONE");
         case STARTUP_TIMEOUT_SECONDS -> List.of("30", "60", "90", "120", "180", "300", "600");
         case RETRY_COUNT -> List.of("0", "1", "2", "3", "4", "5");
         case RETRY_DELAY_SECONDS -> List.of("5", "10", "15", "20", "30", "60");
         case IDLE_SHUTDOWN_MINUTES -> List.of("0", "5", "10", "15", "30", "60", "120");
         default -> List.of(var2);
      };
      int var5 = 0;

      for (int var6 = 0; var6 < var4.size(); var6++) {
         if (((String)var4.get(var6)).equalsIgnoreCase(var2)) {
            var5 = var6;
            break;
         }
      }

      return (String)var4.get(Math.floorMod(var5 + var3, var4.size()));
   }

   private String value(HubPilotMenuHolder var1, SettingKey var2) {
      return var1.global() ? this.plugin.getHubPilotStore().global(var2) : this.plugin.getHubPilotStore().effective(var1.destinationId(), var2);
   }

   private void set(Player var1, HubPilotMenuHolder var2, SettingKey var3, String var4) {
      try {
         if (var2.global()) {
            this.plugin.getHubPilotStore().setGlobal(var3, var4);
         } else {
            this.plugin.getHubPilotStore().setOverride(var2.destinationId(), var3, var4);
         }

         var1.sendMessage("§a" + var3.label() + " set to §f" + var4 + "§a.");
         this.reopen(var1, var2);
      } catch (IOException var6) {
         var1.sendMessage("§cCould not save: " + var6.getMessage());
      }
   }

   private void back(Player var1, HubPilotMenuHolder var2) {
      if (var2.global()) {
         var1.openInventory(AdminNavigation.globalBack(this.plugin, 0));
      } else {
         Destination var3 = this.plugin.getDestinationStore().find(var2.destinationId());
         if (var3 != null) {
            var1.openInventory(AdminEntryBuilder.build(var3));
         }
      }
   }

   private void reopen(Player var1, HubPilotMenuHolder var2) {
      if (var2.global()) {
         var1.openInventory(HubPilotMenuBuilder.buildGlobal(this.plugin));
      } else {
         Destination var3 = this.plugin.getDestinationStore().find(var2.destinationId());
         if (var3 != null) {
            var1.openInventory(HubPilotMenuBuilder.buildServer(this.plugin, var3));
         }
      }
   }

   private void preview(Player var1, String var2) {
      String var6 = var2 == null ? "server" : var2;
      Destination var5;
      if (var2 != null && (var5 = this.plugin.getDestinationStore().find(var2)) != null) {
         var6 = var5.label();
      }

      int var8 = parseInt(
         var2 == null
            ? this.plugin.getHubPilotStore().global(SettingKey.COUNTDOWN_SECONDS)
            : this.plugin.getHubPilotStore().effective(var2, SettingKey.COUNTDOWN_SECONDS),
         5
      );
      var8 = Math.min(var8, 10);
      String var9 = var2 == null
         ? this.plugin.getHubPilotStore().global(SettingKey.COUNTDOWN_SOUND)
         : this.plugin.getHubPilotStore().effective(var2, SettingKey.COUNTDOWN_SOUND);
      float var10 = parseFloat(
         var2 == null
            ? this.plugin.getHubPilotStore().global(SettingKey.SOUND_VOLUME)
            : this.plugin.getHubPilotStore().effective(var2, SettingKey.SOUND_VOLUME),
         1.0F
      );
      String var11 = var2 == null
         ? this.plugin.getHubPilotStore().global(SettingKey.PITCH_STYLE)
         : this.plugin.getHubPilotStore().effective(var2, SettingKey.PITCH_STYLE);
      String var4 = var2 == null
         ? this.plugin.getHubPilotStore().global(SettingKey.COUNTDOWN_MESSAGE)
         : this.plugin.getHubPilotStore().effective(var2, SettingKey.COUNTDOWN_MESSAGE);
      if (var8 <= 0) {
         var1.sendMessage("§aCountdown disabled; transfer would be immediate.");
      } else {
         int var3 = var8;
         int var13 = var8;

         while (var13 >= 1) {
            int var14 = (var3 - var13) * 20;
            int var15 = var13--;
            String var16 = var6;
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
               var1.sendMessage("§e" + var4.replace("{server}", var16).replace("{seconds}", Integer.toString(var15)));
               if (!var9.equalsIgnoreCase("none") && var10 > 0.0F) {
                  try {
                     var1.playSound(var1.getLocation(), var9, var10, pitch(var11, var15, var3));
                  } catch (RuntimeException var9x) {
                     var1.sendMessage("§cInvalid sound: " + var9);
                  }
               }
            }, var14);
         }
      }
   }

   private static float pitch(String var0, int var1, int var2) {
      double var3 = var2 <= 1 ? 1.0 : (double)(var2 - var1) / (var2 - 1);
      String var5 = var0.toUpperCase(Locale.ROOT);

      return switch (var5) {
         case "FALLING" -> (float)(1.6 - var3 * 0.8);
         case "FLAT" -> 1.0F;
         case "NONE" -> 1.0F;
         default -> (float)(0.8 + var3 * 0.8);
      };
   }

   private static int parseInt(String var0, int var1) {
      try {
         return Integer.parseInt(var0);
      } catch (Exception var3) {
         return var1;
      }
   }

   private static float parseFloat(String var0, float var1) {
      try {
         return Float.parseFloat(var0);
      } catch (Exception var3) {
         return var1;
      }
   }
}
