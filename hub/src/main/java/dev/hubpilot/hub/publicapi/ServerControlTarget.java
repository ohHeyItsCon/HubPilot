package dev.hubpilot.hub.publicapi;

public final class ServerControlTarget {
   private ServerControlTarget() {
   }

   public static String resolve(Object var0, String var1) {
      if (var0 != null && var1 != null && !var1.isBlank()) {
         try {
            Object var2 = var0.getClass().getMethod("getDestinationStore").invoke(var0);
            Object var3 = var2.getClass().getMethod("find", String.class).invoke(var2, var1);
            if (var3 == null) {
               return var1;
            }

            String var4 = String.valueOf(var3.getClass().getMethod("target").invoke(var3));
            if (var4 != null && !var4.isBlank() && !"null".equalsIgnoreCase(var4)) {
               return var4;
            }

            String var5 = String.valueOf(var3.getClass().getMethod("statusTarget").invoke(var3));
            if (var5 != null && !var5.isBlank() && !"null".equalsIgnoreCase(var5)) {
               return var5;
            }
         } catch (Throwable var6) {
         }

         return var1;
      } else {
         return var1;
      }
   }
}
