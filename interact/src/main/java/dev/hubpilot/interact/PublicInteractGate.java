package dev.hubpilot.interact;

import dev.hubpilot.hub.publicapi.PublicHubBootstrap;
import org.bukkit.command.CommandSender;

public final class PublicInteractGate {
   private PublicInteractGate() {
   }

   public static boolean allow(Object var0, String[] var1) {
      return PublicHubBootstrap.allowInteractCommand(var0, var1);
   }

   public static boolean canManage(Object var0) {
      if (PublicHubBootstrap.canAdmin(var0)) {
         return true;
      } else {
         return !(var0 instanceof CommandSender var1) ? false : var1.hasPermission("hubpilot.interact.admin") || var1.hasPermission("hubpilot.interact.edit");
      }
   }
}
