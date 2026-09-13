package dev.hubpilot.hub.publicapi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ServerControlBridge {
   private ServerControlBridge() {
   }

   public static boolean stop(Object var0, String var1) {
      try {
         Class var2 = Class.forName("dev.hubpilot.hub.publicapi.PublicHubBootstrap");
         Field var3 = var2.getDeclaredField("INSTANCE");
         var3.setAccessible(true);
         Object var4 = var3.get(null);
         if (var4 == null) {
            return false;
         } else {
            Method var5 = null;

            for (Method var9 : var2.getDeclaredMethods()) {
               if (var9.getName().equals("action") && var9.getParameterCount() == 3) {
                  var5 = var9;
                  break;
               }
            }

            if (var5 == null) {
               return false;
            } else {
               var5.setAccessible(true);
               var5.invoke(var4, var0, "SERVER_STOP", new String[]{var1});
               return true;
            }
         }
      } catch (Throwable var10) {
         return false;
      }
   }
}
