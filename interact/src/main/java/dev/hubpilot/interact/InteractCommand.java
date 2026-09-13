package dev.hubpilot.interact;

import dev.hubpilot.hub.config.Destination;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

final class InteractCommand implements CommandExecutor, TabCompleter {
   private final HubPilotInteractPlugin plugin;

   InteractCommand(HubPilotInteractPlugin var1) {
      this.plugin = var1;
   }

   public boolean onCommandLegacy(CommandSender var1, Command var2, String var3, String[] var4) {
      if (!PublicInteractGate.canManage(var1)) {
         var1.sendMessage("§cYou do not have permission.");
         return true;
      } else if (var4.length == 0) {
         this.help(var1);
         return true;
      } else {
         try {
            if (var4[0].equalsIgnoreCase("reload")) {
               this.plugin.store().load();
               var1.sendMessage("§aHubPilot Interact reloaded.");
               return true;
            }

            if (var4[0].equalsIgnoreCase("list")) {
               var1.sendMessage(
                  "§bHubPilot Interact§7: "
                     + this.plugin.store().portals().size()
                     + " portal(s), entity targets="
                     + this.plugin.store().entityDestinations()
                     + ", sign targets="
                     + this.plugin.store().signDestinations()
               );
               return true;
            }

            if (var4[0].equalsIgnoreCase("bind") && var4.length >= 3) {
               if (var1 instanceof Player var12) {
                  HubPilotInteractPlugin.BindType var13 = var4[1].equalsIgnoreCase("entity")
                     ? HubPilotInteractPlugin.BindType.ENTITY
                     : (var4[1].equalsIgnoreCase("sign") ? HubPilotInteractPlugin.BindType.SIGN : null);
                  if (var13 == null) {
                     var1.sendMessage("§eUse: /hpi bind <entity|sign> <server>");
                     return true;
                  }

                  if (!this.plugin.validDestination(var4[2])) {
                     var1.sendMessage("§cUnknown HubPilot destination: " + var4[2]);
                     return true;
                  }

                  this.plugin.pending().put(var12.getUniqueId(), new HubPilotInteractPlugin.Pending(var13, var4[2]));
                  var1.sendMessage(
                     var13 == HubPilotInteractPlugin.BindType.ENTITY ? "§eRight-click the entity to bind it." : "§eRight-click the sign to bind it."
                  );
                  return true;
               }

               var1.sendMessage("§cRun bind in-game.");
               return true;
            }

            if (var4[0].equalsIgnoreCase("cancel") && var1 instanceof Player var11) {
               this.plugin.pending().remove(var11.getUniqueId());
               var1.sendMessage("§aPending bind cancelled.");
               return true;
            }

            if (var4[0].equalsIgnoreCase("portal") && var4.length >= 3) {
               if (!(var1 instanceof Player var5) && !var4[1].equalsIgnoreCase("delete")) {
                  var1.sendMessage("§cPortal positions must be set in-game.");
                  return true;
               }

               String var10 = var4[1].toLowerCase(Locale.ROOT);
               String var6 = var4[2].toLowerCase(Locale.ROOT);
               HubPilotInteractPlugin.PortalDraft var7 = this.plugin.drafts().getOrDefault(var6, new HubPilotInteractPlugin.PortalDraft(null, null));
               if (var10.equals("pos1")) {
                  Location var14 = ((Player)var1).getLocation();
                  this.plugin.drafts().put(var6, new HubPilotInteractPlugin.PortalDraft(var14, var7.pos2()));
                  var1.sendMessage("§aPortal " + var6 + " pos1 set.");
                  return true;
               }

               if (var10.equals("pos2")) {
                  Location var8 = ((Player)var1).getLocation();
                  this.plugin.drafts().put(var6, new HubPilotInteractPlugin.PortalDraft(var7.pos1(), var8));
                  var1.sendMessage("§aPortal " + var6 + " pos2 set.");
                  return true;
               }

               if (var10.equals("save") && var4.length >= 4) {
                  if (var7.pos1() != null && var7.pos2() != null) {
                     if (!this.plugin.validDestination(var4[3])) {
                        var1.sendMessage("§cUnknown HubPilot destination: " + var4[3]);
                        return true;
                     }

                     this.plugin.store().savePortal(var6, var7.pos1(), var7.pos2(), var4[3]);
                     this.plugin.drafts().remove(var6);
                     var1.sendMessage("§aPortal " + var6 + " linked to §f" + var4[3] + "§a.");
                     return true;
                  }

                  var1.sendMessage("§cSet pos1 and pos2 first.");
                  return true;
               }

               if (var10.equals("delete")) {
                  var1.sendMessage(this.plugin.store().deletePortal(var6) ? "§aPortal deleted." : "§ePortal not found.");
                  return true;
               }
            }
         } catch (IOException var9) {
            var1.sendMessage("§cCould not save Interact config: " + var9.getMessage());
            return true;
         }

         this.help(var1);
         return true;
      }
   }

   private void help(CommandSender var1) {
      var1.sendMessage(
         "§b/hpi bind <entity|sign> <server>§7, §bportal <pos1|pos2> <name>§7, §bportal save <name> <server>§7, §bportal delete <name>§7, §blist§7, §breload§7, §bcancel"
      );
   }

   public List<String> onTabComplete(CommandSender var1, Command var2, String var3, String[] var4) {
      if (var4.length == 1) {
         return this.match(var4[0], List.of("bind", "portal", "list", "reload", "cancel"));
      } else if (var4.length == 2 && var4[0].equalsIgnoreCase("bind")) {
         return this.match(var4[1], List.of("entity", "sign"));
      } else if (var4.length == 2 && var4[0].equalsIgnoreCase("portal")) {
         return this.match(var4[1], List.of("pos1", "pos2", "save", "delete"));
      } else if (var4.length == 3 && var4[0].equalsIgnoreCase("bind")
         || var4.length == 4 && var4[0].equalsIgnoreCase("portal") && var4[1].equalsIgnoreCase("save")) {
         ArrayList var5 = new ArrayList();

         for (Destination var7 : this.plugin.hub().getDestinationStore().all()) {
            var5.add(var7.id());
         }

         return this.match(var4[var4.length - 1], var5);
      } else {
         return List.of();
      }
   }

   private List<String> match(String var1, List<String> var2) {
      String var3 = var1.toLowerCase(Locale.ROOT);
      return var2.stream().filter(var1x -> var1x.toLowerCase(Locale.ROOT).startsWith(var3)).toList();
   }

   public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
      return !PublicInteractGate.allow(var1, var4) ? true : this.onCommandLegacy(var1, var2, var3, var4);
   }
}
