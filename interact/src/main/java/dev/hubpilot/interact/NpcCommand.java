package dev.hubpilot.interact;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

final class NpcCommand implements CommandExecutor, TabCompleter {
   private static final String TAG = "hubpilot_npc";
   private final HubPilotInteractPlugin plugin;

   private NpcCommand(HubPilotInteractPlugin var1) {
      this.plugin = var1;
   }

   static void register(HubPilotInteractPlugin var0) {
      PluginCommand var1 = var0.getCommand("hpi npc");
      if (var1 == null) {
         var0.getLogger().warning("HubPilot NPC command is missing from plugin.yml.");
      } else {
         NpcCommand var2 = new NpcCommand(var0);
         var1.setExecutor(var2);
         var1.setTabCompleter(var2);
      }
   }

   public boolean onCommandLegacy(CommandSender var1, Command var2, String var3, String[] var4) {
      if (var1 instanceof Player var5) {
         if (!PublicInteractGate.canManage(var1)) {
            var1.sendMessage("§cYou do not have permission to manage HubPilot NPCs.");
            return true;
         } else if (var4.length == 0 || var4[0].equalsIgnoreCase("help")) {
            this.help(var1);
            return true;
         } else if (!supportsMannequins()) {
            var1.sendMessage("§cNative HubPilot NPCs require a Paper 1.21.9+ hub. Your current server does not expose the mannequin API.");
            return true;
         } else {
            try {
               String var6 = var4[0].toLowerCase(Locale.ROOT);
               switch (var6) {
                  case "create":
                     this.create(var5, var4);
                     break;
                  case "delete":
                  case "remove":
                     this.deleteNearest(var5);
                     break;
                  case "skin":
                     this.setSkinNearest(var5, var4);
                     break;
                  case "server":
                  case "destination":
                     this.setDestinationNearest(var5, var4);
                     break;
                  case "name":
                     this.setNameNearest(var5, var4);
                     break;
                  case "list":
                     this.list(var5);
                     break;
                  default:
                     this.help(var1);
               }
            } catch (Exception var8) {
               var1.sendMessage("§cHubPilot NPC error: " + safeMessage(var8));
               this.plugin.getLogger().warning("HubPilot NPC command failed: " + var8);
            }

            return true;
         }
      } else {
         var1.sendMessage("§cHubPilot NPC commands must be run by a player.");
         return true;
      }
   }

   private void create(Player var1, String[] var2) throws Exception {
      if (var2.length < 2) {
         var1.sendMessage("§eUsage: /hpi npc create <server> [skin] [display name...]");
      } else {
         String var3 = var2[1];
         if (!this.plugin.validDestination(var3)) {
            var1.sendMessage("§cUnknown HubPilot destination: " + var3);
         } else {
            String var4 = var2.length >= 3 ? var2[2] : var1.getName();
            String var5 = var2.length >= 4 ? join(var2, 3) : var3;
            Location var6 = var1.getLocation().clone().add(0.0, 0.0, 1.5);
            Entity var7 = this.spawnMannequin(var1.getWorld(), var6);
            var7.addScoreboardTag("hubpilot_npc");
            var7.setCustomName(var5);
            var7.setCustomNameVisible(true);
            var7.setInvulnerable(true);
            var7.setGravity(false);
            invokeMannequin(var7, "setImmovable", new Class[]{boolean.class}, true);
            this.plugin.store().bindEntity(var7.getUniqueId(), var3);
            this.applySkinAsync(var7, var4, var1);
            var1.sendMessage("§aCreated HubPilot NPC for §f" + var3 + "§a using skin §f" + var4 + "§a.");
         }
      }
   }

   private void deleteNearest(Player var1) {
      Entity var2 = this.nearestNpc(var1);
      if (var2 == null) {
         var1.sendMessage("§eNo HubPilot NPC found within 6 blocks.");
      } else {
         var2.remove();
         var1.sendMessage("§aRemoved the nearest HubPilot NPC.");
      }
   }

   private void setSkinNearest(Player var1, String[] var2) {
      if (var2.length < 2) {
         var1.sendMessage("§eUsage: /hpi npc skin <player>");
      } else {
         Entity var3 = this.nearestNpc(var1);
         if (var3 == null) {
            var1.sendMessage("§eNo HubPilot NPC found within 6 blocks.");
         } else {
            this.applySkinAsync(var3, var2[1], var1);
            var1.sendMessage("§7Resolving skin §f" + var2[1] + "§7...");
         }
      }
   }

   private void setDestinationNearest(Player var1, String[] var2) throws IOException {
      if (var2.length < 2) {
         var1.sendMessage("§eUsage: /hpi npc server <server>");
      } else {
         String var3 = var2[1];
         if (!this.plugin.validDestination(var3)) {
            var1.sendMessage("§cUnknown HubPilot destination: " + var3);
         } else {
            Entity var4 = this.nearestNpc(var1);
            if (var4 == null) {
               var1.sendMessage("§eNo HubPilot NPC found within 6 blocks.");
            } else {
               this.plugin.store().bindEntity(var4.getUniqueId(), var3);
               var1.sendMessage("§aNPC now requests §f" + var3 + "§a.");
            }
         }
      }
   }

   private void setNameNearest(Player var1, String[] var2) {
      if (var2.length < 2) {
         var1.sendMessage("§eUsage: /hpi npc name <display name...>");
      } else {
         Entity var3 = this.nearestNpc(var1);
         if (var3 == null) {
            var1.sendMessage("§eNo HubPilot NPC found within 6 blocks.");
         } else {
            String var4 = join(var2, 1);
            var3.setCustomName(var4);
            var3.setCustomNameVisible(!var4.isBlank());
            var1.sendMessage("§aNPC display name updated.");
         }
      }
   }

   private void list(Player var1) {
      ArrayList<Entity> var2 = new ArrayList<>();

      for (Entity var4 : var1.getWorld().getEntities()) {
         if (var4.getScoreboardTags().contains("hubpilot_npc")) {
            var2.add(var4);
         }
      }

      if (var2.isEmpty()) {
         var1.sendMessage("§7No HubPilot NPCs are loaded in this world.");
      } else {
         var1.sendMessage("§bHubPilot NPCs in this world: §f" + var2.size());

         for (Entity var8 : var2) {
            String var5 = this.plugin.store().entity(var8.getUniqueId());
            Location var6 = var8.getLocation();
            var1.sendMessage(
               "§7- §f"
                  + (var8.getCustomName() == null ? "NPC" : var8.getCustomName())
                  + " §8-> §f"
                  + (var5 == null ? "unbound" : var5)
                  + " §8@ §7"
                  + var6.getBlockX()
                  + ","
                  + var6.getBlockY()
                  + ","
                  + var6.getBlockZ()
            );
         }
      }
   }

   private Entity nearestNpc(Player var1) {
      Location var2 = var1.getLocation();
      return var1.getNearbyEntities(6.0, 6.0, 6.0)
         .stream()
         .filter(var0 -> var0.getScoreboardTags().contains("hubpilot_npc"))
         .min(Comparator.comparingDouble(var1x -> var1x.getLocation().distanceSquared(var2)))
         .orElse(null);
   }

   private Entity spawnMannequin(World var1, Location var2) throws Exception {
      Class var3 = Class.forName("org.bukkit.entity.EntityType");
      Enum var4 = Enum.valueOf(var3.asSubclass(Enum.class), "MANNEQUIN");
      Method var5 = Class.forName("org.bukkit.World").getMethod("spawnEntity", Location.class, var3);
      return (Entity)var5.invoke(var1, var2, var4);
   }

   private void applySkinAsync(Entity var1, String var2, Player var3) {
      try {
         Class var4 = Class.forName("org.bukkit.Bukkit");
         Class var5 = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
         Object var6 = var4.getMethod("createProfile", String.class).invoke(null, var2);
         CompletableFuture var7 = (CompletableFuture)var5.getMethod("update").invoke(var6);
         var7.whenComplete((var5x, var6x) -> this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            if (var6x == null && var5x != null && var1.isValid()) {
               try {
                  Class var6xx = Class.forName("io.papermc.paper.datacomponent.item.ResolvableProfile");
                  Object var7x = var6xx.getMethod("resolvableProfile", var5).invoke(null, var5x);
                  invokeMannequin(var1, "setProfile", new Class[]{var6xx}, var7x);
                  if (var3.isOnline()) {
                     var3.sendMessage("§aApplied skin §f" + var2 + "§a to the NPC.");
                  }
               } catch (Exception var8x) {
                  if (var3.isOnline()) {
                     var3.sendMessage("§eSkin resolved but could not be applied: " + safeMessage(var8x));
                  }
               }
            } else {
               if (var3.isOnline()) {
                  var3.sendMessage("§eCould not resolve skin §f" + var2 + "§e; the NPC remains usable.");
               }
            }
         }));
      } catch (Exception var8) {
         var3.sendMessage("§eCould not start skin lookup: " + safeMessage(var8));
      }
   }

   private static void invokeMannequin(Entity var0, String var1, Class<?>[] var2, Object... var3) throws Exception {
      Class var4 = Class.forName("org.bukkit.entity.Mannequin");
      if (!var4.isInstance(var0)) {
         throw new IllegalStateException("Spawned entity is not a mannequin");
      } else {
         var4.getMethod(var1, var2).invoke(var0, var3);
      }
   }

   private static boolean supportsMannequins() {
      try {
         Class.forName("org.bukkit.entity.Mannequin");
         Class var0 = Class.forName("org.bukkit.entity.EntityType");
         Enum.valueOf(var0.asSubclass(Enum.class), "MANNEQUIN");
         Class.forName("io.papermc.paper.datacomponent.item.ResolvableProfile");
         return true;
      } catch (Throwable var1) {
         return false;
      }
   }

   private void help(CommandSender var1) {
      var1.sendMessage(
         "§bHubPilot NPC commands: §f/hpi npc create <server> [skin] [display name...]§7, §f/hpi npc skin <player>§7, §f/hpi npc server <server>§7, §f/hpi npc name <name...>§7, §f/hpi npc delete§7, §f/hpi npc list"
      );
   }

   public List<String> onTabComplete(CommandSender var1, Command var2, String var3, String[] var4) {
      if (var4.length == 1) {
         return match(var4[0], List.of("create", "skin", "server", "name", "delete", "list"));
      } else if ((var4.length != 2 || !var4[0].equalsIgnoreCase("create"))
         && (var4.length != 2 || !var4[0].equalsIgnoreCase("server") && !var4[0].equalsIgnoreCase("destination"))) {
         return List.of();
      } else {
         ArrayList var5 = new ArrayList();
         this.plugin.hub().getDestinationStore().all().forEach(var1x -> var5.add(var1x.id()));
         return match(var4[1], var5);
      }
   }

   private static List<String> match(String var0, List<String> var1) {
      String var2 = var0.toLowerCase(Locale.ROOT);
      return var1.stream().filter(var1x -> var1x.toLowerCase(Locale.ROOT).startsWith(var2)).toList();
   }

   private static String join(String[] var0, int var1) {
      return String.join(" ", Arrays.copyOfRange(var0, var1, var0.length));
   }

   private static String safeMessage(Throwable var0) {
      Throwable var1 = var0;

      while (var1.getCause() != null) {
         var1 = var1.getCause();
      }

      String var2 = var1.getMessage();
      return var2 != null && !var2.isBlank() ? var2 : var1.getClass().getSimpleName();
   }

   public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
      return !PublicInteractGate.allow(var1, var4) ? true : this.onCommandLegacy(var1, var2, var3, var4);
   }
}
