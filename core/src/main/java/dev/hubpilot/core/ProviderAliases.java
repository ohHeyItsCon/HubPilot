package dev.hubpilot.core;

import java.util.Locale;

final class ProviderAliases {
   private ProviderAliases() {
   }

   static String canonical(String var0) {
      if (var0 == null) {
         return "";
      } else {
         String var1 = var0.trim().toLowerCase(Locale.ROOT);

         return switch (var1) {
            case "craft" -> "crafty";
            case "always", "alwaysonline", "always_online" -> "always-online";
            default -> var1;
         };
      }
   }
}
