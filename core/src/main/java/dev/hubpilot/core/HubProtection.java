package dev.hubpilot.core;

import java.util.Locale;
import java.util.Map;

final class HubProtection {
   static boolean matches(ManagedServer var0, String var1) {
      return var0 != null && (sameName(var0.id(), var1) || sameName(var0.velocityServer(), var1));
   }

   static void repair(Map<String, ManagedServer> var0, String var1) {
      var0.replaceAll((var1x, var2) -> matches(var2, var1) ? protect(var2) : var2);
   }

   private static ManagedServer protect(ManagedServer var0) {
      return new ManagedServer(
         var0.id(),
         var0.displayName(),
         var0.velocityServer(),
         var0.provider(),
         var0.providerServerId(),
         var0.requiredVersion(),
         var0.strictVersion(),
         var0.loader(),
         var0.permission(),
         var0.maintenance(),
         var0.autoStartEnabled(),
         var0.expectedStartupSeconds(),
         var0.startupTimeoutSeconds(),
         var0.pingTimeoutSeconds(),
         var0.retryCount(),
         var0.retryDelaySeconds(),
         false,
         false,
         0,
         var0.countdownSeconds(),
         var0.countdownSound(),
         var0.countdownVolume(),
         var0.pitchStyle(),
         var0.countdownMessage(),
         var0.announceEverySecond(),
         var0.tags()
      );
   }

   private static boolean sameName(String var0, String var1) {
      String var2 = key(var0);
      String var3 = key(var1);
      return !var2.isEmpty() && !var3.isEmpty() && (var2.equals(var3) || roleKey(var2).equals(roleKey(var3)));
   }

   private static String key(String var0) {
      return var0 == null ? "" : var0.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
   }

   private static String roleKey(String var0) {
      if (var0.startsWith("the") && var0.length() > 3) {
         var0 = var0.substring(3);
      }

      if (var0.startsWith("main") && var0.length() > 4) {
         var0 = var0.substring(4);
      }

      if (var0.endsWith("server") && var0.length() > 6) {
         var0 = var0.substring(0, var0.length() - 6);
      }

      return var0;
   }
}
