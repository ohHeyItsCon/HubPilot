package dev.hubpilot.core;

final class DiscoveryAliases {
   private DiscoveryAliases() {
   }

   static String[] normalize(String[] var0) {
      if (var0 == null || var0.length < 3) {
         return var0;
      } else if ("discover".equalsIgnoreCase(var0[0]) && "add".equalsIgnoreCase(var0[1])) {
         String var1 = var0[2];
         return !"*".equals(var1) && !"all".equalsIgnoreCase(var1) ? var0 : new String[]{"discover", "all"};
      } else {
         return var0;
      }
   }
}
