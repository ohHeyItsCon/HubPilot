package dev.hubpilot.core;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Set;

final class UnifiedCommandFacade {
   private static final MinecraftChannelIdentifier AUTH = MinecraftChannelIdentifier.from("hubpilot:control");

   private UnifiedCommandFacade() {
   }

   static boolean handle(Object var0, Object var1) {
      try {
         Object var2 = Reflect.call(var1, "source");
         String[] var3 = (String[])Reflect.call(var1, "arguments");
         if (var3 != null && var3.length != 0 && "gui".equalsIgnoreCase(var3[0])) {
            if (AccessManager.get().state() != AccessManager.SetupState.READY) {
               PublicCommandLayer.send(var2, "§eHubPilot setup is not complete. Run §f/hp setup§e first.");
               return true;
            } else if (!(var2 instanceof Player var4)) {
               PublicCommandLayer.send(var2, "§cOnly players can open the HubPilot GUI.");
               return true;
            } else {
               String var5 = var3.length >= 2 ? var3[1].toLowerCase(Locale.ROOT) : "admin";
               Set var6 = Set.of("admin", "servers", "navigator", "diagnostics", "editor", "compass", "reload", "setup", "staff");
               if (var6.contains(var5) && !AccessManager.get().isAdminSource(var2)) {
                  PublicCommandLayer.send(var2, "§cYou do not have permission to open that HubPilot GUI.");
                  return true;
               } else {
                  StringBuilder var7 = new StringBuilder(var5);

                  for (int var8 = 2; var8 < var3.length; var8++) {
                     var7.append('\u001f').append(var3[var8]);
                  }

                  if (send(var4, "GUI:" + var7)) {
                     PublicCommandLayer.send(var2, "§aOpening HubPilot GUI...");
                  } else {
                     PublicCommandLayer.send(var2, "§cCould not open the HubPilot GUI on the configured hub.");
                  }

                  return true;
               }
            }
         } else {
            return false;
         }
      } catch (Throwable var9) {
         return false;
      }
   }

   private static boolean send(Player var0, String var1) {
      return DirectOpenTransport.send(var0, var1);
   }

   private static String hubServer() {
      try {
         Field var0 = AccessManager.class.getDeclaredField("hubServer");
         var0.setAccessible(true);
         Object var1 = var0.get(AccessManager.get());
         return var1 == null ? "hub" : String.valueOf(var1);
      } catch (Throwable var2) {
         return "hub";
      }
   }
}
