package dev.hubpilot.link.bukkit;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

final class PublicLinkCommand implements CommandExecutor, TabCompleter {
   static void install(JavaPlugin var0) {
      PluginCommand var1 = var0.getCommand("hpl");
      if (var1 != null) {
         PublicLinkCommand var2 = new PublicLinkCommand();
         var1.setExecutor(var2);
         var1.setTabCompleter(var2);
      }
   }

   public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
      var1.sendMessage("§bHubPilot Link §f1.0.2");
      var1.sendMessage("§7Hub telemetry bridge is enabled. Use §f/hp status <server> §7from the network for server status.");
      return true;
   }

   public List<String> onTabComplete(CommandSender var1, Command var2, String var3, String[] var4) {
      return List.of();
   }
}
