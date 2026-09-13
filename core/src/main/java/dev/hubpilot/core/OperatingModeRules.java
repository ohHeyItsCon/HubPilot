package dev.hubpilot.core;

import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public final class OperatingModeRules {
   public static final String PROPERTY = "always-on-server";

   private OperatingModeRules() {
   }

   public static ManagedServer applyProperties(ManagedServer var0, Properties var1, String var2) {
      if (var0 != null && var1 != null) {
         String var3 = normalize(var2 == null ? var0.id() : var2);
         String var4 = var1.getProperty("server." + var3 + ".always-on-server");
         if (var4 == null) {
            var4 = var1.getProperty("global.always-on-server", "false");
         }

         return Boolean.parseBoolean(var4) ? alwaysOn(var0) : var0;
      } else {
         return var0;
      }
   }

   public static Set<String> addAllowed(Set<String> var0) {
      HashSet var1 = new HashSet();
      if (var0 != null) {
         var1.addAll(var0);
      }

      var1.add("always-on-server");
      return Set.copyOf(var1);
   }

   public static ManagedServer alwaysOn(ManagedServer var0) {
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
         true,
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

   private static String normalize(String var0) {
      return var0.trim().toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9_-]", "-");
   }
}
