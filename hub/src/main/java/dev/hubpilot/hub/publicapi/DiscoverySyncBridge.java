package dev.hubpilot.hub.publicapi;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class DiscoverySyncBridge {
   private static volatile Object plugin;

   private DiscoverySyncBridge() {
   }

   public static void attach(Object var0) {
      plugin = var0;
   }

   public static boolean handle(String var0, Object var1, byte[] var2) {
      if ("hubpilot:control".equalsIgnoreCase(var0) && var2 != null && var2.length != 0) {
         try {
            boolean var24;
            try (DataInputStream var3 = new DataInputStream(new ByteArrayInputStream(var2))) {
               String var4 = var3.readUTF();
               if (!"DISCOVERY_ADD".equals(var4)) {
                  return false;
               }

               Object var5 = plugin;
               if (var5 == null) {
                  return true;
               }

               Object var6 = var5.getClass().getMethod("getDestinationStore").invoke(var5);
               int var7 = var3.readInt();
               if (var7 < 0 || var7 > 500) {
                  throw new IllegalArgumentException("invalid destination count");
               }

               Class var8 = Class.forName("dev.hubpilot.hub.config.Destination");
               Class var9 = Class.forName("dev.hubpilot.hub.config.Destination$TargetType");
               Enum var10 = Enum.valueOf(var9.asSubclass(Enum.class), "SERVER");
               Constructor var11 = var8.getConstructor(
                  String.class, String.class, String.class, String.class, String.class, var9, String.class, String.class, boolean.class
               );
               Method var12 = var6.getClass().getMethod("find", String.class);
               Method var13 = var6.getClass().getMethod("add", var8);

               for (int var14 = 0; var14 < var7; var14++) {
                  String var15 = var3.readUTF();
                  String var16 = var3.readUTF();
                  String var17 = var3.readUTF();
                  if (!var16.isBlank() && !var15.isBlank() && var12.invoke(var6, var16) == null) {
                     Object var18 = var11.newInstance(var16, var15, "Click to connect", "Unknown", "GRASS_BLOCK", var10, var15, var17, true);
                     var13.invoke(var6, var18);
                  }
               }

               var24 = true;
            }

            return var24;
         } catch (Throwable var21) {
            log("Could not apply discovered Navigator destinations: " + root(var21));
            return true;
         }
      } else {
         return false;
      }
   }

   private static void log(String var0) {
      Object var1 = plugin;
      if (var1 != null) {
         try {
            Object var2 = var1.getClass().getMethod("getLogger").invoke(var1);
            var2.getClass().getMethod("warning", String.class).invoke(var2, "[HubPilot] " + var0);
         } catch (Throwable var3) {
         }
      }
   }

   private static String root(Throwable var0) {
      while (var0.getCause() != null && var0.getCause() != var0) {
         var0 = var0.getCause();
      }

      return var0.getMessage() == null ? var0.getClass().getSimpleName() : var0.getMessage();
   }
}
