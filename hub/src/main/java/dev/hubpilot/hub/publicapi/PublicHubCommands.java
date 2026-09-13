package dev.hubpilot.hub.publicapi;

import dev.hubpilot.hub.HubPilotHubPlugin;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PublicHubCommands {
   private PublicHubCommands() {
   }

   public static boolean handle(HubPilotHubPlugin var0, CommandSender var1, String[] var2) {
      String var3 = var2 != null && var2.length != 0 ? var2[0].toLowerCase(Locale.ROOT) : "";
      if (var3.isEmpty()) {
         if (!PublicHubBootstrap.setupReady()) {
            var1.sendMessage("§eHubPilot first-time setup is not complete. Use §f/hp claimowner §eand then §f/hp setup§e.");
         } else {
            var1.sendMessage("§bHubPilot §f1.0.2 §7- use §f/hp help §7for commands or §f/hp gui admin §7for the admin GUI.");
         }

         return true;
      } else if (var3.equals("setup")) {
         if (var1 instanceof Player var6) {
            PublicHubBootstrap.openSetup(var6);
         } else {
            var1.sendMessage("Use /hp setup from Velocity/Core.");
         }

         return true;
      } else if (var3.equals("staff")) {
         if (var1 instanceof Player var5) {
            PublicHubBootstrap.openStaff(var5);
         } else {
            var1.sendMessage("Use /hp staff from Velocity/Core.");
         }

         return true;
      } else if (var3.equals("help") || var3.equals("?") || var3.equals("version")) {
         return false;
      } else if (!PublicHubBootstrap.setupReady()) {
         if (var1 instanceof Player var4) {
            PublicHubBootstrap.openSetup(var4);
            var1.sendMessage("§eHubPilot setup is locked. Claim ownership with /hp claimowner, then run /hp setup.");
         } else {
            var1.sendMessage("HubPilot setup is not complete. Run /hp setup in-game from the hub.");
         }

         return true;
      } else {
         return false;
      }
   }
}
